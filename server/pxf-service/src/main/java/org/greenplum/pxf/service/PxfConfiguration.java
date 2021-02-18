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

    @Bean
    public WebMvcTagsContributor webMvcTagsContributor() {
        return new PxfWebMvcTagsProvider();
    }

    public static class PxfWebMvcTagsProvider implements WebMvcTagsContributor {

        @Override
        public Iterable<Tag> getTags(HttpServletRequest request, HttpServletResponse response, Object handler, Throwable exception) {
            Tags tags = Tags.empty();
            tags = addTagFromHttpHeader("user", "X-GP-USER", tags, request);
            tags = addTagFromHttpHeader("segmentID", "X-GP-SEGMENT-ID", tags, request);
            tags = addTagFromHttpHeader("profile", "X-GP-OPTIONS-PROFILE", tags, request);
            tags = addTagFromHttpHeader("server", "X-GP-OPTIONS-SERVER", tags, request);
            return tags;
        }

        @Override
        public Iterable<Tag> getLongRequestTags(HttpServletRequest request, Object handler) {
            return new ArrayList<>();
        }

        private Tags addTagFromHttpHeader(String tag, String header, Tags tags, HttpServletRequest request) {
            String headerValue = request.getHeader(header);
            if (StringUtils.isNotBlank(headerValue)) {
                tags = tags.and(Tag.of(tag, headerValue));
            }
            return tags;
        }
    }

//    @Bean
//    public WebMvcTagsContributor webMvcTagsContributor() {
//        return new WebMvcTagsContributor() {
//
//            @Override
//            public Iterable<Tag> getTags(HttpServletRequest request, HttpServletResponse response, Object handler, Throwable exception) {
//                Tags tags = Tags.empty();
//                addTagFromHttpHeader("user","X-GP-USER", tags, request);
//                addTagFromHttpHeader("segmentID", "X-GP-SEGMENT-ID", tags, request);
//                addTagFromHttpHeader("profile", "X-GP-OPTIONS-PROFILE", tags, request);
//                addTagFromHttpHeader("server", "X-GP-OPTIONS-SERVER", tags, request);
//                return tags;
//            }
//
//            @Override
//            public Iterable<Tag> getLongRequestTags(HttpServletRequest request, Object handler) {
//                return null;
//            }
//
//            private void addTagFromHttpHeader(String tag, String header, Tags tags, HttpServletRequest request) {
//                String headerValue = request.getHeader(header);
//                if (StringUtils.isNotBlank(headerValue)) {
//                    tags.and(Tag.of(tag, headerValue));
//                }
//            }
//        };
//    }
}
