package com.example.swxh;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

@RestController
@Slf4j
public class StockController {

    @Resource
    StockManager stockManager;

    @PostMapping("/deductionStock")
    public boolean deductionStock(@RequestParam("sku") String sku, @RequestParam("num") Long num) {
        log.info("deductionStock sku: " + sku + " num: " + num);
        boolean b = stockManager.deductionStock(sku, num);
        if (b) {
            log.info("成功减少库存，sku:{},num:{}", sku, num);
        } else {
            log.info("库存不足，sku:{},num:{}", sku, num);
        }
        return b;
    }

    @PostMapping("/increaseStock")
    public boolean increaseStock(@RequestParam("sku") String sku, @RequestParam("num") Long num, @RequestParam("creator") String creator) {
        log.info("开始增加库存，sku:{}，num:{}，creator:{}", sku, num, creator);
        stockManager.increaseStock(sku, num, creator);
        return true;
    }


}
