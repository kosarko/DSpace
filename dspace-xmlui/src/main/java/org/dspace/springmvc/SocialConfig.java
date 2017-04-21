package org.dspace.springmvc;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.social.google.api.Google;

/**
 * Created by ondra on 21.4.17.
 */
@Configuration
public class SocialConfig {

    @Bean
    @Scope(value="request", proxyMode = ScopedProxyMode.INTERFACES)
    public Google google(){
        return null;
    }
}
