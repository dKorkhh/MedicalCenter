package org.example.billing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.data.mongodb.auto-index-creation=false",
		"spring.kafka.listener.auto-startup=false"
})
class BillingApplicationTests {

	@Test
	void contextLoads() {
	}

}
