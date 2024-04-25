package com.grazy.config;

import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * @Author: grazy
 * @Date: 2023-11-08 16:05
 * @Description: 配置线程池
 */

@Configuration
@EnableAsync
public class AsyncTaskConfig implements AsyncConfigurer {

    // ThredPoolTaskExcutor的处理流程
    // 当池子大小小于corePoolSize，就新建线程，并处理请求
    // 当池子大小等于corePoolSize，把请求放入workQueue中，池子里的空闲线程就去workQueue中取任务并处理
    // 当workQueue放不下任务时，就新建线程入池，并处理请求，如果池子大小撑到了maximumPoolSize，就用RejectedExecutionHandler来做拒绝处理
    // 当池子的线程数大于corePoolSize时，多余的线程会等待keepAliveTime长时间，如果无请求可处理就自行销毁

    @Override
    @Bean
    public Executor getAsyncExecutor() {
        //创建线程池
        ThreadPoolTaskExecutor threadPoolTaskExecutor = new ThreadPoolTaskExecutor();
        //设置核心线程数
        threadPoolTaskExecutor.setCorePoolSize(10);
        //设置最大线程数
        threadPoolTaskExecutor.setMaxPoolSize(100);
        //线程池所使用的缓冲对列
        threadPoolTaskExecutor.setQueueCapacity(10);
        //等待任务在关机时完成--表明等待全部线程执行完
        threadPoolTaskExecutor.setWaitForTasksToCompleteOnShutdown(true);
        //等待时间（默认为0，此时立即停止）,并没有等待xx秒后强制停止
        threadPoolTaskExecutor.setAwaitTerminationSeconds(60);
        //线程名前缀
        threadPoolTaskExecutor.setThreadNamePrefix("Rabbit-Async-");
        //初始化线程
        threadPoolTaskExecutor.initialize();
        return threadPoolTaskExecutor;
    }


    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return null;
    }

}
