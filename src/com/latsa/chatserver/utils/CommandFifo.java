package com.latsa.chatserver.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A fifo that accept commands and holds them, until
 * they get taken.
 */
public class CommandFifo {
    private List<String> list;

    /**
     * Constructs a new fifo.
     */
    public CommandFifo() {
        list = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Puts a new element in the fifo and notifies other
     * threads, that they can take it out now.
     *
     * @param elem element to put in fifo
     */
    public synchronized void put(String elem) {
        list.add(elem);
        this.notifyAll();
    }

    /**
     * If there is an element in the fifo, threads can
     * synchronously take it out.
     *
     * @return first element of the fifo
     * @throws InterruptedException
     */
    public synchronized String get() throws InterruptedException {
        while (list.size() <= 0)
            this.wait();
        String temp = list.get(0);
        list.remove(0);
        this.notifyAll();
        return temp;
    }
}
