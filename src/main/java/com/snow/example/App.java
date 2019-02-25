package com.snow.example;

import com.snow.example.Single.ReepMessageFactory;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        /*ThreadLocal<Object> threadLocal1 = new ThreadLocal<>();
        ThreadLocal<Object> threadLocal2 = new ThreadLocal<>();
        ConcurrentHashMap concurrentHashMap = new ConcurrentHashMap();
        HashMap hashMap = new HashMap();
        threadLocal1.set(1);
        System.out.println( "Hello World!" );*/
        ReepMessageFactory reepMessageFactory = new ReepMessageFactory();
        reepMessageFactory.newMessage("中国");
    }
}
