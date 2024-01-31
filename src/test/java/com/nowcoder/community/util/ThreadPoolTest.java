package com.nowcoder.community.util;

import com.nowcoder.community.service.impl.AlphaService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@SpringBootTest
public class ThreadPoolTest {
    private final Logger log = LoggerFactory.getLogger(ThreadPoolTest.class);

    // JDK普通线程池
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    // JDK可执行定时任务的线程池
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);

    // Spring普通线程池
    @Autowired
    private ThreadPoolTaskExecutor taskExecutor;

    // Spring可执行定时任务的线程池
    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private AlphaService alphaService;

    public void sleep(long m) {
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    // JDK普通线程池
    @Test
    void testExecutorService() {
        Runnable task = () -> log.debug("Hello ExecutorService");

        for (int i = 0; i < 10; i++) {
            executorService.submit(task);
        }

        sleep(10000);
    }

    // JDK定时任务线程池
    @Test
    void testScheduledExecutorService() {
        Runnable task = () -> log.debug("Hello ScheduledExecutorService");

        scheduledExecutorService.scheduleAtFixedRate(task, 3000, 1000, TimeUnit.MILLISECONDS);

        sleep(20000);
    }

    // Spring普通线程池
    @Test
    void testThreadPoolTaskExecutor() {
        Runnable task = () -> log.debug("Hello ThreadPoolTaskExecutor");

        for (int i = 0; i < 10; i++) {
            taskExecutor.execute(task);
        }

        sleep(10000);
    }

    // Spring定时任务线程池
    @Test
    void testThreadPoolTaskScheduler() {
        Runnable task = () -> log.debug("Hello ThreadPoolTaskScheduler");

        taskScheduler.scheduleAtFixedRate(task, Instant.now().plusSeconds(5), Duration.ofSeconds(1));

        sleep(20000);
    }

    // Spring普通线程池（简化）
    @Test
    public void testThreadPoolTaskExecutorSimple() {
        for (int i = 0; i < 10; i++) {
            alphaService.execute1();
        }

        sleep(10000);
    }

    // Spring定时任务线程池（简化）
    @Test
    public void testThreadPoolTaskSchedulerSimple() {
        sleep(30000);
    }
}
