package com.snow.example.Single;

public class ReepMessageFactory implements MessageFactory {

    @Override
    public Message newMessage(String countryCode) {
        return ReepMessage.getSingleton(countryCode);
    }
}
