package com.snow.example.thread.threadPool;

import java.util.concurrent.*;

/**
 * @author 王鹏涛
 * @Version: 1.0.0
 * @date 2019年5月31日11:03:50
 */
public class DiscountThreadPoolManager {

    /**
     * 普通线程池
     * 使用时注意异常处理问题
     *
     * submit,该方法发生异常，不会抛出来
     * 使用 submit(Runnable task) 的时候，错误的堆栈信息跑出来的时候会被内部捕获到，所以打印不出来具体的信息让我们查看
     *
     * execute,该方法发生异常，会抛出来
     */
    private final static ThreadPoolExecutor demoPool;

    /**
     * 普通调度线程池
     */
    private final static ScheduledExecutorService demoScheduledPool;

    static {

        demoPool = new ThreadPoolExecutor(5, 20, 60, TimeUnit.SECONDS,
                new ArrayBlockingQueue(1000), new DiscountThreadFactory("task-demo-Submit"), new ThreadPoolExecutor.CallerRunsPolicy());

        demoScheduledPool = new ScheduledThreadPoolExecutor(50,
                new DiscountThreadFactory("Task-demo-Schedule"), new ThreadPoolExecutor.CallerRunsPolicy());


    }



    public static ThreadPoolExecutor getDemoPool() {
        return demoPool;
    }

    public static ScheduledExecutorService getDemoScheduledPool() {
        return demoScheduledPool;
    }


    public static void main(String[] args) {
        DiscountThreadPoolManager.getDemoScheduledPool().scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(System.currentTimeMillis());
            }
        },2,5, TimeUnit.SECONDS);

        DiscountThreadPoolManager.getDemoPool().execute(new Runnable() {
            @Override
            public void run() {
                System.out.println("测试");
            }
        });
    }
}
