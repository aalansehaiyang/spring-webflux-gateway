1.http/rest api网关：作为反向代理，对外部系统提供统一的服务访问入口，进行鉴权、授权、限流等访问控制，通过后将请求转发给后端服务。

2.高性能：反应式&异步非阻塞io，springboot2+spring5 webflux包括webclient,webfilter、controller

3.详细介绍见[基于spring webflux的高性能rest api网关]()


---
### 示例

1、 鉴权不过

http://localhost:9988/route?

```
{
    "code": 2000,
    "message": "token鉴权不过"
}
```

2、目标url参数不合法

http://localhost:9988/route?appKey="111"&targetUrl=“22222”

```
{
    "code": 1000,
    "message": "系统异常"
}
```

3、目标url合法

http://localhost:9988/route?appKey="111"&targetUrl=https://www.baidu.com

```
正常结果。。。。

```

4、转发http超时

WebClient 增加超时配置

http://localhost:9988/route?appKey="111"&targetUrl=https://www.baidu.com


```
{
    "code": 1001,
    "message": "网络超时"
}
```

5、线程切换

```
2019-08-22 18:01:46.031 ERROR 78970 --- [    core-pool-0] c.d.h.a.g.access.filter.RateLimitFilter  : [RateLimitFilter] order(2) 后置处理
2019-08-22 18:01:46.729 ERROR 78970 --- [ctor-http-nio-2] c.d.h.a.g.a.c.ApiProxyController         : [ApiProxyController] 代理转发、处理！！！！！！！
2019-08-22 18:01:46.816 ERROR 78970 --- [    back-poll-0] c.d.h.a.g.access.filter.AuthFilter       : [AuthFilter] order(1) 后置处理
```

6、增加Nginx的心跳检测


```

```


参考：

http://wanshi.iteye.com/blog/2410210
