package com.example.swxh;

public class StockMessage {
    public OperatorEnum operator;
    public Stock stock;
    public StockMessage(OperatorEnum operator, Stock stock) {
        this.operator = operator;
        this.stock = stock;
    }

    public static   enum OperatorEnum {
        CREATE,
        UPDATE,
        DELETE
    }
}
