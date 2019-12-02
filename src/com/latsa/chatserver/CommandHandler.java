package com.latsa.chatserver;

import com.latsa.chatserver.gui.ServerTerminal;
import com.latsa.chatserver.utils.CommandFifo;


public class CommandHandler implements Runnable {
    private CommandFifo fifo;
    private ServerTerminal terminal;
    private TcpChatServer server;
    private boolean serverRunning;

    public CommandHandler(CommandFifo fifo, ServerTerminal terminal) {
        this.fifo = fifo;
        this.terminal = terminal;
        serverRunning = false;
    }

    @Override
    public void run() {
        while (true) {
            try {
                String command = fifo.get();

                String[] cmd = command.split(" ");
                if (cmd[0].equals("start_server") && cmd.length == 2)
                    startServer(cmd[1]);
                else if (cmd[0].equals("stop_server") && cmd.length == 1)
                    stopServer();
                else if (cmd[0].equals("add_admin") && cmd.length == 2)
                    addAdmin(cmd[1]);
                else if (cmd[0].equals(("remove_admin")) && cmd.length == 2)
                    removeAdmin(cmd[1]);
                else if (cmd[0].equals("show_users") && cmd.length == 1)
                    showUsers();
                else if (cmd[0].equals("delete_history") && cmd.length == 1)
                    deleteHistory();
                else if (cmd[0].equals("kick_user") && cmd.length == 2)
                    kickUser(cmd[1]);
                else if (cmd[0].equals("ban_user") && cmd.length > 3)
                    banUser(cmd);
                else if (cmd[0].equals("remove_ban") && cmd.length == 2)
                    removeBan(cmd[1]);
                else if (cmd[0].equals("ban_ip") && cmd.length == 2)
                    banIp(cmd[1]);
                else if (cmd[0].equals("unban_ip") && cmd.length == 2)
                    unbanIp(cmd[1]);
                else if (cmd[0].equals("delete_user") && cmd.length == 2)
                    deleteUser(cmd[1]);
                else if (cmd[0].equals("show_msg") && cmd.length == 1)
                    showMsg(true);
                else if (cmd[0].equals("hide_msg") && cmd.length == 1)
                    showMsg(false);
                else
                    terminal.appendTextToTerminal("Error: Not a valid command!");

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void startServer(String port) {
        if (!serverRunning) {
            if(Integer.parseInt(port) > 1023) {
                try {
                    int PORT = Integer.parseInt(port);
                    server = new TcpChatServer(PORT, terminal);
                    Thread serverThread = new Thread(server);
                    serverThread.start();
                    terminal.appendTextToTerminal("Server started on port " + port + ".");
                    serverRunning = true;
                } catch (Exception e) {
                    terminal.appendTextToTerminal("Error: Port must be a number between 1024 and 65535!");
                }
            }else{
                terminal.appendTextToTerminal("Error: Port number cannot be a well known port!");
            }
        } else {
            terminal.appendTextToTerminal("Server already running on this PC!");
        }

    }

    private void stopServer() {
        if (server != null) {
            server.stopServer();
            terminal.appendTextToTerminal("Server stopped on port " + server.getPORT());
            serverRunning = false;
        }else
            noServer();
    }

    private void addAdmin(String s) {
        if(server != null) {
            UserData selected = server.getUser(s);
            if (selected == null)
                terminal.appendTextToTerminal("Error: No such user!");
            else {
                if (selected.getIsAdmin())
                    terminal.appendTextToTerminal("Error: User is already admin!");
                else {
                    selected.setAdmin(true);
                    terminal.appendTextToTerminal(String.format("%s is now an admin!", s));
                }
            }
        }else
            noServer();
    }

    private void removeAdmin(String s) {
        if(server != null) {
            UserData selected = server.getUser(s);
            if (selected == null)

                terminal.appendTextToTerminal("Error: No such user!");
            else {
                if (selected.getIsAdmin()) {
                    selected.setAdmin(false);
                    terminal.appendTextToTerminal(String.format("%s is now not an admin!", s));
                } else {
                    terminal.appendTextToTerminal("Error: user is not an admin!");
                }
            }
        }else
            noServer();
    }

    private void showUsers() {
        if (server != null) {
            server.showUsers();
        } else {
            noServer();
        }
    }

    private void deleteHistory() {
        if(server != null) {
            if (areYouSure()) {
                server.deleteHistory();
                terminal.appendTextToTerminal("History deleted successfully!");
            }
        }else
            noServer();
    }

    private void kickUser(String user) {
        if(server != null) {
            if (areYouSure()) {
                if (user.contains("_"))
                    user = user.replace("_", " ");
                server.kickUser(user);
            }
        }else
            noServer();
    }

    private void banUser(String[] cmd) {
        if(server != null) {
            if (areYouSure()) {
                String user = cmd[1];
                if (user.contains("_"))
                    user = user.replace("_", " ");

                cmd[0] = "";
                cmd[1] = "";
                StringBuilder reasonB = new StringBuilder();
                for (String s : cmd)
                    reasonB.append(s + " ");

                String reason = reasonB.toString();
                server.banUser(user, reason);
            }
        }else
            noServer();

    }

    private void removeBan(String user) {
        if(server != null) {
            if (areYouSure()) {
                if (user.contains("_"))
                    user = user.replace("_", " ");

                server.removeBan(user);
            }
        }else
            noServer();
    }

    private void deleteUser(String user)
    {
        if(server != null) {
            if (user.contains("_"))
                user.replaceAll("_", " ");

            server.deleteUser(user);
        }else
            noServer();
    }

    private void banIp(String ip)
    {
        if(server != null)
        {
            server.banIp(ip);
        }else
            noServer();
    }

    private  void unbanIp(String ip)
    {
        if(server != null)
        {
            server.unbanIp(ip);
        }else
            noServer();
    }

    private boolean areYouSure() {
        terminal.appendTextToTerminal("Are you sure? (y/n)");
        try {
            String answer = fifo.get().toUpperCase();
            if (answer.equals("Y")) return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }


    private void noServer()
    {
        terminal.appendTextToTerminal("Error: No server running!");
    }

    private void showMsg(boolean show)
    {
        ClientHandler.showMsg(show);
    }
}
