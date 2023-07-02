package com.kgromov.core;

import com.kgromov.mappers.MapperScanner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

import java.util.List;

@Configuration
@EnableAspectJAutoProxy
public class ProjectionConfig {

    @Bean
    JsonViewScanner jsonViewScanner() {
        return new JsonViewScanner();
    }

    @Bean
    JsonViewMethodProcessor jsonViewMethodProcessor(JsonViewScanner jsonViewScanner, List<MapperScanner> mapperScanners) {
        return new JsonViewMethodProcessor(jsonViewScanner, mapperScanners);
    }
}
