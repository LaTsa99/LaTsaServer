package com.latsa.chatserver;

import com.latsa.chatserver.gui.ServerTerminal;
import com.latsa.chatserver.utils.CommandFifo;

public class ServerApp {

    public static void main(String[] args)
    {
        CommandFifo fifo = new CommandFifo();

        ServerTerminal terminal = new ServerTerminal(fifo);
        Thread commandHandler = new Thread(new CommandHandler(fifo, terminal));
        commandHandler.start();
    }
}
