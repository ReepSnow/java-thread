package com.snow.example.thread;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 这里所说的lock对象指的是lock接口的实现类，一般我们成为lock对象
 */
public class LockDemo {

    public static void main(String[] args) {
        //testLock();
        testLockCondition();
    }


    public static void testLock(){
        Lock lock = new ReentrantLock();
        new Thread(()->runMethod(lock)).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                runMethod(lock);
            }
        }).start();
    }

    /**
     * 使用lock对象的lock和unlock方法 实现 object中的wait跟notify方法
     * @param lock
     */
    public static void runMethod(Lock lock){
        lock.lock();
        for(int i =1; i<5 ; i++){
            System.out.println("thredName:"+Thread.currentThread().getName()+":"+i);
        }
        lock.unlock();
    }


    public static void testLockCondition(){
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();
        new Thread(()->await(condition,lock),"thread1_conditionA").start();
        new Thread(()->signal(condition,lock),"thread2_conditionA").start();


    }
    /**
     * 使用Lock对象跟lock new出来的condition对象实现 object（wait 和notify方法）和synchronize实现线程间的同步
     * Object 的 wait() 方法相当于 Condition 类中的 await() 方法；
     * Object 的 notify() 方法相当于 Condition 类中的 signal() 方法；
     * Object 的 notifyAll() 方法相当于 Condition 类中的 signalAll() 方法；
     */
    public static void await(Condition condition,Lock lock) {
        lock.lock();
        System.out.println("开始等待await！ ThreadName：" + Thread.currentThread().getName());
        try {
            condition.await();
            System.out.println("等待await结束！ ThreadName：" + Thread.currentThread().getName());
        } catch (InterruptedException e) {
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
    }

    public static void signal(Condition condition,Lock lock){
        lock.lock();
        System.out.println("发送通知signal！ ThreadName：" + Thread.currentThread().getName());
        condition.signal();
        lock.unlock();
    }

}
