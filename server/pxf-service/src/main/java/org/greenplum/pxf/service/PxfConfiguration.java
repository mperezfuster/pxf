package org.greenplum.pxf.service;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import org.apache.commons.lang.StringUtils;
import org.greenplum.pxf.api.configuration.PxfServerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.actuate.metrics.web.servlet.DefaultWebMvcTagsProvider;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsContributor;
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsProvider;
import org.springframework.boot.autoconfigure.task.TaskExecutionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.task.TaskExecutorBuilder;
import org.springframework.boot.task.TaskExecutorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.AsyncAnnotationBeanPostProcessor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;

/**
 * Configures the {@link AsyncTaskExecutor} for tasks that will stream data to
 * clients
 */
@Configuration
@EnableConfigurationProperties(PxfServerProperties.class)
public class PxfConfiguration implements WebMvcConfigurer {

    private static final Logger LOG = LoggerFactory.getLogger(PxfConfiguration.class);

    /**
     * Bean name of PXF's {@link TaskExecutor}.
     */
    public static final String PXF_RESPONSE_STREAM_TASK_EXECUTOR = "pxfResponseStreamTaskExecutor";

    private final ListableBeanFactory beanFactory;

    /**
     * Constructs a PXF Configuration object with the provided
     * {@link ListableBeanFactory}
     *
     * @param beanFactory the beanFactory
     */
    public PxfConfiguration(ListableBeanFactory beanFactory) {
        this.beanFactory = beanFactory;
    }

    /**
     * Configures the TaskExecutor to be used for async requests (i.e. Bridge
     * Read).
     */
    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        AsyncTaskExecutor taskExecutor = (AsyncTaskExecutor) this.beanFactory
                .getBean(PXF_RESPONSE_STREAM_TASK_EXECUTOR);
        configurer.setTaskExecutor(taskExecutor);
    }

    /**
     * Configures and builds the {@link ThreadPoolTaskExecutor}
     *
     * @return the {@link ThreadPoolTaskExecutor}
     */
    @Bean(name = {PXF_RESPONSE_STREAM_TASK_EXECUTOR,
            AsyncAnnotationBeanPostProcessor.DEFAULT_TASK_EXECUTOR_BEAN_NAME})
    public ThreadPoolTaskExecutor pxfApplicationTaskExecutor(PxfServerProperties pxfServerProperties,
                                                             ObjectProvider<TaskExecutorCustomizer> taskExecutorCustomizers,
                                                             ObjectProvider<TaskDecorator> taskDecorator) {

        TaskExecutionProperties properties = pxfServerProperties.getTask();
        TaskExecutionProperties.Pool pool = properties.getPool();
        TaskExecutorBuilder builder = new TaskExecutorBuilder();
        builder = builder.queueCapacity(pool.getQueueCapacity());
        builder = builder.corePoolSize(pool.getCoreSize());
        builder = builder.maxPoolSize(pool.getMaxSize());
        builder = builder.allowCoreThreadTimeOut(pool.isAllowCoreThreadTimeout());
        builder = builder.keepAlive(pool.getKeepAlive());
        TaskExecutionProperties.Shutdown shutdown = properties.getShutdown();
        builder = builder.awaitTermination(shutdown.isAwaitTermination());
        builder = builder.awaitTerminationPeriod(shutdown.getAwaitTerminationPeriod());
        builder = builder.threadNamePrefix(properties.getThreadNamePrefix());
        builder = builder.customizers(taskExecutorCustomizers.orderedStream()::iterator);
        builder = builder.taskDecorator(taskDecorator.getIfUnique());

        LOG.debug("Initializing PXF ThreadPoolTaskExecutor with prefix={}. " +
                        "Pool options: " +
                        "queue capacity={}, core size={}, max size={}, " +
                        "allow core thread timeout={}, keep alive={}. " +
                        "Shutdown options: await termination={}, await " +
                        "termination period={}.",
                properties.getThreadNamePrefix(),
                pool.getQueueCapacity(),
                pool.getCoreSize(),
                pool.getMaxSize(),
                pool.isAllowCoreThreadTimeout(),
                pool.getKeepAlive(),
                shutdown.isAwaitTermination(),
                shutdown.getAwaitTerminationPeriod());

        return builder.build(PxfThreadPoolTaskExecutor.class);
    }

    /**
     * Custom {@link WebMvcTagsContributor} that adds PXF specific tags to metrics for Spring MVC (REST endpoints)
     *
     * @return the {@link WebMvcTagsContributor} instance
     */
    @Bean
    public WebMvcTagsContributor webMvcTagsContributor() {
        return new WebMvcTagsContributor() {
            @Override
            public Iterable<Tag> getTags(HttpServletRequest request, HttpServletResponse response, Object handler, Throwable exception) {
                Tags tags = Tags.empty();
                // add tags only if the request is for PXF service endpoints (not actuator ones)
                String user = request.getHeader("X-GP-USER");
                if (StringUtils.isNotBlank(user)) {
                    tags = tags.and("user", user);
                    tags = addTag("segmentID", request.getHeader("X-GP-SEGMENT-ID"), tags, null);
                    tags = addTag("profile", request.getHeader("X-GP-OPTIONS-PROFILE"), tags, null);
                    tags = addTag("server", request.getHeader("X-GP-OPTIONS-SERVER"), tags, "default");
                }
                return tags;
            }

            @Override
            public Iterable<Tag> getLongRequestTags(HttpServletRequest request, Object handler) {
                return new ArrayList<>();
            }

            private Tags addTag(String tag, String value, Tags tags, String defaultValue) {
                value = StringUtils.defaultIfBlank(value, defaultValue);
                if (StringUtils.isNotBlank(value)) {
                    tags = tags.and(tag, value);
                }
                return tags;
            }
        };
    }

}
