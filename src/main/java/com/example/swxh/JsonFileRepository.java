package com.example.swxh;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONConfig;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
public class JsonFileRepository {
//    private static final String FILE_PATH =JsonFileRepository.class.getClassLoader().getResource("entities.json").getPath();


    BlockingDeque<StockMessage> queue = new LinkedBlockingDeque<>();


    private static final String FILE_PATH = "entities.json";
    JSONConfig jsonConfig = new JSONConfig();

    {
        jsonConfig.setOrder(true);
    }

    @PostConstruct
    public void init() {
        // 启动消费者线程
        new Thread(this::consume).start();
    }

    public void consume() {

        while (true) {

            // 阻塞取出队列中的消息
            StockMessage poll = queue.poll();
            if (poll == null) {
                continue;
            }
            //转发操作
            switch (poll.operator) {
                case CREATE:
                    // 创建
                    create(poll.stock);
                    break;
                case UPDATE:
                    update(poll.stock);
                    break;
                case DELETE:
                    // 删除
                    delete(poll.stock.id);
                    break;
                default:
                    throw new RuntimeException("操作类型错误");
            }


        }
    }

    // 推送消息到队列，这里选用内存阻塞队列并不是更好的方式
    public boolean push(StockMessage stock) {
        return queue.offer(stock);
    }


    // 读取全部实体
    public List<Stock> readAll() {

        File file = new File(FILE_PATH);
        return JSONUtil.readJSONArray(file, Charset.defaultCharset()).toList(Stock.class);
    }

    // 新增实体（自动追加到数组）
    public void create(Stock entity) {
        List<Stock> stocks = readAll();
        stocks.add(entity);
        JSONArray objects = JSONUtil.parseArray(stocks, jsonConfig);
        FileUtil.writeString(JSONUtil.toJsonStr(objects), new File(FILE_PATH), Charset.defaultCharset());
    }

    // 按ID删除
    public void delete(Long id) {
        List<Stock> stocks = readAll().stream()
                .filter(e -> !e.id.equals(id))
                .collect(Collectors.toList());
        FileUtil.writeString(JSONUtil.toJsonStr(stocks), new File(FILE_PATH), Charset.defaultCharset());
    }

    // 编辑实体
    public synchronized void update(Stock stock) {
        try {
            List<Stock> stocks = readAll();
            LinkedHashMap<Long, Stock> map = stocks.stream()
                    .collect(Collectors.toMap(Stock::getId, Function.identity(), (o1, o2) -> o1, LinkedHashMap::new));
            Stock stock1 = map.get(stock.id);
            stock1.availableStock = stock.availableStock;
            JSONArray objects = JSONUtil.parseArray(stocks, jsonConfig);
            FileUtil.writeString(JSONUtil.toJsonStr(objects), new File(FILE_PATH), Charset.defaultCharset());
        } catch (Exception exception) {
            log.error("更新库存失败", exception);
        }
    }
}
