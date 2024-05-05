package com.wizzdi.flexicore.init.config;

import com.wizzdi.flexicore.boot.base.interfaces.Plugin;
import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.buf.EncodedSolidusHandling;
import org.pf4j.Extension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.firewall.HttpFirewall;
import org.springframework.security.web.firewall.StrictHttpFirewall;

@Configuration
@Extension
public class FirewallConfig implements Plugin {

    @Value("${flexicore.allow.encoded.params:false}")
    private boolean allowEncodedParams;

    @Bean
    public HttpFirewall allowEncodedParamsFirewall() {
        StrictHttpFirewall firewall = new StrictHttpFirewall();
        if(allowEncodedParams){
            firewall.setAllowUrlEncodedPercent(true);
            firewall.setAllowUrlEncodedSlash(true);
            firewall.setAllowUrlEncodedDoubleSlash(true);
            firewall.setAllowBackSlash(true);
            firewall.setAllowUrlEncodedCarriageReturn(true);
            firewall.setAllowUrlEncodedLineFeed(true);
            firewall.setAllowUrlEncodedPeriod(true);
            firewall.setAllowUrlEncodedParagraphSeparator(true);
            firewall.setAllowUrlEncodedLineSeparator(true);
        }

        return firewall;
    }

}
