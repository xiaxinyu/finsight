package com.finsight.config;

import com.finsight.common.security.SensitiveDataMasker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class StartUpListener implements ApplicationListener<ApplicationReadyEvent> {
    private static final Logger log = LoggerFactory.getLogger(StartUpListener.class);

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        printConfig(event);
    }

    private void printConfig(ApplicationReadyEvent event) {
        AccountProperties prop = event.getApplicationContext().getBean(AccountProperties.class);
        log.info("Account sign key: {}", SensitiveDataMasker.maskSecret(prop.getDesSignKey()));
    }
}
