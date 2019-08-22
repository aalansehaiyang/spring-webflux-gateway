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

* 当借助WebClient发起异步请求后，调用线程即可释放，交由netty的线程处理
* 

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
2019-08-22 23:43:36.660 ERROR 1066 --- [ctor-http-nio-2] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [9490]
2019-08-22 23:43:36.670 ERROR 1066 --- [    core-pool-0] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [9490]
2019-08-22 23:43:36.672 ERROR 1066 --- [    core-pool-0] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [9490]
2019-08-22 23:43:37.557 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（1）,TaskCount（1）
2019-08-22 23:43:37.557 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（0）,TaskCount（0）
2019-08-22 23:43:38.295 ERROR 1066 --- [ctor-http-nio-3] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [4932]
2019-08-22 23:43:38.297 ERROR 1066 --- [    core-pool-1] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [4932]
2019-08-22 23:43:38.297 ERROR 1066 --- [    core-pool-1] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [4932]
2019-08-22 23:43:38.562 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（2）,TaskCount（2）
2019-08-22 23:43:38.562 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（0）,TaskCount（0）
2019-08-22 23:43:39.566 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（2）,TaskCount（2）
2019-08-22 23:43:39.566 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（0）,TaskCount（0）
2019-08-22 23:43:39.628 ERROR 1066 --- [ctor-http-nio-2] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [6726]
2019-08-22 23:43:39.629 ERROR 1066 --- [    core-pool-2] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [6726]
2019-08-22 23:43:39.629 ERROR 1066 --- [    core-pool-2] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [6726]
2019-08-22 23:43:40.443 ERROR 1066 --- [ctor-http-nio-1] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [6476]
2019-08-22 23:43:40.444 ERROR 1066 --- [    core-pool-3] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [6476]
2019-08-22 23:43:40.444 ERROR 1066 --- [    core-pool-3] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [6476]
2019-08-22 23:43:40.569 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（4）,TaskCount（4）
2019-08-22 23:43:40.569 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（0）,TaskCount（0）
2019-08-22 23:43:41.574 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（4）,TaskCount（4）
2019-08-22 23:43:41.574 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（0）,TaskCount（0）
2019-08-22 23:43:41.827 ERROR 1066 --- [ctor-http-nio-4] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [2534]
2019-08-22 23:43:41.827 ERROR 1066 --- [    core-pool-0] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [2534]
2019-08-22 23:43:41.827 ERROR 1066 --- [    core-pool-0] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [2534]
2019-08-22 23:43:42.547 ERROR 1066 --- [ctor-http-nio-3] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [3388]
2019-08-22 23:43:42.547 ERROR 1066 --- [    core-pool-1] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [3388]
2019-08-22 23:43:42.547 ERROR 1066 --- [    core-pool-1] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [3388]
2019-08-22 23:43:42.581 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（6）,TaskCount（6）
2019-08-22 23:43:42.581 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（0）,TaskCount（0）
2019-08-22 23:43:43.251 ERROR 1066 --- [ctor-http-nio-2] c.gateway.filter.support.EntranceFilter  : [EntranceFilter] order(-1) 前前前前置处理 [5642]
2019-08-22 23:43:43.251 ERROR 1066 --- [    core-pool-2] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 前前前前置处理 [5642]
2019-08-22 23:43:43.251 ERROR 1066 --- [    core-pool-2] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 前前前前置处理 [5642]
2019-08-22 23:43:43.587 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:43.588 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（0）,TaskCount（0）
2019-08-22 23:43:44.592 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:44.592 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（0）,TaskCount（0）
2019-08-22 23:43:45.596 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:45.597 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（0）,TaskCount（0）
2019-08-22 23:43:46.602 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:46.602 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（0）,TaskCount（0）
2019-08-22 23:43:46.863 ERROR 1066 --- [ctor-http-nio-2] c.gateway.controller.ApiProxyController  : [ApiProxyController] 请求转发、处理！！！！！！！ [9490]
2019-08-22 23:43:47.607 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:47.607 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（1）,TaskCount（1）
2019-08-22 23:43:48.316 ERROR 1066 --- [ctor-http-nio-1] c.gateway.controller.ApiProxyController  : [ApiProxyController] 请求转发、处理！！！！！！！ [4932]
2019-08-22 23:43:48.611 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:48.612 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（2）,TaskCount（2）
2019-08-22 23:43:49.615 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:49.615 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（2）,TaskCount（2）
2019-08-22 23:43:49.649 ERROR 1066 --- [ctor-http-nio-4] c.gateway.controller.ApiProxyController  : [ApiProxyController] 请求转发、处理！！！！！！！ [6726]
2019-08-22 23:43:50.462 ERROR 1066 --- [ctor-http-nio-3] c.gateway.controller.ApiProxyController  : [ApiProxyController] 请求转发、处理！！！！！！！ [6476]
2019-08-22 23:43:50.617 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:50.617 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（4）,TaskCount（4）
2019-08-22 23:43:51.620 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:51.620 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（4）,TaskCount（4）
2019-08-22 23:43:51.845 ERROR 1066 --- [ctor-http-nio-2] c.gateway.controller.ApiProxyController  : [ApiProxyController] 请求转发、处理！！！！！！！ [2534]
2019-08-22 23:43:52.563 ERROR 1066 --- [ctor-http-nio-1] c.gateway.controller.ApiProxyController  : [ApiProxyController] 请求转发、处理！！！！！！！ [3388]
2019-08-22 23:43:52.620 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:52.621 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（6）,TaskCount（6）
2019-08-22 23:43:53.264 ERROR 1066 --- [ctor-http-nio-4] c.gateway.controller.ApiProxyController  : [ApiProxyController] 请求转发、处理！！！！！！！ [5642]
2019-08-22 23:43:53.266 ERROR 1066 --- [    back-poll-0] c.g.filter.support.RateLimitFilter       : [RateLimitFilter] order(2) 后置处理 [5642]
2019-08-22 23:43:53.266 ERROR 1066 --- [    back-poll-0] com.gateway.filter.support.AuthFilter    : [AuthFilter] order(1) 后置处理 [5642]
2019-08-22 23:43:53.626 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:53.626 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:54.629 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:54.630 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:55.634 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:55.634 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:56.637 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : core线程池：ActiveCount（0）,CorePoolSize（4）,queueSize（0）,completedTaskCount（7）,TaskCount（7）
2019-08-22 23:43:56.638 ERROR 1066 --- [pool-3-thread-1] com.gateway.monitor.ThreadMonitor        : back线程池：ActiveCount（0）,CorePoolSize（2）,queueSize（0）,completedTaskCount（7）,TaskCount（7）


```

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
