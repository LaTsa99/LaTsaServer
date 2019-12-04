package com.latsa.chatserver;


import com.latsa.chatserver.gui.ServerTerminal;
import org.springframework.security.crypto.bcrypt.BCrypt;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Class, that handles a connected client.
 */
public class ClientHandler extends Thread {

    private Socket clientSock;
    private DataInputStream dis;
    private DataOutputStream dos;

    private ServerTerminal terminal;
    private UserData thisUser;

    private ArrayList<UserData> users;
    private ArrayList<ClientHandler> clients;
    private ArrayList<String> previous;
    private static TreeMap<String, String> online;

    final static String OFFLINE = "Offline";
    final static String ONLINE = "Online";

    private boolean isLoggedIn;
    private boolean isConnected;
    private static boolean showMessages;


    /**
     * Constructs a new clienthandler.
     *
     * @param clientSock socket this handler handles
     * @param terminal default output terminal
     * @param users list of registered users
     * @param clients list of other clients on the server
     * @param previous previously sent messages on the server
     */
    public ClientHandler(Socket clientSock, ServerTerminal terminal, ArrayList<UserData> users, ArrayList<ClientHandler> clients,
                         ArrayList<String> previous) {
        this.clientSock = clientSock;
        try {
            this.dis = new DataInputStream(this.clientSock.getInputStream());
            this.dos = new DataOutputStream(this.clientSock.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        isLoggedIn = false;
        isConnected = true;
        this.terminal = terminal;
        if (this.users == null)
            this.users = users;

        if (this.clients == null)
            this.clients = clients;

        if (online == null) {
            online = new TreeMap<>();
            if(users != null) {
                for (UserData ud : users) {
                    online.put(ud.getUsername(), OFFLINE);
                }
            }
        }

        this.previous = previous;

        try {
            dos.writeUTF("ACCEPTED");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return if the user is successfully logged in
     */
    public boolean getLoggedIn() {
        return isLoggedIn;
    }

    /**
     * If user is logged in, waits for commands from the socket.
     */
    @Override
    public void run() {
        String temp;
        String[] cmd;
        while (isConnected) {
            if (clientSock.isClosed())
                isConnected = false;
            else {
                try {
                    temp = dis.readUTF();
                    cmd = temp.split("#");

                    if (cmd[0].equals("login") && cmd.length == 3) {
                        login(cmd[1], cmd[2]);
                    } else if (cmd[0].equals("register") && cmd.length == 3) {
                        register(cmd[1], cmd[2]);
                    } else if (cmd[0].equals("disconnect") && cmd.length == 1) {
                        disconnect();
                    } else if (cmd[0].equals("msg") && cmd.length > 1 && isLoggedIn) {
                        message(cmd);
                    } else if (cmd[0].equals("kick") && cmd.length == 2) {
                        kickUser(cmd[1]);
                    } else if (cmd[0].equals("ban")) {
                        banUser(cmd[1], cmd[2]);
                    } else {
                        sendError("Invalid command");
                    }
                } catch (IOException e) {
                    if (!isConnected)
                        e.printStackTrace();
                }
            }
        }
    }

    /**
     * If username exists, password matches and user isn't banned,
     * user will be logged in.
     *
     * @param username name of the user
     * @param hash hash of the password (in fact just plain text here)
     */
    private void login(String username, String hash) {
        if (users != null && users.size() != 0) {
            try {
                for (UserData ud : users) {
                    if (ud.getUsername().equals(username) && BCrypt.checkpw(hash, ud.getPassword())) {
                        thisUser = ud;
                        if (ud.getBanned()) {
                            sendMessage("ban#REKT");
                            disconnect();
                        }
                        isLoggedIn = true;
                    }
                }

                if (isLoggedIn) {
                    if (thisUser.getIsAdmin())
                        dos.writeUTF("OK_ADMIN");
                    else
                        dos.writeUTF("OK");
                    online.replace(username, ONLINE);
                    for (UserData d : users)
                        sendMessage(String.format("user#%s#%s", d.getUsername(), online.get(d.getUsername())));
                    terminal.appendTextToTerminal(username + " logged into the server!");
                    if (previous != null)
                        for (String s : previous)
                            sendMessage(s);
                    announce(String.format("user#%s#%s", thisUser.getUsername(), ONLINE));
                    announce(String.format("%s joined the server!", thisUser.getUsername()));
                } else {
                    sendError("Wrong credentials!");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            sendError("Wrong credentials!");
        }
    }

    /**
     * If username is not used, new user will be added to users list.
     *
     * @param username new username
     * @param hash new password
     */
    private void register(String username, String hash) {
        boolean exists = false;
        if(users != null)
        for (UserData ud : users) {
            if (ud.getUsername().equals(username))
                exists = true;
        }

        if (!exists) {
            UserData newUser = new UserData(username, BCrypt.hashpw(hash, BCrypt.gensalt()));
            users.add(newUser);
            online.put(username, OFFLINE);
            try {
                dos.writeUTF("OK");
                announce(String.format("user#%s#%s", username, OFFLINE));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            sendError("Username already exists!");
        }

    }


    /**
     * Disconnects user from the server.
     */
    void disconnect() {
        try {
            dos.writeUTF("disconnect");
            dos.close();
            dis.close();
            clientSock.close();
            String disc = String.format("%s disconnected from server.", thisUser.getUsername());
            clients.remove(this);
            if (isLoggedIn) {
                terminal.appendTextToTerminal(disc);
                isLoggedIn = false;
            } else
                terminal.appendTextToTerminal(disc);
            isConnected = false;
            online.replace(thisUser.getUsername(), OFFLINE);
            announce(String.format("user#%s#%s", thisUser.getUsername(), OFFLINE));
            announce(String.format("%s left the server!", thisUser.getUsername()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * Sends message to every online user on the server.
     *
     * @param s message to send
     */
    public void announce(String s) {
        if ((s.contains("#") && !s.substring(0, s.indexOf("#")).equals("user")) || !s.contains("#"))
            previous.add(s);
        for (ClientHandler ch : clients) {
            if (ch.getLoggedIn())
                ch.sendMessage(s);
        }
    }

    /**
     * Sends message from a user to every other user.
     *
     * @param msg message to send
     */
    private void message(String[] msg) {
        if (thisUser.getIsAdmin())
            msg[0] = thisUser.getUsername() + "(admin)# ";
        else
            msg[0] = thisUser.getUsername() + "# ";
        StringBuilder sb = new StringBuilder();
        for (String s : msg)
            sb.append(s);
        String message = sb.toString();
        if(showMessages){
            terminal.appendTextToTerminal(message);
        }
        announce(message);
    }

    /**
     * Sends an error message to the user.
     *
     * @param issue reason of error message
     */
    private void sendError(String issue) {
        try {
            dos.writeUTF(String.format("Error: %s", issue));
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends a message to the client.
     *
     * @param msg message to send
     */
    void sendMessage(String msg) {
        if (msg.contains("#")) {
            String user = msg.substring(0, msg.indexOf("#"));
            if (isLoggedIn && user.contains(thisUser.getUsername())) {
                msg = msg.replaceFirst(Pattern.quote(user), "Me");
                if (thisUser.getIsAdmin())
                    msg = msg.replaceFirst("#", "(admin)#  ");
                else
                    msg = msg.replaceFirst("#", "#  ");
            }

        }
        try {
            dos.writeUTF(msg);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return users and their online status
     */
    public static TreeMap<String, String> getOnline() {
        return online;
    }

    /**
     * Kick the selected user from the server. This is the function
     * to use when an admin client wants to kick another user.
     *
     * @param name user to kick
     */
    private void kickUser(String name) {
        terminal.appendTextToTerminal(String.format("%s want's to kick %s.", thisUser.getUsername(), name));

        UserData selected = null;
        for (UserData ud : users) {
            if (ud.getUsername().equals(name))
                selected = ud;
        }

        if (selected == null)
            terminal.appendTextToTerminal("No such user!");
        else if (selected.getUsername().equals(thisUser.getUsername())) {
            terminal.appendTextToTerminal("Error: user wanted to kick himself :D");
            try {
                dos.writeUTF("kick#YOU");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (selected.getIsAdmin()) {
            terminal.appendTextToTerminal("Error: user wanted to kick another admin!");
            try {
                dos.writeUTF("kick#ADMIN");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            terminal.appendTextToTerminal(String.format("Admin %s kicked user: %s", thisUser.getUsername(), name));

            sendMessage("kick#KICKED");
            ClientHandler toDelete = null;
            for (ClientHandler ch : clients) {
                if (ch.getUsername().equals(name))
                    toDelete = ch;
            }
            toDelete.sendMessage("kick#REKT");
            toDelete.disconnect();
            announce(String.format("%s has been kicked from the server!", name));

        }
    }

    /**
     * Bans a user from the server and sends the reason of ban.
     * This is the function of the client side ban request.
     *
     * @param name user to ban
     * @param reason why you want to ban this user
     */
    private void banUser(String name, String reason) {
        terminal.appendTextToTerminal(String.format("%s want's to ban %s.", thisUser.getUsername(), name));

        UserData selected = null;
        for (UserData ud : users) {
            if (ud.getUsername().equals(name))
                selected = ud;
        }

        if (selected == null)
            terminal.appendTextToTerminal("No such user!");
        else if (selected.getUsername().equals(thisUser.getUsername())) {
            terminal.appendTextToTerminal("Error: user wanted to ban himself :D");
            try {
                dos.writeUTF("ban#YOU");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (selected.getIsAdmin()) {
            terminal.appendTextToTerminal("Error: user wanted to ban another admin!");
            try {
                dos.writeUTF("ban#ADMIN");
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            selected.setBanned(true);
            terminal.appendTextToTerminal(String.format("Admin %s banned user: %s", thisUser.getUsername(), name));

            sendMessage("ban#BANNED");
            ClientHandler toDelete = null;
            for (ClientHandler ch : clients) {
                if (ch.getUsername().equals(name))
                    toDelete = ch;
            }
            toDelete.sendMessage(String.format("ban#%s", reason));
            toDelete.disconnect();
            announce(String.format("%s has been banned from the server!", name));
        }
    }

    /**
     * @return username of the user of this client
     */
    public String getUsername() {
        return thisUser.getUsername();
    }

    /**
     * Sets, that the terminal should display every messages
     * going through the server.
     *
     * @param show show the messages in the terminal or not
     */
    public static void showMsg(boolean show)
    {
        showMessages = show;
    }
}
