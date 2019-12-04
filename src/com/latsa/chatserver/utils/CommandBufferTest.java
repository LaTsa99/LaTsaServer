package com.latsa.chatserver.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests command buffer class.
 */
class CommandBufferTest {

    CommandBuffer cb;

    /**
     * Creates an instance of CommandBuffer before testing.
     */
    @BeforeEach
    void setUp() {
        cb = new CommandBuffer();
    }

    /**
     * Actually testing CommandBuffer. Uses 3 of its methods.
     */
    @Test
    void testBuffer() {
        final String test1 = "Test1";
        final String test2 = "Test2";
        final String test3 = "Test3";

        cb.put(test1);
        assertEquals(cb.getPrev(), test1);

        cb.put(test2);
        cb.put(test3);

        assertEquals(test3, cb.getPrev());
        assertEquals(test2, cb.getPrev());
        assertEquals(test1, cb.getPrev());

        assertEquals(test2, cb.getNext());
        assertEquals(test3, cb.getNext());
        assertEquals(test2, cb.getPrev());
        assertEquals(test3, cb.getNext());
    }
}