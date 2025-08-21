package com.example.demo;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HelloControllerTest {
    @Test
    void hello_returns_expected_message(){
        HelloController c = new HelloController();
        assertEquals("Hello from CI/CD!", c.hello());
    }
}
