package com.caresync.utils;

import com.caresync.models.User;

public final class SessionManager {
    private static User currentUser;

    private SessionManager() {
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User currentUser) {
        SessionManager.currentUser = currentUser;
    }

    public static void clear() {
        currentUser = null;
    }
}
