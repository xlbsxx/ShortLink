package com.example.shortlink.controller;

import com.example.shortlink.entity.MyRequest;
import com.example.shortlink.service.UrlService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class UrlController {
    @Autowired
    private UrlService urlService;

    @RequestMapping("/{url}")
    public void redirect(@PathVariable String url, HttpServletRequest httpServletRequest,HttpServletResponse httpServletResponse){
        urlService.redirect(url,httpServletRequest,httpServletResponse);
    }
    @RequestMapping(path = "/short_url/get" ,method = RequestMethod.POST)
    public void generateShortUrl(@RequestBody MyRequest request){
        urlService.longUrlToShortUrl(request);
    }
}
