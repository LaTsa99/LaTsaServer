package com.latsa.chatserver.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandFifoTest {

    CommandFifo cf;

    @BeforeEach
    void setUp() {
        cf = new CommandFifo();
    }

    @Test
    void testCommandFifo() throws InterruptedException {
        final String testString = "Test";
        cf.put(testString);
        String result = cf.get();
        assertTrue(result.equals(testString));
    }
}