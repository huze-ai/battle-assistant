package com.example.battle_assistant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@org.springframework.test.context.ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "BATTLE_TEST_MYSQL", matches = "1")
@SpringBootTest
class BattleAssistantApplicationTests {

	@Test
	void contextLoads() {
	}

}
