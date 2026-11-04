package com.example.jolijob;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.EnabledIf;

@Import(TestcontainersConfiguration.class)
@EnabledIf(expression = "#{T(com.example.jolijob.support.TestDockerSupport).isDockerAvailable()}", loadContext = false)
@SpringBootTest(properties = "spring.docker.compose.enabled=false")
class JoliJobApplicationDockerIT {

    @Test
    void contextLoadsWithTestcontainers() {
    }

}


