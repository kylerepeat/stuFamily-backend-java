package com.stufamily.backend.shared.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.stufamily.backend.**.infrastructure.persistence.mapper")
public class MybatisConfig {
}

