package com.flexicore.config;

import dev.samstevens.totp.code.HashingAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TotpConfig {

    @Value("${flexicore.security.totp.hasingAlgorithm:SHA1}")
    private String hasingAlgorithm;


    @Bean
    public HashingAlgorithm hashingAlgorithm() {
        return HashingAlgorithm.valueOf(hasingAlgorithm);
    }

}
