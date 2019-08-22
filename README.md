### 简介

API网关：作为反向代理，对外部系统提供统一的服务访问入口，并提供鉴权、限流等基础组件服务，最后将请求转发给后面的业务集群。

特性：基于事件响应、异步非阻塞

框架：springboot2、spring5 webflux

### 重点

* 基于扩展接口`WebFilter`实现子类，组成`WebFilterChain`链，优先级由小到大，顺序执行实现一系列组件功能
* 底层与外界（接收请求、转发请求、转发后异步等待返回数据）通信基于netty，需要借助异步特性+NIO，当有数据响应时通过IO线程触发Mono任务链

### 示例

1、 鉴权不过

http://localhost:8080/route?

```
{
    "code": 2000,
    "message": "token鉴权不过"
}
```

2、目标url参数不合法

http://localhost:8080/route?appKey="111"&targetUrl=“22222”

```
{
    "code": 1000,
    "message": "系统异常"
}
```

3、目标url合法

http://localhost:8080/route?appKey=111&targetUrl=http%3A%2F%2Flocalhost%3A8091%2Forder%2Fquery%3ForderId%3D111%26time%3D2000

转义前：

```
http://localhost:8080/route?appKey=111&targetUrl=http://localhost:8091/order/query?orderId=111&time=2000
```

```
查到订单：111

```

4、转发http超时

WebClient 增加超时配置 `1ms`

http://localhost:8080/route?appKey=111&targetUrl=http%3A%2F%2Flocalhost%3A8091%2Forder%2Fquery%3ForderId%3D111%26time%3D2000


```
{
    "code": 1001,
    "message": "网络超时"
}
```

5、线程切换

```
2019-08-22 20:37:08.953 ERROR 82156 --- [ctor-http-nio-4] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理
2019-08-22 20:37:08.953 ERROR 82156 --- [    core-pool-0] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理
2019-08-22 20:37:08.953 ERROR 82156 --- [    core-pool-0] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理
2019-08-22 20:37:10.967 ERROR 82156 --- [ctor-http-nio-3] c.gateway.controller.ApiProxyController  : [ApiProxyController] 请求转发、处理！！！！！！！
2019-08-22 20:37:10.970 ERROR 82156 --- [    back-poll-1] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 后置处理
2019-08-22 20:37:10.970 ERROR 82156 --- [    back-poll-1] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 后置处理

```

6、增加Nginx的心跳检测

http://localhost:8080/check_backend_active.html

```
success
```

7、业务集群API响应模拟超时，看看大访问情况下，线程池的空闲情况

```


```


参考：

http://wanshi.iteye.com/blog/2410210
