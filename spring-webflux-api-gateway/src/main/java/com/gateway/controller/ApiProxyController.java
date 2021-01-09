package com.gateway.controller;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;

import com.gateway.constant.KeyConstant;
import com.gateway.util.LogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestBodySpec;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;
import org.springframework.web.server.ServerWebExchange;

import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import javax.annotation.Resource;

@RestController
@Slf4j
public class ApiProxyController {

    @Resource
    private ExecutorService gatewayCallbackExecutor;

    WebClient               webClient                     = WebClient.create();

    // 后端服务超时
    // @Value("${backend.service.timeout.inmillis}")
    long                    backendServiceTimeoutInMillis = 20_000;

    @RequestMapping("/**")
    public Mono<Void> proxyRequest(ServerWebExchange exchange) {

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        HttpMethod httpMethod = request.getMethod();
//        String targetUrl = exchange.getRequest().getQueryParams().getFirst("targetUrl");

        String targetUrl="https://127.0.0.1:9097";
        RequestBodySpec reqBodySpec = webClient.method(httpMethod).uri(targetUrl).contentType(MediaType.valueOf(MediaType.TEXT_PLAIN_VALUE)).headers(httpHeaders -> {
            httpHeaders.addAll(request.getHeaders());
            httpHeaders.remove("HOST");
        });

        RequestHeadersSpec<?> reqHeadersSpec;
        if (requireHttpBody(httpMethod)) {
            reqHeadersSpec = reqBodySpec.body(BodyInserters.fromDataBuffers(request.getBody()));
        } else {
            reqHeadersSpec = reqBodySpec;
        }

        // 切回调线程（感觉没必要，nio->callback-->nio）
        return reqHeadersSpec.exchange().timeout(Duration.ofMillis(backendServiceTimeoutInMillis))
//                .publishOn(Schedulers.fromExecutorService(gatewayCallbackExecutor))
                .onErrorResume(ex -> {

            return Mono.defer(() -> {

                String errorResultJson = "";
                if (ex instanceof TimeoutException) {
                    errorResultJson = "{\"code\":1001,\"message\":\"网络超时\"}";
                } else {
                    errorResultJson = "{\"code\":1000,\"message\":\"系统异常\"}";
                }
                // response.setStatusCode(HttpStatus.UNAUTHORIZED);
//                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return response.writeWith(Mono.just(response.bufferFactory().wrap(errorResultJson.getBytes())));

            }).then(Mono.empty());

        }).flatMap(backendResponse -> {

            LogUtil.logRecord(log, "[ApiProxyController] 将数据返回给客户端！！！！！！！",
                              (Integer) exchange.getAttributes().get(KeyConstant.traceId));

            response.setStatusCode(backendResponse.statusCode());
            response.getHeaders().putAll(backendResponse.headers().asHttpHeaders());

            // 此处响应给client走的是netty线程，格式如： ctor-http-nio-2
            return response.writeWith(backendResponse.bodyToFlux(DataBuffer.class));

        });
    }

    private boolean requireHttpBody(HttpMethod method) {
        return HttpMethod.POST == method || HttpMethod.PUT == method || HttpMethod.PATCH == method
               || HttpMethod.TRACE == method;
    }

}
