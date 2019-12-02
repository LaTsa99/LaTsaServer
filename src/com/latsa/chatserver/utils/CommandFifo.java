package com.latsa.chatserver.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CommandFifo {
    private List<String> list;

    public CommandFifo() {
        list = Collections.synchronizedList(new ArrayList<>());
    }

    public synchronized void put(String elem) {
        list.add(elem);
        this.notifyAll();
    }

    public synchronized String get() throws InterruptedException {
        while (list.size() <= 0)
            this.wait();
        String temp = list.get(0);
        list.remove(0);
        this.notifyAll();
        return temp;
    }
}
