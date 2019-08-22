package com.gateway.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
@Slf4j
public class OrderController {

    @RequestMapping(value = "/query")
    public String apiRequestHandleForGet(HttpServletRequest request, HttpServletResponse response) throws Exception {

        Long orderId = Long.valueOf(request.getParameter("orderId"));
        Long time = Long.valueOf(request.getParameter("time"));

        log.error("接到订单查询请求，订单号：{}", orderId);
        // 模拟业务处理
        Thread.sleep(time);

        return "查到订单：" + orderId;
    }
}
