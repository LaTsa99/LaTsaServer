package com.latsa.chatserver;

import com.latsa.chatserver.gui.ServerTerminal;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

public class TcpChatServer implements Runnable {
    private int PORT;
    private ServerSocket welcomeSocket;
    private boolean stopServer = false;

    private ArrayList<ClientHandler> clients;
    private ArrayList<UserData> users;
    private ArrayList<String> blacklist;
    private ArrayList<String> previousMessages;

    private ServerTerminal terminal;

    private static final String IPADDRESS_PATTERN =
            "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

    @SuppressWarnings("unchecked")
    public TcpChatServer(int port, ServerTerminal terminal) {
        this.PORT = port;
        this.terminal = terminal;
        initServer();

        users = new ArrayList<>();
        previousMessages = new ArrayList<>();
        blacklist = new ArrayList<>();

        users = (ArrayList<UserData>)initList("users.ser");
        previousMessages = (ArrayList<String>)initList("prev.ser");
        blacklist = (ArrayList<String>)initList("blacklist.ser");

        clients = new ArrayList<>();
    }

    private ArrayList<?> initList(String location) {
        ArrayList<?> temp = new ArrayList<>();
        try {
            FileInputStream fis = new FileInputStream(location);
            ObjectInputStream ois = new ObjectInputStream(fis);
            temp = (ArrayList<?>) ois.readObject();
            ois.close();
            fis.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }

    private void saveList(String location, ArrayList<?> list) {
        try {
            FileOutputStream fos = new FileOutputStream(location);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(list);
            oos.close();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initServer() {
        try {
            welcomeSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        Socket clientSocket;
        ClientHandler newClient;
        while (!stopServer) {
            try {
                clientSocket = welcomeSocket.accept();
                boolean accepted = true;
                String address = clientSocket.getInetAddress().getHostAddress();

                if(blacklist != null) {
                    for (String s : blacklist) {
                        if (s.equals(address)) {
                            refused(clientSocket);
                            clientSocket.close();
                            accepted = false;
                        }
                    }
                }

                if (accepted) {
                    newClient = new ClientHandler(clientSocket, terminal, users, clients, previousMessages);
                    clients.add(newClient);
                    newClient.start();
                    terminal.appendTextToTerminal(String.format("Connection from %s accepted!", address));
                } else {
                    terminal.appendTextToTerminal(String.format("Connection from %s refused!", address));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private void refused(Socket client) {
        try {
            DataOutputStream dos = new DataOutputStream(client.getOutputStream());
            dos.writeUTF("REFUSED");
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stopServer() {
        stopServer = true;
        saveList("users.ser", users);
        saveList("prev.ser", previousMessages);
        saveList("blacklist.ser", blacklist);
        try {
            welcomeSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int getPORT() {
        return welcomeSocket.getLocalPort();
    }

    public UserData getUser(String username) {
        for (UserData ud : users)
            if (ud.getUsername().equals(username))
                return ud;

        return null;
    }

    public void showUsers() {
        TreeMap<String, String> online = ClientHandler.showOnline();
        terminal.appendTextToTerminal("\n\nusers\tonline");
        terminal.appendTextToTerminal("------------------------------------------------------------------------------");
        if (online != null) {
            Set<String> names = online.keySet();
            for (String name : names)
                terminal.appendTextToTerminal(String.format("%s\t%s", name, online.get(name)));
        } else {
            for (UserData ud : users) {
                terminal.appendTextToTerminal(String.format("%s\t%s", ud.getUsername(), "Offline"));
            }
        }

    }

    public void deleteHistory() {
        previousMessages.clear();
        saveList("prev.ser", previousMessages);
    }


    public void kickUser(String user) {
        ClientHandler selected = null;
        for (ClientHandler ch : clients) {
            if (ch.getUsername().equals(user)) {
                selected = ch;
            }
        }
        if(selected != null){
            selected.sendMessage("kick#REKT");
            selected.disconnect();
            selected.announce(String.format("%s has been kicked from the server by server admin!", user));
            terminal.appendTextToTerminal(String.format("%s has been kicked.", user));
        } else
            terminal.appendTextToTerminal("Error: selected user is offline!");
    }

    public void banUser(String user, String reason) {
        UserData target = null;
        for (UserData ud : users) {
            if (ud.getUsername().equals(user)) {
                target = ud;
            }
        }

        if (target == null) {
            terminal.appendTextToTerminal("Error: No such user!");
            return;
        }
        if (target.getBanned())
            terminal.appendTextToTerminal("Error: User is already banned!");
        else {
            target.setBanned(true);
            ClientHandler selected = null;
            for(ClientHandler ch : clients)
            {
                if(ch.getUsername().equals(user))
                    selected = ch;
            }
            if(selected != null)
                selected.sendMessage(String.format("ban#%s", reason));
            terminal.appendTextToTerminal(String.format("%s banned successfully!", user));
        }

        for (ClientHandler ch : clients) {
            if (ch.getUsername().equals(user)) {
                ch.sendMessage(String.format("ban#%s", reason));
                ch.disconnect();
                ch.announce(String.format("%s has been banned by server admin!", user));
            }
        }
    }

    public void removeBan(String user) {
        UserData target = null;
        for (UserData ud : users) {
            if (ud.getUsername().equals(user))
                target = ud;
        }

        if (target == null)
            terminal.appendTextToTerminal("Error: No such user!");
        else {
            if (target.getBanned()) {
                target.setBanned(false);
                terminal.appendTextToTerminal("Ban removed from user.");
            } else
                terminal.appendTextToTerminal("Error: User is not banned.");
        }
    }

    public void banIp(String ip) {
        if (ip.matches(IPADDRESS_PATTERN)) {
            if (blacklist.contains(ip))
                terminal.appendTextToTerminal("Error: IP address already banned.");
            else {
                blacklist.add(ip);
                terminal.appendTextToTerminal("IP address added to blacklist!");
            }
        } else
            terminal.appendTextToTerminal("Error: This is not an IP address!");
    }

    public void unbanIp(String ip) {
        if (ip.matches(IPADDRESS_PATTERN)) {
            if (blacklist.contains(ip)) {
                blacklist.remove(ip);
                terminal.appendTextToTerminal("IP removed from blacklist!");
            } else {
                terminal.appendTextToTerminal("Error: IP address is not on blacklist!");
            }
        } else
            terminal.appendTextToTerminal("Error: This is not an IP address!");
    }

    public void deleteUser(String user) {
        UserData toDelete = null;
        for(UserData ud : users)
        {
            if(ud.getUsername().equals(user))
                toDelete = ud;
        }

        if (toDelete == null)
            terminal.appendTextToTerminal("Error: User doesn't exist!");
        else {
            users.remove(toDelete);
            if(ClientHandler.showOnline() != null){
                ClientHandler.showOnline().remove(user);
            }
            terminal.appendTextToTerminal(String.format("%s removed from users!", user));
        }
    }


    //FOR TESTING

    public void addUser(String name, String pass) {
        boolean exists = false;
        for (UserData u : users) {
            if (u.getUsername().equals(name))
                exists = true;
        }

        if (!exists)
            users.add(new UserData(name, pass));

    }

    public ArrayList<String> getPreviousMessages() {
        return previousMessages;
    }

    public ArrayList<String> getBlacklist() {
        return blacklist;
    }
}
