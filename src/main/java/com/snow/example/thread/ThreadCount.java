package com.snow.example.thread;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;

public class ThreadCount {
    public static void main(String[] args) throws InterruptedException {

        //testCountDownLatch();
        testCyclicBarrier();
    }

    public static void testCountDownLatch() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(5);
        new Thread(()->countDownLatch.countDown()).start();
        new Thread(()->countDownLatch.countDown()).start();
        new Thread(()->countDownLatch.countDown()).start();
        new Thread(()->countDownLatch.countDown()).start();
        new Thread(()->countDownLatch.countDown()).start();
        countDownLatch.await();
        System.out.println("所有线程处理完毕");
    }

    public static void testCyclicBarrier(){
        final int THREAD_COUNT_NUM = 7;
        CyclicBarrier cyclicBarrier = new CyclicBarrier(THREAD_COUNT_NUM, new Runnable() {
            @Override
            public void run() {
                System.out.println("所有线程已经创建就绪");
            }
        });
        for(int i=1;i<=THREAD_COUNT_NUM; i++){
            new Thread( ()->{
                System.out.println("创建了"+Thread.currentThread().getName()+"线程");
                try {
                    cyclicBarrier.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (BrokenBarrierException e) {
                    e.printStackTrace();
                }}).start();

        }
    }
}
