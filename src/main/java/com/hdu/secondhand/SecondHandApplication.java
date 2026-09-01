package com.hdu.secondhand;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 第二手（SecondHand AI）后端启动类
 * 校园二手交易平台（AI版） · 田博开发
 *
 * <p>Mapper 注册方式：默认使用本类上的 @MapperScan 扫描（生产，需 mybatis-spring 3.x）；
 * 本地离线环境（mybatis-spring 2.1.2）请通过 spring.profiles.active=local 激活
 * {@link com.hdu.secondhand.config.MapperManualConfig} 手动注册。</p>
 */
@SpringBootApplication
public class SecondHandApplication {

    public static void main(String[] args) {
        SpringApplication.run(SecondHandApplication.class, args);
    }
}
