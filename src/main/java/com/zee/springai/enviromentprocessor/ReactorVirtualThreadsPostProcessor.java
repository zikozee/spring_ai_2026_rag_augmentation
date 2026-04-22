package com.zee.springai.enviromentprocessor;


import org.jspecify.annotations.NonNull;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * @dev : Ezekiel Eromosei
 * @date : 21 Apr, 2026
 */

public class ReactorVirtualThreadsPostProcessor implements EnvironmentPostProcessor {
    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, @NonNull SpringApplication application) {

        boolean enabled = environment.getProperty("bound-elastic-on-vt.enabled", Boolean.class, Boolean.TRUE);

        if(enabled){
            System.setProperty("reactor.schedulers.defaultBoundedElasticOnVirtualThreads",  "true");
        }

    }
}
