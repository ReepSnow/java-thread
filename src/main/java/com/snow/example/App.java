package com.snow.example;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args )
    {
        ThreadLocal<Object> threadLocal1 = new ThreadLocal<>();
        ThreadLocal<Object> threadLocal2 = new ThreadLocal<>();
        threadLocal1.set(1);
        System.out.println( 111*10/100);
    }
}
