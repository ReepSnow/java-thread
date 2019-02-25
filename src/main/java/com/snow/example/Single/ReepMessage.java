package com.snow.example.Single;

public class ReepMessage implements Message {

    @Override
    public void printMessage() {
        System.out.println("实例化对象操作");
    }
    private ReepMessage(String countryCode){
        printMessage();
        System.out.println("接收到的参数是："+countryCode);
    }
    private volatile static ReepMessage reepMessage;

    public static ReepMessage getSingleton(String countryCode) {
        if (reepMessage == null) {
            synchronized (ReepMessage.class) {
                if (reepMessage == null) {
                    reepMessage = new ReepMessage(countryCode);
                }
            }
        }
        return reepMessage;
    }
}
