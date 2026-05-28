package com.dm.cn.text2sql.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@Getter
public class TextToSqlCfg {
    private boolean initializeSchema;

    @Value("${server.port}")
    private int port;
    @Value("${server.host}")
    private String host;
}
