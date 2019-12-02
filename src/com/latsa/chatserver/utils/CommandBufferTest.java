package com.latsa.chatserver.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
//import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandBufferTest {

    CommandBuffer cb;

    @BeforeEach
    void setUp() {
        cb = new CommandBuffer();
    }

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