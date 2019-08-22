package com.gateway.filter.support;

import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;

import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import com.gateway.filter.AbstractGatewayWebFilter;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * @author onlyone
 */
@Order(-1)
@Component
@Slf4j
public class EntranceFilter extends AbstractGatewayWebFilter {

    @Resource
    private ExecutorService gatewayCoreAsyncRequestExecutor;

    // 如果有后置逻辑，需要覆写filter
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        log.error("[EntranceFilter] order(-1) 前前前前置处理");

        String path = exchange.getRequest().getURI().getPath();
        if (path != null && path.indexOf("check_backend_active") >= 0) {
            ServerHttpResponse response = exchange.getResponse();
            log.error("心跳检测线程：" + Thread.currentThread().getName());
            return response.writeWith(Mono.just(response.bufferFactory().wrap("success".getBytes())));
        }

        // 切换线程池，指定上游源头的调度器
        return chain.filter(exchange).subscribeOn(Schedulers.fromExecutorService(gatewayCoreAsyncRequestExecutor));
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
