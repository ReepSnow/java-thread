package com.snow.example.thread;

/**
 * @author wangpengtao
 * @date 2019/2/17  13:36
 * @EMAIL wptxc@foxmail.com
 */

/**
 * 查看源码可知ThreadLocal的数据最终是经过threadLocal的set方法塞到各个线程的threadLocals变量（threadLocals变量的类型是ThreadLocal.ThreadLocalMap）
 * 一个 Thread 中只有一个 ThreadLocalMap，一个 ThreadLocalMap 中可以有多个 ThreadLocal 对象
 * ，其中一个 ThreadLocal 对象对应一个 ThreadLocalMap 中的一个 Entry（也就是说：一个 Thread 可以依附有多个 ThreadLocal 对象）。
 *
 *
 * ThreadLocal 的实现是这样的：每个 Thread 维护一个 ThreadLocal.ThreadLocalMap 映射表，这个映射表的 key 是 ThreadLocal实例本身，value 是真正需要存储的 Object
 * 也就是说 ThreadLocal 本身并不存储值，它只是作为一个 key 来让线程从 ThreadLocalMap 获取 value。ThreadLocalMap 是使用 ThreadLocal 的弱引用作为 Key 的，弱引用的对象在 GC 时会被回收。
 * 但是value不会被回收所以存在内存泄漏，最好的做法是使用完threadLocal后将调用 threadlocal 的 remove 方法，
 */
public class ThreadLocalDemo {
    public static void main(String[] args) {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();

        new Thread(() -> {
            threadLocal.set("王五");
            System.out.println(Thread.currentThread().getName());
            System.out.println(threadLocal.get());
        }).start();

        new Thread(() -> {
            try {
                Thread.sleep(1000);

                threadLocal.set("Chinese");
                System.out.println(Thread.currentThread().getName());
                System.out.println(threadLocal.get());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}
