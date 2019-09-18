package com.gateway.monitor;

import java.util.concurrent.*;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import com.gateway.util.LogUtil;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @author onlyone
 */
@Component
@Slf4j
public class ThreadMonitor {

    @Resource
    private ThreadPoolExecutor gatewayCoreAsyncRequestExecutor;

    @Resource
    private ThreadPoolExecutor gatewayCallbackExecutor;

    @PostConstruct
    public void handle() {

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleWithFixedDelay(() -> {

            String commonFormat = "ActiveCount（%d）,PoolSize（%d）,queueSize（%d）,completedTaskCount（%d）,TaskCount（%d）";
            String coreFormat = "core线程池：" + commonFormat;
            String core = String.format(coreFormat, gatewayCoreAsyncRequestExecutor.getActiveCount(),
                                        gatewayCoreAsyncRequestExecutor.getPoolSize(),
                                        gatewayCoreAsyncRequestExecutor.getQueue().size(),
                                        gatewayCoreAsyncRequestExecutor.getCompletedTaskCount(),
                                        gatewayCoreAsyncRequestExecutor.getTaskCount());
            LogUtil.logRecord(log, core);

            String backFormat = "back线程池：" + commonFormat;
            String back = String.format(backFormat, gatewayCallbackExecutor.getActiveCount(),
                                        gatewayCallbackExecutor.getPoolSize(),
                                        gatewayCallbackExecutor.getQueue().size(),
                                        gatewayCallbackExecutor.getCompletedTaskCount(),
                                        gatewayCallbackExecutor.getTaskCount());
//            LogUtil.logRecord(log, back);

        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

}
