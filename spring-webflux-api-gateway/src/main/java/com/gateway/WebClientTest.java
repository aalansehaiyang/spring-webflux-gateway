package com.gateway;

import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * @author onlyone
 */
public class WebClientTest {

    public static void main(String[] args) throws InterruptedException {
        WebClient webClient = WebClient.create();
        webClient.method(HttpMethod.GET).uri("http://localhost:8091/order/query?orderId=111&time=1000").exchange().onErrorResume(e -> {
            e.printStackTrace();
            return Mono.empty().then(Mono.empty());
        }).flatMap(backendResponse -> {
            System.out.println("接收到结果：");
            Mono<String> responseParamMono = backendResponse.bodyToMono(String.class);
            return responseParamMono.flatMap(s -> {
                System.out.println("输出内容：" + s);
                return Mono.empty().then(Mono.empty());
            });
        }).block();
        System.out.println("任务结束");

        Thread.sleep(15000);
    }
}
