package com.latsa.chatserver.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains previous server-side commands.
 */
public class CommandBuffer {
    List<String> list;
    private int current;

    /**
     * Constructs a new buffer.
     */
    public CommandBuffer() {
        list = new ArrayList<>();
        current = 0;
    }

    /**
     * Adds new element to the buffer.
     *
     * @param s Command string
     */
    public void put(String s) {
        list.add(s);
        current = -1;
    }

    /**
     * Returns the command, that was sent before command
     * previously shown.
     *
     * @return command string
     */
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

    /**
     * Returns the command, that was sent after the command
     * previously shown.
     *
     * @return command string
     */
    public String getNext() {
        if (current < list.size() - 1) {
            current = current + 1;
            return list.get(current);
        }

        return null;

    }

}
