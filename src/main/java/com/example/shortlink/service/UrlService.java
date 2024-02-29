package com.example.shortlink.service;

import com.example.shortlink.dao.UrlMapper;
import com.example.shortlink.entity.Event;
import com.example.shortlink.entity.MyRequest;
import com.example.shortlink.event.EventProducer;
import com.example.shortlink.util.Constant;
import com.example.shortlink.util.SimpleBloomFilter;
import com.google.common.hash.Hashing;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class UrlService implements Constant {
    @Autowired
    private UrlMapper urlMapper;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private SimpleBloomFilter simpleBloomFilter;

    @Autowired
    private EventProducer eventProducer;



    /**
     * 短链接生成
     */
    public String longUrlToShortUrl(MyRequest request){
        String longUrl=request.getLongUrl();
        String shortUrl = shortenUrl(longUrl);
        //将生成的短链接存入布隆过滤器
        if(simpleBloomFilter.contains(shortUrl)){//判重，重复了就加上一串随机字符
            shortUrl = shortenUrl(longUrl+generateUUID());
        }

        //将生成的短链接存入Redis和MySQL数据库
        redisTemplate.opsForValue().set(URL_CACHE+":"+shortUrl,longUrl,7, TimeUnit.DAYS);//有效期一周
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date()); // 设置为当前时间
        // 添加一周
        calendar.add(Calendar.DAY_OF_WEEK, 7);
        Date oneWeekLater = calendar.getTime();
        urlMapper.insertUrl(shortUrl,longUrl,oneWeekLater);

        return shortUrl;

    }
    public static String generateUUID() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }
    public static String shortenUrl(String longUrl) {
        // 使用 MurmurHash3 算法生成哈希值
        int hashCode = Hashing.murmur3_32().hashBytes(longUrl.getBytes()).asInt();

        // 转换为六位的 base62 编码
        String base62 = encodeBase62(hashCode);

        return base62;
    }
    private static String encodeBase62(int number) {
        String base62Chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        StringBuilder sb = new StringBuilder();

        // 转换为 base62 编码
        while (number > 0) {
            int remainder = number % 62;
            sb.insert(0, base62Chars.charAt(remainder));
            number /= 62;
        }

        // 补齐六位
        while (sb.length() < 6) {
            sb.insert(0, '0');
        }

        return sb.toString();
    }

    /**
     * 短链接跳转
     */
    public void redirect(String shortUrl, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse){
        //用消息队列异步记录一些其他信息
        Event event=new Event();
        event.setTopic(TOPIC_RECORD);
        event.setIp(httpServletRequest.getRemoteAddr());
        event.setShortUrl(shortUrl);
        eventProducer.fireEvent(event);
        //利用布隆过滤器判断短链接是否存在
        if(!simpleBloomFilter.contains(shortUrl)){
            try {
                httpServletResponse.sendRedirect("/notFound");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        String cacheKey=URL_CACHE+":"+shortUrl;
        //尝试从缓存中获取值
        String longUrl=(String) redisTemplate.opsForValue().get(URL_CACHE+":"+shortUrl);
        int maxRetries = 3; // 最大重试次数
        int retryCount = 0;

        while (true){
            if (longUrl == null) {
                // 如果缓存中不存在，尝试获取锁
                if (tryAcquireLock(cacheKey)) {
                    // 获取锁成功，重新检查缓存
                    longUrl=(String) redisTemplate.opsForValue().get(cacheKey);
                    if (longUrl == null) {
                        // 缓存仍不存在，从数据库中读取长链接
                        longUrl=urlMapper.selectLongUrlByShortUrl(shortUrl);

                        // 将长链接放入缓存
                        redisTemplate.opsForValue().set(cacheKey,longUrl,7, TimeUnit.DAYS);//有效期一周
                    }

                    // 释放锁
                    releaseLock(cacheKey);
                    break;
                } else {
                    // 获取锁失败，等待一段时间后重试
                    try {
                        Thread.sleep(1000); // 休眠一秒钟
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    retryCount++;
                    if(retryCount==maxRetries){
                        longUrl="/notFound";
                    }
                }
            }
        }

        try {
            httpServletResponse.sendRedirect(longUrl);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private boolean tryAcquireLock(String cacheKey) {
        // 实现简化，使用 Redis 的 SETNX 命令作为简单的锁
        String value = "lock";
        String key = cacheKey + ":" +"LOCK" ;
        //setNX 是一个原子操作，它会检查键是否存在，如果不存在则设置，这一系列操作是一个不可分割的单元。
        return(boolean) redisTemplate.execute(new RedisCallback<Boolean>() {
            @Override
            public Boolean doInRedis(RedisConnection connection) {
                byte[] keyBytes = key.getBytes();
                byte[] valueBytes = value.getBytes();
                return connection.setNX(keyBytes, valueBytes);
            }
        });
    }

    public void releaseLock(String cacheKey) {
        String key = cacheKey + ":" + "LOCK";
        redisTemplate.delete(key);
    }
}
