package com.latsa.chatserver.gui;


import com.latsa.chatserver.utils.CommandBuffer;
import com.latsa.chatserver.utils.CommandFifo;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 * A simple terminal like interface. Takes input and acts as a default output.
 */
public class ServerTerminal extends JFrame {
    private CommandFifo fifo;
    private JPanel mainPanel;
    private JTextArea terminalArea;
    private JTextField terminalInput;
    private final int COLUMNS = 50;

    private CommandBuffer buffer;
    private final String PREV = "previous";
    private final String NEXT = "next";
    private SimpleDateFormat format;

    private final String welcome = "Welcome to my chat server!\n"
            + "Usage:\n"
            + "start_server [port]              Starting server on port [port].\n"
            + "stop_server                      Stopping server.\n"
            + "add_admin [username]             Adds admin privileges to [username]\n"
            + "remove_admin [username]          Removes admin privileges from [username]\n"
            + "show_users                       Show all registered users and their status.\n"
            + "delete_history                   Deletes previous messages from storage.\n"
            + "kick_user [username]             Kicks [username] from server.\n"
            + "ban_user [username] [reason]     Bans [username] from the server for the following\n"
            + "                                 reason: [reason].\n"
            + "remove_ban [username]            Removes ban from [username].\n"
            + "ban_ip [ip address]              Adds [ip address] to blacklist.\n"
            + "unban_ip [ip address]            Removes [ip address] from blacklist.\n"
            + "delete_user [username]           Deletes data of [username] from server.\n"
            + "show_msg                         Toggles showing messages going through the server.\n"
            + "hide_msg                         Hides messages going through the server.\n\n"
            + "Important: In case of names with two or more parts use '_' instead of whitespace!\n"
            + "\n";

    private ArrayList<String> logs;

    /**
     * Constructs a new terminal.
     *
     * @param fifo command fifo where incoming commands can be placed
     */
    public ServerTerminal(CommandFifo fifo) {
        super("LaTsa chat server");
        this.fifo = fifo;
        this.buffer = new CommandBuffer();

        logs = new ArrayList<>();
        format = new SimpleDateFormat("yyyy.MM.dd hh:mm:ss a");

        initWindow();
        this.setVisible(true);
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setResizable(false);
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                sendCommand("stop_server");
                saveLogs();
            }
        });


    }

    /**
     * Initiates a swing window.
     */
    private void initWindow() {
        mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(Color.BLACK);
        JPanel textPanel = new JPanel();
        JPanel inputPanel = new JPanel();
        textPanel.setBackground(Color.BLACK);
        inputPanel.setBackground(Color.BLACK);

        terminalArea = new JTextArea();
        terminalArea.setColumns(COLUMNS);
        terminalArea.setRows(30);
        terminalArea.setEnabled(false);
        terminalArea.setDisabledTextColor(Color.GREEN);
        terminalArea.setBackground(Color.BLACK);
        terminalArea.setLineWrap(true);
        appendTextToTerminal(welcome);
        terminalArea.setMargin(new Insets(5, 3, 5, 5));
        JScrollPane areaScroller = new JScrollPane(terminalArea);
        areaScroller.createVerticalScrollBar();
        areaScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        textPanel.add(areaScroller);
        mainPanel.add(textPanel, BorderLayout.NORTH);

        terminalInput = new JTextField();
        terminalInput.setColumns(COLUMNS);
        terminalInput.addActionListener(actionEvent -> {
            sendCommand(terminalInput.getText());
            terminalInput.setText("");
        });
        terminalInput.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("UP"), PREV);
        terminalInput.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DOWN"), NEXT);
        terminalInput.getActionMap().put(PREV, new PrevAction(1));
        terminalInput.getActionMap().put(NEXT, new PrevAction(2));
        inputPanel.add(terminalInput);
        mainPanel.add(inputPanel, BorderLayout.SOUTH);


        this.add(mainPanel);
        this.pack();
    }

    /**
     * Shows text on the terminal.
     *
     * @param s text to display
     */
    public synchronized void appendTextToTerminal(String s) {
        terminalArea.append(s + "\n");
        terminalArea.setCaretPosition(terminalArea.getDocument().getLength());
        logs.add(String.format("%s :\t %s\n", format.format(new Date()), s));
    }

    /**
     * Places command in the fifo.
     *
     * @param cmd command to place in fifo
     */
    private void sendCommand(String cmd) {
        buffer.put(cmd);
        appendTextToTerminal("YOU >> " + cmd);
        fifo.put(cmd);
    }

    /**
     * @return previously given command
     */
    private String previousCommand() {
        String prev = buffer.getPrev();
        return prev;
    }

    /**
     * @return command after previous command
     */
    private String nextCommand() {
        String next = buffer.getNext();
        return next;
    }

    /**
     * Listens for up-arrow and down-arrow keys. If up-arrow pressed,
     * then it returns the command before the command in input, by
     * down-arrow it's the command after the one in the input.
     */
    private class PrevAction extends AbstractAction {
        int to;

        PrevAction(int to) {
            this.to = to;
        }


        /**
         * Executes commands if up-arrow or down-arrow pressed.
         *
         * @param actionEvent a key has been pressed
         */
        @Override
        public void actionPerformed(ActionEvent actionEvent) {
            if (to == 1) {
                String c = previousCommand();
                if (c != null)
                    terminalInput.setText(c);
            } else if (to == 2) {
                String c = nextCommand();
                if (c != null)
                    terminalInput.setText(c);
            }
        }
    }

    /**
     * Saves terminal output in a txt file.
     */
    private void saveLogs() {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter("log.txt", true));
            for (String s : logs)
                bw.write(s);

            bw.write("****************************************************************");
            bw.write("\n\n");

            bw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
