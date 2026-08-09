package com.car.rental.config;

import com.car.rental.service.FingerprintService;
import com.car.rental.service.MockFingerprintService;
import com.car.rental.service.ZkFingerprintService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class FingerprintConfig {

    /**
     * Single shared FingerprintService for the whole app.
     * Host/port come from application.yml — same machine LAN today, server LAN later.
     */
    @Bean
    @Primary
    public FingerprintService fingerprintService(FingerprintProperties props) {
        if (props.isMock()) {
            return new MockFingerprintService();
        }
        return new ZkFingerprintService(props.getHost(), props.getPort(), props.getConnectTimeoutMs());
    }
}
