package com.gateway.filter.support;

import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;

import com.gateway.filter.AbstractGatewayWebFilter;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * 限流过滤器
 */
@Order(2)
@Component
@Slf4j
public class RateLimitFilter extends AbstractGatewayWebFilter {

    @Resource
    private ExecutorService gatewayCallbackExecutor;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        log.error("[RateLimitFilter] order(2) 前前前前置处理");

        return chain.filter(exchange).publishOn(Schedulers.fromExecutorService(gatewayCallbackExecutor)).then(Mono.fromRunnable(() -> {
            log.error("[RateLimitFilter] order(2) 后置处理");
        }));
    }

    @Override
    protected Mono<Boolean> doFilter(ServerWebExchange exchange, WebFilterChain chain) {
        return Mono.just(true);
    }

    @Override
    protected Mono<Void> doDenyResponse(ServerWebExchange exchange) {
        return Mono.empty();
    }

}
