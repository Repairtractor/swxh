package com.example.swxh;

import cn.hutool.http.HttpUtil;
import org.junit.Test;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public class ThreadTest {

    @Test
    public void test() {
        FutureTask<String> future=new FutureTask<>(()->"hello world");
        Thread thread = new Thread(future);

    }

    @Test
    public void test2() {
        webs
    }
}
