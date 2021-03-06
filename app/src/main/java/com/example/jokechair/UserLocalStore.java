package com.example.jokechair;

import android.content.Context;
import android.content.SharedPreferences;

public class UserLocalStore {
    public final String SP_NAME = "userDetails";

    SharedPreferences userLocalDatabase;

    public UserLocalStore(Context context) {
        userLocalDatabase = context.getSharedPreferences(SP_NAME, 0);
    }

    public void storeUserData(User user) {
        SharedPreferences.Editor userLocalDatabaseEditor = userLocalDatabase.edit();
        userLocalDatabaseEditor.putString("email", user.getEmail());
        userLocalDatabaseEditor.putString("password", user.getPassword());
        userLocalDatabaseEditor.putInt("uid", user.getUid());
        userLocalDatabaseEditor.apply();
    }

    public void setUserLoggedIn(boolean loggedIn) {
        SharedPreferences.Editor userLocalDatabaseEditor = userLocalDatabase.edit();
        userLocalDatabaseEditor.putBoolean("loggedIn", loggedIn);
        userLocalDatabaseEditor.apply();
    }

    public void clearUserData() {
        SharedPreferences.Editor userLocalDatabaseEditor = userLocalDatabase.edit();
        userLocalDatabaseEditor.clear();
        userLocalDatabaseEditor.apply();
    }

    public User getLoggedInUser() {
        if (!userLocalDatabase.getBoolean("loggedIn", false)) {
            return null;
        }

        String email = userLocalDatabase.getString("email", "");
        String password = userLocalDatabase.getString("password", "");
        int uid = userLocalDatabase.getInt("uid", 0);

        return new User(email, password, uid);
    }
}
