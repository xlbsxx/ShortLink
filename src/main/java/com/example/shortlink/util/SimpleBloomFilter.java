package com.example.shortlink.util;

import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import org.springframework.stereotype.Component;

@Component
public class SimpleBloomFilter {
    private BloomFilter<String> bloomFilter=BloomFilter.create(Funnels.stringFunnel(Charsets.UTF_8), 500000000,  0.001);

    public void insert(String value) {
        bloomFilter.put(value);
    }

    public boolean contains(String value) {
        return bloomFilter.mightContain(value);
    }
}

