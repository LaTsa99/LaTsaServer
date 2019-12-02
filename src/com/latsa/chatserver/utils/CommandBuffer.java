package com.latsa.chatserver.utils;

import java.util.ArrayList;
import java.util.List;

public class CommandBuffer {
    List<String> list;
    private int current;

    public CommandBuffer() {
        list = new ArrayList<>();
        current = 0;
    }

    public void put(String s) {
        list.add(s);
        current = -1;
    }

    public String getPrev() {
        if (current > 0) {
            current = current - 1;
            return list.get(current);

        } else if (current == -1) {
            current = list.size() - 1;
            return list.get(current);
        }
        return null;
    }

    public String getNext() {
        if (current < list.size() - 1) {
            current = current + 1;
            return list.get(current);
        }

        return null;

    }

}
