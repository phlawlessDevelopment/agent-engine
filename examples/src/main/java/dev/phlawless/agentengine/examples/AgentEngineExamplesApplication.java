package dev.phlawless.agentengine.examples;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "dev.phlawless.agentengine")
public class AgentEngineExamplesApplication {

	public static void main(String[] args) {
		SpringApplication.run(AgentEngineExamplesApplication.class, args);
	}

}
