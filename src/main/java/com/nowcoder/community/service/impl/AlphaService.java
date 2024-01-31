package com.nowcoder.community.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AlphaService {
    private final Logger log = LoggerFactory.getLogger(AlphaService.class);

    @Async
    public void execute1() {
        log.info("execute1");
    }

    // @Scheduled(initialDelay = 10000, fixedRate = 1000)
    public void execute2() {
        log.info("execute2");
    }
}
