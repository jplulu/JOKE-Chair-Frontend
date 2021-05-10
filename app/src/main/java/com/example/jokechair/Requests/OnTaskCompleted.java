package com.example.jokechair.Requests;

import org.json.JSONException;
import org.json.JSONObject;

public interface OnTaskCompleted {
    void onTaskCompleted(JSONObject response) throws JSONException;
}
