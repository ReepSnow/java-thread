package com.snow.example.thread;

import java.util.concurrent.*;

/**
 *      Executor框架提供的Executors提供了五个方法
 *
 *      newFixedThreadPool：该方法返回一个固定线程数量的线程池；
 *      newSingleThreadExecutor：该方法返回一个只有一个现成的线程池；
 *      newSingleThreadScheduledExecutor：该方法和 newSingleThreadExecutor 的区别是给定了时间执行某任务的功能，可以进行定时执行等；
 *      newCachedThreadPool：返回一个可以根据实际情况调整线程数量的线程池；
 *      newScheduledThreadPool：在newSingleThreadScheduledExecutor的基础上可以指定线程数量
 *
 *      上面五个方法创建线程池是指调用的还是 ThreadPoolExecutor,该方法的参数有
 *      1、corePoolSize 核心线程池大小；
 *      2、maximumPoolSize 线程池最大容量大小；
 *      3、keepAliveTime 线程池空闲时，线程存活的时间；
 *      4、TimeUnit 时间单位；
 *      5、ThreadFactory 线程工厂；
 *      6、BlockingQueue任务队列；
 *      7、RejectedExecutionHandler 线程拒绝策略；
 *
 *
 */
public class ThreadPoolDemo {

    /**
     * @param args
     */
    public static void main(String[] args) {
        testExecute();
    }

    /**
     * 正常都是使用ThreadPoolExecutor来创建线程池
    * */
    public static void testThreadPoolExecutor(){
        ThreadPoolExecutor pool = new ThreadPoolExecutor(2, 2, 0L,
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(10),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy());

        for (int i = 0; i < 10; i++) {
            int index = i;
            pool.submit(() -> System.out.println("i:" + index + " executorService"));
        }
        pool.shutdown();
    }

    /**
     * 测试submit,该方法发生异常，不会抛出来
     * 使用 submit(Runnable task) 的时候，错误的堆栈信息跑出来的时候会被内部捕获到，所以打印不出来具体的信息让我们查看
     */
    public static void testSubmit(){
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 5; i++) {
            int index = i;
            executorService.submit(() -> divTask(100, index));
        }
        executorService.shutdown();
    }
    /**
     * 测试execute,该方法发生异常，会抛出来
     */
    public static void testExecute(){
        ExecutorService executorService = Executors.newFixedThreadPool(4);

        for (int i = 0; i < 5; i++) {
            int index = i;
            executorService.execute(() -> divTask(100, index));
        }
        executorService.shutdown();
    }

    public static void testSubmitFuture (){
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 5; i++) {
            int index = 1;
            Future future = executorService.submit(() -> divTask(200, index));
            try {
                //这种方式实现RunAble接口得到的future对象实际是ScheduledFutureTask线程的运行结果
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            executorService.shutdown();
        }
    }

    public static void testSubmitCallable (){
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        for (int i = 0; i < 5; i++) {
            int index = 1;
            //这种方式实现Callable接口得到的future对象实际是你实际业务返回的对象
            Future future = executorService.submit(new Callable<Object>() {
                                                       @Override
                                                       public Object call() throws Exception {
                                                           return  1;
                                                       }
                                                   }
            );
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
            executorService.shutdown();
        }
    }
    private static void divTask(int a, int b) {
        double result = a / b;
        System.out.println(result);
    }

}
