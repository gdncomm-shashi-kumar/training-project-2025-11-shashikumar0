package com.blibli.gdn.gateway;


import com.blibli.gdn.gateway.config.CorsConfig;
import com.blibli.gdn.gateway.config.JwtConfig;
import com.blibli.gdn.gateway.config.PublicEndpointsConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtConfig.class, CorsConfig.class, PublicEndpointsConfig.class})
public class GatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(GatewayApplication.class, args);
	}

}
