package com.esprit.eureka;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class EurekaMainTest {

    @Test
    void main_invokesSpringApplicationRun() {
        try (MockedStatic<SpringApplication> mocked = Mockito.mockStatic(SpringApplication.class)) {
            EurekaApplication.main(new String[0]);
            mocked.verify(() -> SpringApplication.run(EurekaApplication.class, new String[0]));
        }
    }
}
