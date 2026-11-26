package com.deepsource.horas.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Reloj inyectable: en las pruebas se reemplaza por uno fijo
@Configuration
public class RelojConfig {
    @Bean
    public Clock reloj() {
        return Clock.system(ZoneId.of("America/Santiago"));
    }
}
