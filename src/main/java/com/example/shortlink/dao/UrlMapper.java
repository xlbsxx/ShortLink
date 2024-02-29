package com.example.shortlink.dao;

import com.example.shortlink.entity.Url;
import org.apache.ibatis.annotations.Mapper;

import java.util.Date;
@Mapper
public interface UrlMapper {
    int insertUrl(String shortUrl, String longUrl, Date expireTime);
    String selectLongUrlByShortUrl(String shortUrl);
    int updateIP(String shortUrl,String ip);

}
