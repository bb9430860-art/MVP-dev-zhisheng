package com.zhisheng.mvp;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({
        "com.zhisheng.mvp.process.mapper",
        "com.zhisheng.mvp.production.mapper",
        "com.zhisheng.mvp.inventory.mapper"
})
public class AppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppApplication.class, args);
    }
}
