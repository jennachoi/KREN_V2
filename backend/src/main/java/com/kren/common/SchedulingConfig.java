package com.kren.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Enables {@code @Scheduled} (SSE heartbeats). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
