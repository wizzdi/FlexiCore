package com.flexicore.init;

import com.flexicore.model.Job;
import com.flexicore.runningentities.JobProcessor;
import com.flexicore.service.impl.JobService;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class GenericjobProvider {


    @Autowired
    private JobBuilderFactory jobBuilderFactory;
    @Autowired
    private StepBuilderFactory steps;

    @Bean
    protected Step step1(ItemReader<Job> reader,
                         ItemProcessor<Job, Job> processor,
                         ItemWriter<Job> writer) {
        return steps.get("step1").
                <com.flexicore.model.Job, com.flexicore.model.Job>chunk(10)
                .reader(reader).processor(processor).writer(writer).build();
    }

    @Bean(name = "genericJob")
    public org.springframework.batch.core.Job job(@Qualifier("step1") Step step1) {
        return jobBuilderFactory.get("genericJob")
                .start(step1)
                .build();
    }

    @Bean
    @Primary
    public ItemWriter<com.flexicore.model.Job> itemWriter() {
        return job -> {
        };
    }


    @Bean
    @Primary
    @JobScope
    public ItemReader<com.flexicore.model.Job> itemReader(@Value("#{jobParameters['jobId']}") String jobId) {
        return () -> jobId!=null?JobService.readJob(jobId):null;

    }

    @Bean
    @Primary
    public ItemProcessor<com.flexicore.model.Job, com.flexicore.model.Job> itemProcessor() {
        return new JobProcessor();
    }


}
