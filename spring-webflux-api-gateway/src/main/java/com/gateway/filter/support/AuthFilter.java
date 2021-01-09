//package com.gateway.filter.support;
//
//import com.gateway.constant.KeyConstant;
//import com.gateway.util.LogUtil;
//import org.springframework.core.annotation.Order;
//import org.springframework.http.MediaType;
//import org.springframework.http.server.reactive.ServerHttpResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.web.server.WebFilterChain;
//
//import com.gateway.filter.AbstractGatewayWebFilter;
//
//import lombok.extern.slf4j.Slf4j;
//import reactor.core.publisher.Mono;
//
//@Order(1)
//@Component
//@Slf4j
//public class AuthFilter extends AbstractGatewayWebFilter {
//
//    static final String APPKEY_HTTP_HEAD     = "appKey";
//    static final String WEB_FILTER_ATTR_NAME = "WebFilterChain";
//
//    // 如果有后置逻辑，需要覆写filter
//    @Override
//    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
//        LogUtil.logRecord(log, "[AuthFilter] order(1) 前前前前置处理",
//                          (Integer) exchange.getAttributes().get(KeyConstant.traceId));
//
//        exchange.getAttributes().put(WEB_FILTER_ATTR_NAME, chain);
//
//        String appKey = exchange.getRequest().getQueryParams().getFirst(APPKEY_HTTP_HEAD);
//        if (appKey == null) {
//            String denyHttpBody = "{\"code\":2000,\"message\":\"token鉴权不过\"}";
//            ServerHttpResponse response = exchange.getResponse();
//            response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
//            return response.writeWith(Mono.just(response.bufferFactory().wrap(denyHttpBody.getBytes())));
//        }
//
//        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
//            LogUtil.logRecord(log, "[AuthFilter] order(1) 后置处理",
//                              (Integer) exchange.getAttributes().get(KeyConstant.traceId));
//        }));
//    }
//
//    @Override
//    protected Mono<Boolean> doFilter(ServerWebExchange exchange, WebFilterChain chain) {
//        return Mono.just(true);
//    }
//
//    @Override
//    protected Mono<Void> doDenyResponse(ServerWebExchange exchange) {
//        return Mono.empty();
//    }
//
//}
