package com.gateway.controller;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

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

@RestController
@Slf4j
public class ApiProxyController {

    WebClient webClient                     = WebClient.create();


    // 后端服务超时
    // @Value("${backend.service.timeout.inmillis}")
    long      backendServiceTimeoutInMillis = 3000;

    @RequestMapping("/**")
    public Mono<Void> proxyRequest(ServerWebExchange exchange) {

        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();

        HttpMethod httpMethod = request.getMethod();
        String targetUrl = exchange.getRequest().getQueryParams().getFirst("targetUrl");

        RequestBodySpec reqBodySpec = webClient.method(httpMethod).uri(targetUrl).headers(httpHeaders -> {
            httpHeaders.addAll(request.getHeaders());
            httpHeaders.remove("HOST");
        });

        RequestHeadersSpec<?> reqHeadersSpec;
        if (requireHttpBody(httpMethod)) {
            reqHeadersSpec = reqBodySpec.body(BodyInserters.fromDataBuffers(request.getBody()));
        } else {
            reqHeadersSpec = reqBodySpec;
        }

        return reqHeadersSpec.exchange().timeout(Duration.ofMillis(backendServiceTimeoutInMillis)).onErrorResume(ex -> {

            return Mono.defer(() -> {

                String errorResultJson = "";
                if (ex instanceof TimeoutException) {
                    errorResultJson = "{\"code\":1001,\"message\":\"网络超时\"}";
                } else {
                    errorResultJson = "{\"code\":1000,\"message\":\"系统异常\"}";
                }
                // response.setStatusCode(HttpStatus.UNAUTHORIZED);
                response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return response.writeWith(Mono.just(response.bufferFactory().wrap(errorResultJson.getBytes())));

            }).then(Mono.empty());

        }).flatMap(backendResponse -> {

            // 将后端服务的响应回写到前端resp
            log.error("[ApiProxyController] 请求转发、处理！！！！！！！");
            response.setStatusCode(backendResponse.statusCode());
            response.getHeaders().putAll(backendResponse.headers().asHttpHeaders());
            return response.writeWith(backendResponse.bodyToFlux(DataBuffer.class));

        });
    }

    private boolean requireHttpBody(HttpMethod method) {
        return HttpMethod.POST == method || HttpMethod.PUT == method || HttpMethod.PATCH == method
               || HttpMethod.TRACE == method;
    }

}
