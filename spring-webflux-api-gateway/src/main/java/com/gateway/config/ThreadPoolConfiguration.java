package com.gateway.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import lombok.extern.slf4j.Slf4j;

@Configuration
@Slf4j
public class ThreadPoolConfiguration {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService gatewayCoreAsyncRequestExecutor() {
        ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(4, 4, 60, TimeUnit.SECONDS,
                                                                 new LinkedBlockingQueue<>(10000),
                                                                 new ThreadFactoryBuilder().setNameFormat("core-pool-%d").build(),
                                                                 (r,
                                                                  executor) -> log.error("Core async thread pool is full! "));
        return taskExecutor;
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService gatewayCallbackExecutor() {
        ThreadPoolExecutor taskExecutor = new ThreadPoolExecutor(2, 2, 60, TimeUnit.SECONDS,
                                                                 new LinkedBlockingQueue<>(10000),
                                                                 new ThreadFactoryBuilder().setNameFormat("back-poll-%d").build(),
                                                                 (r,
                                                                  executor) -> log.error("callback thread pool is full! "));
        return taskExecutor;
    }
}
