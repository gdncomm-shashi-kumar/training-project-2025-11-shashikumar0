package com.blibli.gdn.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "gateway")
@Data
public class PublicEndpointsConfig {

    private List<String> publicEndpoints = new ArrayList<>();

    private List<String> optionalAuthEndpoints = new ArrayList<>();
}
