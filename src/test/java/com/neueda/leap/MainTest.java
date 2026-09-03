package com.neueda.leap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import com.neueda.leap.Main;

public class MainTest {

    @Test
    public void testMain() {
        Main.main(new String[] {});
    }

    @Test
    public void testGreeting() {
        assertEquals("Hello world from the Fintech Five's Sprint 1 project skeleton", Main.greeting());
    }

}