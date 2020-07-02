package com.flexicore.init;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.support.MapJobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.core.scope.JobScope;
import org.springframework.batch.core.scope.StepScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class SpringBatchConfiguration {

    @Bean
    public TaskExecutor threadPoolTaskExecutor(){

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setMaxPoolSize(12);
        executor.setCorePoolSize(8);
        executor.setQueueCapacity(15);
        executor.setThreadNamePrefix("AsyncThread");

        return executor;
    }


    @Bean
    public JobRepository jobRepository( MapJobRepositoryFactoryBean mapJobRepositoryFactoryBean) throws Exception {
        JobRepository mapJobRepository = mapJobRepositoryFactoryBean.getObject();
        return mapJobRepository;
    }

    @Bean
    public MapJobRepositoryFactoryBean mapJobRepositoryFactoryBean(PlatformTransactionManager resourcelessTransactionManager) {
        return new MapJobRepositoryFactoryBean(resourcelessTransactionManager);
    }
    @Bean
    public JobScope jobScope() {
        return new JobScope();
    }
    @Bean
    public StepScope stepScope() {
        return new StepScope();
    }

    @Bean
    public JobLauncher jobLauncher(JobRepository jobRepository) throws Exception {
        SimpleJobLauncher simpleJobLauncher = new SimpleJobLauncher();
        simpleJobLauncher.setJobRepository(jobRepository);
        simpleJobLauncher.setTaskExecutor(threadPoolTaskExecutor());

        simpleJobLauncher.afterPropertiesSet();
        return simpleJobLauncher;
    }


    @Bean
    public JobRegistry jobRegistry() throws Exception {
        return new MapJobRegistry();
    }

    @Bean
    public JobBuilderFactory jobBuilderFactory(JobRepository jobRepository){
        return new JobBuilderFactory(jobRepository);
    }

    @Bean
    public StepBuilderFactory stepBuilderFactory(JobRepository jobRepository,PlatformTransactionManager resourcelessTransactionManager){
        return new StepBuilderFactory(jobRepository,resourcelessTransactionManager);
    }
    @Bean
    public JobExplorer jobExplorer(MapJobRepositoryFactoryBean mapJobRepositoryFactoryBean) throws Exception {
        return new MapJobExplorerFactoryBean(mapJobRepositoryFactoryBean).getObject();

    }

    @Bean
    public BatchConfigurer batchConfigurer( JobRepository jobRepository,
                                           JobLauncher jobLauncher,
                                            PlatformTransactionManager resourcelessTransactionManager,
                                           JobExplorer jobExplorer){

        return new BatchConfigurer() {
            @Override
            public JobRepository getJobRepository() throws Exception {
                return jobRepository;
            }

            @Override
            public PlatformTransactionManager getTransactionManager() throws Exception {
                return resourcelessTransactionManager;
            }

            @Override
            public JobLauncher getJobLauncher() throws Exception {
                return jobLauncher;
            }

            @Override
            public JobExplorer getJobExplorer() throws Exception {
                return jobExplorer;
            }
        };
    }

}