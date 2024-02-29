package com.example.shortlink.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {
    @Bean//哪里要用到redis就把这个redisTemplate注入到哪里
    public RedisTemplate<String,Object> redisTemplate(RedisConnectionFactory factory){
        RedisTemplate<String,Object> template=new RedisTemplate<>();
        template.setConnectionFactory(factory);//template有了工厂以后就有了访问数据库的能力
        //设置key的序列化方式
        template.setKeySerializer(RedisSerializer.string());//能够序列化字符串的序列化器
        //设置value的序列化方式
        template.setValueSerializer(RedisSerializer.json());
        //设置hash的key的的序列化方式
        template.setHashKeySerializer(RedisSerializer.string());
        //设置hash的value的的序列化方式
        template.setHashValueSerializer(RedisSerializer.json());

        template.afterPropertiesSet();
        return template;
    }
}
