package com.example.shortlink.entity;

import java.util.HashMap;
import java.util.Map;

public class Event {

    private String topic;
    private String shortUrl;
    private String ip;
    private Map<String, Object> data = new HashMap<>();//以后还可能用到的字段，为了将来的可拓展性

    public String getTopic() {
        return topic;
    }

    public Event setTopic(String topic) {
        this.topic = topic;
        return this;
    }


    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Event setData(String key, Object value) {
        this.data.put(key, value);
        return this;
    }

}
