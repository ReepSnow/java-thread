package com.snow.example.thread.threadPool;

import java.util.Date;
import java.util.concurrent.TimeUnit;

public class TestPool {

    public static void main(String[] args) {
        System.out.println(new Date().getTime());
        DiscountThreadPoolManager.getDemoScheduledPool().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(new Date().getTime());
            }
        },2,5, TimeUnit.SECONDS);
    }


}
