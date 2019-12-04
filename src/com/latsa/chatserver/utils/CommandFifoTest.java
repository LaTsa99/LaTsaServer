package com.latsa.chatserver.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests CommandFifo.
 */
class CommandFifoTest {

    CommandFifo cf;

    /**
     * Instantiates a new CommandFifo before testing.
     */
    @BeforeEach
    void setUp() {
        cf = new CommandFifo();
    }

    /**
     * Testing CommandFifo. Uses 2 of its methods.
     *
     * @throws InterruptedException
     */
    @Test
    void testCommandFifo() throws InterruptedException {
        final String testString = "Test";
        cf.put(testString);
        String result = cf.get();
        assertTrue(result.equals(testString));
    }
}