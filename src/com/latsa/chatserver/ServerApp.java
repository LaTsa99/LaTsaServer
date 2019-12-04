package com.latsa.chatserver;

import com.latsa.chatserver.gui.ServerTerminal;
import com.latsa.chatserver.utils.CommandFifo;

/**
 * Main class of the application.
 */
public class ServerApp {

    /**
     * Entry point of the program. Creates a Fifo object,
     * a terminal and a command handler.
     *
     * @param args cmd args
     */
    public static void main(String[] args)
    {
        CommandFifo fifo = new CommandFifo();

        ServerTerminal terminal = new ServerTerminal(fifo);
        Thread commandHandler = new Thread(new CommandHandler(fifo, terminal));
        commandHandler.start();
    }
}
