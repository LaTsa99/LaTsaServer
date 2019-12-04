package com.latsa.chatserver;

import com.latsa.chatserver.gui.ServerTerminal;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

/**
 * Runs the server and executes some commands.
 */
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

    /**
     * Instantiates a new server on given port. Port can't be a well known port!
     *
     * @param port The port number that the server listens on
     * @param terminal The terminal class that the server sends output messages
     */
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

    /**
     * This loads serialized data from location, and returns it in an ArrayList.
     * If no data is stored, returns an empty ArrayList.
     *
     * @param location Where the serialized list is located
     * @return a generic ArrayList of previously saved data
     */
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

    /**
     * Saves the serialized list to the given location.
     *
     * @param location Where the file should be saved
     * @param list The ArrayList to save
     */
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

    /**
     * Creates the server socket on given port (from constructor)
     */
    private void initServer() {
        try {
            welcomeSocket = new ServerSocket(PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Accepts incoming sockets, checks, if they are on blacklist.
     * If not, then adds them to client handlers.
     */
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

    /**
     * Refuses connection to selected client.
     *
     * @param client socket, that should not connect to server
     */
    private void refused(Socket client) {
        try {
            DataOutputStream dos = new DataOutputStream(client.getOutputStream());
            dos.writeUTF("REFUSED");
            dos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Stops the server loop and saves data.
     */
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

    /**
     * Returns the port number of the server socket.
     *
     * @return port number of server
     */
    public int getPORT() {
        return welcomeSocket.getLocalPort();
    }

    /**
     * Returns the data of a selected user.
     * If data doesn't exist, returns null.
     *
     * @param username name of selected user
     * @return UserData object of user
     */
    public UserData getUser(String username) {
        for (UserData ud : users)
            if (ud.getUsername().equals(username))
                return ud;

        return null;
    }

    /**
     * Lists users of server and their status (online/offline).
     */
    public void showUsers() {
        TreeMap<String, String> online = ClientHandler.getOnline();
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


    /**
     * Deletes previous messages sent through the server.
     */
    public void deleteHistory() {
        previousMessages.clear();
        saveList("prev.ser", previousMessages);
    }


    /**
     * Kicks given user from the server.
     *
     * @param user name of user to kick.
     */
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

    /**
     * Bans the selected user from the server and sends her/him the reason
     * why she/he got banned.
     *
     * @param user name of user to ban
     * @param reason why you want to ban this user
     */
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

    /**
     * Removes ban from the selected user.
     *
     * @param user name of user to unban
     */
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

    /**
     * Adds ip address to blacklist.
     *
     * @param ip Ip address to ban
     */
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

    /**
     * Removes ip address from blacklist.
     *
     * @param ip Ip address to unban
     */
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

    /**
     * Deletes data of selected user.
     *
     * @param user name of user to delete
     */
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
            if(ClientHandler.getOnline() != null){
                ClientHandler.getOnline().remove(user);
            }
            terminal.appendTextToTerminal(String.format("%s removed from users!", user));
        }
    }


    //FOR TESTING

    /**
     * FOR TESTING ONLY
     * Adds new user to list of users of this server.
     *
     * @param name name of new user
     * @param pass password (preferably hash) of new user
     *
     */
    public void addUser(String name, String pass) {
        boolean exists = false;
        for (UserData u : users) {
            if (u.getUsername().equals(name))
                exists = true;
        }

        if (!exists)
            users.add(new UserData(name, pass));

    }

    /**
     * FOR TESTING ONLY
     * Returns messages, that went through the server.
     *
     * @return list of previous messages
     */
    public ArrayList<String> getPreviousMessages() {
        return previousMessages;
    }

    /**
     * FOR TESTING ONLY
     * Returns ip addresses that are banned from the server.
     *
     * @return list of banned ip addresses
     */
    public ArrayList<String> getBlacklist() {
        return blacklist;
    }
}
