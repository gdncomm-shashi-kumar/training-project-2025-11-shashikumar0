package com.blibli.gdn.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.data.redis.host=localhost",
		"spring.data.redis.port=6379"
})
class GatewayApplicationTests {

	@Test
	void contextLoads() {
		// Context loads successfully
	}

}
