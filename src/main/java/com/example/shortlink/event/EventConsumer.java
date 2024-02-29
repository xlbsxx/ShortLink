package com.example.shortlink.event;

import com.alibaba.fastjson.JSONObject;
import com.example.shortlink.dao.UrlMapper;
import com.example.shortlink.entity.Event;
import com.example.shortlink.util.Constant;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
public class EventConsumer  implements Constant {
    @Autowired
    private UrlMapper urlMapper;
    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @KafkaListener(topics = {TOPIC_RECORD})
    public void handleRecord(ConsumerRecord record) {
        if (record == null || record.value() == null) {
            logger.error("消息的内容为空!");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);//把JSON字符串解析为对象
        if (event == null) {
            logger.error("消息格式错误!");
            return;
        }
        urlMapper.updateIP(event.getShortUrl(),event.getIp());

    }

}
