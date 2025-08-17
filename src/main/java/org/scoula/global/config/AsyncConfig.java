package org.scoula.global.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import lombok.extern.log4j.Log4j2;

@Configuration
@EnableAsync(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@Log4j2
public class AsyncConfig implements AsyncConfigurer {

      @Override
      @Bean(name = "taskExecutor")
      public Executor getAsyncExecutor() {
          ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
          executor.setCorePoolSize(5);
          executor.setMaxPoolSize(10);
          executor.setQueueCapacity(100);
          executor.setThreadNamePrefix("AsyncExecutor-");
          executor.setWaitForTasksToCompleteOnShutdown(true);
          executor.setAwaitTerminationSeconds(60);
          executor.initialize();

          log.info(
                  "비동기 실행자 설정 완료 - CorePoolSize: {}, MaxPoolSize: {}",
                  executor.getCorePoolSize(),
                  executor.getMaxPoolSize());

          return executor;
      }

      @Bean(name = "aiTaskExecutor")
      public Executor aiTaskExecutor() {
          ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
          executor.setCorePoolSize(3);
          executor.setMaxPoolSize(6);
          executor.setQueueCapacity(50);
          executor.setThreadNamePrefix("AI-AsyncExecutor-");
          executor.setWaitForTasksToCompleteOnShutdown(true);
          executor.setAwaitTerminationSeconds(120);
          executor.initialize();

          log.info(
                  "AI 비동기 실행자 설정 완료 - CorePoolSize: {}, MaxPoolSize: {}",
                  executor.getCorePoolSize(),
                  executor.getMaxPoolSize());

          return executor;
      }
}
