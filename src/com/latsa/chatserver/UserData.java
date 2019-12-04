package com.latsa.chatserver;

import java.io.Serializable;

/**
 * Class that holds data of a user.
 */
public class UserData implements Serializable {
    private String username;
    private String password;
    private boolean isAdmin;
    private boolean banned;

    /**
     * Constructs a new data object.
     *
     * @param username name of the user
     * @param passwordHash hash of the password
     */
    public UserData(String username, String passwordHash) {
        this.username = username;
        this.password = passwordHash;
        isAdmin = false;
        banned = false;
    }

    /**
     * @return hash of the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return name of user
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return if this user is an admin
     */
    public boolean getIsAdmin() {
        return isAdmin;
    }

    /**
     * @return if this user is banned
     */
    public boolean getBanned() {
        return banned;
    }

    /**
     * @param b admin or not
     */
    public void setAdmin(boolean b) {
        isAdmin = b;
    }

    /**
     * @param ban ban or not
     */
    public void setBanned(boolean ban) {
        banned = ban;
    }
}
