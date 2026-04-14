package com.stufamily.backend.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest(classes = com.stufamily.backend.boot.StuFamilyBackendApplication.class)
public class SimpleConfigTest {

    @Test
    void contextLoads() {
        assertNotNull("test");
    }
}
