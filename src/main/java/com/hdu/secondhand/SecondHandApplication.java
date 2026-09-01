package com.hdu.secondhand;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第二手（SecondHand AI）后端启动类
 * 校园二手交易平台（AI版） · 田博开发
 */
@SpringBootApplication
@MapperScan("com.hdu.secondhand.mapper")
public class SecondHandApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecondHandApplication.class, args);
    }
}
