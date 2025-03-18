package com.example.swxh;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicLong;

public class Stock implements Serializable {
    public Long id;
    public String sku;
    public AtomicLong availableStock;
    public String creator;

    public String createTime;

    public Stock(){

    }

    public Stock(long id, String sku, AtomicLong availableStock, String creator, String createTime) {
        this.id = id;
        this.sku = sku;
        this.availableStock = availableStock;
        this.creator = creator;
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "Stock{" +
                "id=" + id +
                ", sku='" + sku + '\'' +
                ", availableStock=" + availableStock +
                ", creator='" + creator + '\'' +
                ", createTime=" + createTime +
                '}';
    }

    public Long getId(){
        return id;
    }
}
