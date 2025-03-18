package com.example.swxh;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.StopWatch;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiFunction;
import java.util.stream.Collectors;


/**
 * 库存管理类，库存操作从此进行
 */
@Slf4j
@Service
public class StockManager {

    private static HashMap<Long, Stock> DATA_POOLS;
    private static HashMap<String, Long> SKU_ID_MAP;
    private static TreeMap<String, Long> CREATOR_ID_MAP;
    private static TreeMap<String, Long> CREATE_TIME_ID_MAP;


    @Resource
    JsonFileRepository JSON_FILE_REPOSITORY;
    private static final Long DEFAULT_ID = -1L;
    private static final int MAX_SIZE = 1000;


    private static final AtomicLong ID_INCREASE = new AtomicLong(0);
    private static final BiFunction<String, Long, String> CREATOR_ID_FUNCTION = (creator, id) -> creator + "_" + id;


    private static final Lock lock = new ReentrantLock();


    public StockManager() {
        DATA_POOLS = new HashMap<>(MAX_SIZE);
        SKU_ID_MAP = new HashMap<>(MAX_SIZE);
        CREATOR_ID_MAP = new TreeMap<>(String::compareTo);
        CREATE_TIME_ID_MAP = new TreeMap<>(String::compareTo);
    }


    //初始化数据，从持久化数据中读取到内存，现在从json读取，
    // 性能较差，真实情况可以选用其他更好的持久化方式，例如数据库，并且可以只读取热数据，防止本地内存过大
    @PostConstruct
    public void init() {
        //初始化数据
        List<Stock> stocks = JSON_FILE_REPOSITORY.readAll();
        addStock(stocks);
    }

    /**
     * 库存扣减入口
     *
     * @param sku 库存sku
     * @param num 扣减值
     * @return 是否扣减成功
     */
    public boolean deductionStock(String sku, Long num) {
        try {
            lock.lock();
            //根据sku查询到数据
            Stock stock = query(sku);
            if (Objects.isNull(stock)) {
                return false;
            }
            //扣减库存
            AtomicLong atomicLong = stock.availableStock;
            long befor = atomicLong.get();
            long after = befor - num;
            log.info("扣减前库存：{}，扣减后库存：{}，扣减sku：{}，扣减数量：{}", befor, after, sku, num);

            if (after >= 0L) {
                //更新数据
                stock.availableStock.set(after);
                DATA_POOLS.put(stock.id, stock);
                JSON_FILE_REPOSITORY.push(new StockMessage(StockMessage.OperatorEnum.UPDATE, stock));
                return true;
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean increaseStock(String sku, Long num, String creator) {
        try {
            lock.lock();
            //查询库存
            Stock stock = query(sku);
            if (Objects.isNull(stock)) {
                stock = buildStock(sku, creator);
            }
            //增加库存
            stock.availableStock.addAndGet(num);
            persistence(stock);
            return true;
        } finally {
            lock.unlock();
        }
    }


    public Stock buildStock(String sku, String creator) {
        Stock stock = new Stock(ID_INCREASE.incrementAndGet(), sku, new AtomicLong(0), creator, DateUtil.format(DateTime.now(), "yyyy-MM-dd HH:mm:ss,SSS"));

        DATA_POOLS.put(stock.id, stock);
        SKU_ID_MAP.put(stock.sku, stock.id);
        CREATOR_ID_MAP.put(CREATOR_ID_FUNCTION.apply(creator, stock.id), stock.id);
        CREATE_TIME_ID_MAP.put(CREATOR_ID_FUNCTION.apply(stock.createTime, stock.id), stock.id);
        JSON_FILE_REPOSITORY.push(new StockMessage(StockMessage.OperatorEnum.CREATE, stock));
        return stock;
    }

    public void addStock(List<Stock> stocks) {
        if (CollUtil.isEmpty(stocks)) {
            return;
        }
        for (Stock stock : stocks) {
            DATA_POOLS.put(stock.id, stock);
            SKU_ID_MAP.put(stock.sku, stock.id);
            CREATOR_ID_MAP.put(CREATOR_ID_FUNCTION.apply(stock.creator, stock.id), stock.id);
            CREATE_TIME_ID_MAP.put(CREATOR_ID_FUNCTION.apply(stock.createTime, stock.id), stock.id);
        }

    }


    public  boolean removeStock(String sku) {
        try {
            Stock stock = DATA_POOLS.get(SKU_ID_MAP.getOrDefault(sku, DEFAULT_ID));
            if (Objects.isNull(stock))
                return true;
            SKU_ID_MAP.remove(stock.sku);
            CREATOR_ID_MAP.remove(CREATOR_ID_FUNCTION.apply(stock.creator, stock.id));
            CREATE_TIME_ID_MAP.remove(CREATOR_ID_FUNCTION.apply(stock.createTime.toString(), stock.id));
            return true;
        }finally {
            lock.unlock();
        }
    }

    public Stock query(String sku) {
        return DATA_POOLS.get(SKU_ID_MAP.getOrDefault(sku, DEFAULT_ID));
    }

    public Stock query(Long id) {
        return DATA_POOLS.get(id);
    }

    public List<Stock> queryByCreator(String creator) {
        SortedMap<String, Long> map = CREATOR_ID_MAP.subMap(CREATOR_ID_FUNCTION.apply(creator, 0L), CREATOR_ID_FUNCTION.apply(creator, Long.MAX_VALUE));
        return map.values().stream().map(it -> DATA_POOLS.get(it)).filter(Objects::nonNull).collect(Collectors.toList());
    }

    public List<Stock> queryByCreateTime(String start, String end) {
        SortedMap<String, Long> map = CREATE_TIME_ID_MAP.subMap(CREATOR_ID_FUNCTION.apply(start, 0L), CREATOR_ID_FUNCTION.apply(end, Long.MAX_VALUE));
        return map.values().stream().map(it -> DATA_POOLS.get(it)).filter(Objects::nonNull).collect(Collectors.toList());
    }


    public void persistence(Stock stock) {
        JSON_FILE_REPOSITORY.create(stock);
    }


    public static void main(String[] args) throws InterruptedException {
        StockManager stockManager = new StockManager();
        String sku = "药品", name = "lcc";

        String[] names = new String[]{"lcc", "lkk", "ccc", "lyj", "lyp"};

        CountDownLatch countDownLatch = new CountDownLatch(1000);
        Random random = new Random();

        StopWatch stopWatch = new StopWatch("扣减库存");
        stopWatch.start();
        for (int i = 0; i < 1000; i++) {
//            new Thread(() -> {
//                stockManager.increaseStock(sku, 1L, names[random.nextInt(names.length)]);
//                countDownLatch.countDown();
//            }).start();
            stockManager.increaseStock(sku + random.nextInt(50000), 1L, names[random.nextInt(names.length)]);
        }
//        countDownLatch.await();

    }


}
