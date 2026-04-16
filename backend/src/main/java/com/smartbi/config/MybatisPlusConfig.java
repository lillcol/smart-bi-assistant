package com.smartbi.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.smartbi.mapper")
public class MybatisPlusConfig {
}
