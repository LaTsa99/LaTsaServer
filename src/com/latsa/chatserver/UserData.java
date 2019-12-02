package com.latsa.chatserver;

import java.awt.*;
import java.io.Serializable;

public class UserData implements Serializable {
    private String username;
    private String password;
    private boolean isAdmin;
    private boolean banned;
    private Color color;

    public UserData(String username, String passwordHash) {
        this.username = username;
        this.password = passwordHash;
        isAdmin = false;
        banned = false;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public boolean getIsAdmin() {
        return isAdmin;
    }

    public boolean getBanned() {
        return banned;
    }

    public Color getColor() {
        return color;
    }

    public void setAdmin(boolean b) {
        isAdmin = b;
    }

    public void setBanned(boolean ban) {
        banned = ban;
    }

    public void setColor(Color c) {
        color = c;
    }
}
