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

WebClient 增加超时配置 `2秒`

http://localhost:8080/route?appKey=111&targetUrl=http%3A%2F%2Flocalhost%3A8091%2Forder%2Fquery%3ForderId%3D111%26time%3D2000


```
{
    "code": 1001,
    "message": "网络超时"
}
```

5、线程切换

* 当借助WebClient发起异步请求后，调用线程即可释放，交由netty的线程处理


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

* 采用Jmeter压测，启动6个线程（并发6），循环3次，共发出18次请求
* 转发地址：http://localhost:8091/order/query?orderId=111&time=9000，模拟超时休眠9秒

```
2019-09-18 16:42:24.036 ERROR 41039 --- [ctor-http-nio-1] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [7056]
2019-09-18 16:42:24.036 ERROR 41039 --- [ctor-http-nio-3] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [9241]
2019-09-18 16:42:24.036 ERROR 41039 --- [ctor-http-nio-2] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [9660]
2019-09-18 16:42:24.036 ERROR 41039 --- [ctor-http-nio-4] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [782]
2019-09-18 16:42:24.044 ERROR 41039 --- [    core-pool-2] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [782]
2019-09-18 16:42:24.044 ERROR 41039 --- [    core-pool-1] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [9660]
2019-09-18 16:42:24.044 ERROR 41039 --- [ctor-http-nio-2] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [6909]
2019-09-18 16:42:24.045 ERROR 41039 --- [    core-pool-3] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [7056]
2019-09-18 16:42:24.045 ERROR 41039 --- [    core-pool-2] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [782]
2019-09-18 16:42:24.045 ERROR 41039 --- [    core-pool-0] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [9241]
2019-09-18 16:42:24.046 ERROR 41039 --- [    core-pool-0] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [9241]
2019-09-18 16:42:24.046 ERROR 41039 --- [    core-pool-1] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [9660]
2019-09-18 16:42:24.046 ERROR 41039 --- [    core-pool-3] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [7056]
2019-09-18 16:42:24.048 ERROR 41039 --- [ctor-http-nio-3] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [600]
2019-09-18 16:42:24.066 ERROR 41039 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（4）,PoolSize（4）,queueSize（2）,completedTaskCount（0）,TaskCount（6）
2019-09-18 16:42:24.202 ERROR 41039 --- [    core-pool-3] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [6909]
2019-09-18 16:42:24.202 ERROR 41039 --- [    core-pool-2] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [600]
2019-09-18 16:42:24.202 ERROR 41039 --- [    core-pool-2] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [600]
2019-09-18 16:42:24.202 ERROR 41039 --- [    core-pool-3] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [6909]
2019-09-18 16:42:25.070 ERROR 41039 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,PoolSize（4）,queueSize（0）,completedTaskCount（6）,TaskCount（6）
2019-09-18 16:42:26.071 ERROR 41039 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,PoolSize（4）,queueSize（0）,completedTaskCount（6）,TaskCount（6）
2019-09-18 16:42:27.076 ERROR 41039 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,PoolSize（4）,queueSize（0）,completedTaskCount（6）,TaskCount（6）
2019-09-18 16:42:28.081 ERROR 41039 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,PoolSize（4）,queueSize（0）,completedTaskCount（6）,TaskCount（6）
2019-09-18 16:42:29.086 ERROR 41039 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,PoolSize（4）,queueSize（0）,completedTaskCount（6）,TaskCount（6）
2019-09-18 16:42:30.091 ERROR 41039 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,PoolSize（4）,queueSize（0）,completedTaskCount（6）,TaskCount（6）
2019-09-18 16:42:31.095 ERROR 41039 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,PoolSize（4）,queueSize（0）,completedTaskCount（6）,TaskCount（6）
2019-09-18 16:42:32.100 ERROR 41039 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,PoolSize（4）,queueSize（0）,completedTaskCount（6）,TaskCount（6）
2019-09-18 16:42:33.104 ERROR 41039 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,PoolSize（4）,queueSize（0）,completedTaskCount（6）,TaskCount（6）
2019-09-18 16:42:33.247 ERROR 41039 --- [ctor-http-nio-1] c.gateway.controller.ApiProxyController  : [ApiProxyController] 将数据返回给客户端！！！！！！！ [7056]
2019-09-18 16:42:33.247 ERROR 41039 --- [ctor-http-nio-3] c.gateway.controller.ApiProxyController  : [ApiProxyController] 将数据返回给客户端！！！！！！！ [9660]
2019-09-18 16:42:33.247 ERROR 41039 --- [ctor-http-nio-2] c.gateway.controller.ApiProxyController  : [ApiProxyController] 将数据返回给客户端！！！！！！！ [782]
2019-09-18 16:42:33.389 ERROR 41039 --- [ctor-http-nio-3] c.gateway.controller.ApiProxyController  : [ApiProxyController] 将数据返回给客户端！！！！！！！ [6909]
2019-09-18 16:42:33.391 ERROR 41039 --- [ctor-http-nio-3] c.gateway.controller.ApiProxyController  : [ApiProxyController] 将数据返回给客户端！！！！！！！ [9241]
```
结论：

* gatewayCoreAsyncRequestExecutor 执行完3个前置拦截器后，然后通过WebClient将请求发送出去，然后线程就释放了，`无需等待结果返回`
* 待netty拿到响应结果后，通过`[ctor-http-nio] `线程回调给上层的回调函数

压测结果（业务模拟处理 800ms）：

* 线程数：50， 循环次数：10，QPS：60，RT：809
* 线程数：100，循环次数：10，QPS：120，RT：812
* 线程数：200，循环次数：10，QPS：232，RT：822  （较合理）
* 线程数：300，循环次数：10，QPS：242，RT：1175
* 线程数：500，循环次数：10，QPS：245，RT：1930


8、ApiProxyController和RateLimitFilter各切一次线程

过程，当后面业务集群数据返回时，nio-->callback--->nio(响应给client)---->callback

```
2019-08-23 00:32:37.053 ERROR 13776 --- [ctor-http-nio-2] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [604]
2019-08-23 00:32:37.057 ERROR 13776 --- [    core-pool-0] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [604]
2019-08-23 00:32:37.058 ERROR 13776 --- [    core-pool-0] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [604]
2019-08-23 00:32:42.144 ERROR 13776 --- [    back-poll-0] c.gateway.controller.ApiProxyController  : [ApiProxyController] 将数据返回给客户端！！！！！！！ [604]
2019-08-23 00:32:42.161 ERROR 13776 --- [    back-poll-1] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 后置处理 [604]
2019-08-23 00:32:42.162 ERROR 13776 --- [    back-poll-1] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 后置处理 [604]
```


参考：

http://wanshi.iteye.com/blog/2410210
