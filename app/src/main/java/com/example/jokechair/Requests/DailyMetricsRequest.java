package com.example.jokechair.Requests;

import com.example.jokechair.Requests.RequestUtil;

import android.os.AsyncTask;

import com.android.volley.RequestQueue;
import com.example.jokechair.DailyFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class DailyMetricsRequest extends AsyncTask<Void, Void, JSONObject> {

    RequestQueue mQueue;
    OnTaskCompleted listener;

    int timeSpentSitting;
    String percentSpentProper;
    String mostCommonImproper;
    String percentMostCommonImproper;


    public DailyMetricsRequest(OnTaskCompleted listener) {
        this.listener = listener;
    }

    @Override
    protected void onPreExecute() {

    }

    @Override
    protected JSONObject doInBackground(Void... voids) {
//        mQueue = Volley.newRequestQueue(this.dailyFragment.getActivity().getApplicationContext());
        URL url = null;
        try {
            url = new URL("http://localhost:3333/posture/daily_metrics?uid=2");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        conn.setReadTimeout(150000); //milliseconds
        conn.setConnectTimeout(15000); // milliseconds
        try {
            conn.setRequestMethod("GET");
        } catch (ProtocolException e) {
            e.printStackTrace();
        }

        try {
            conn.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            JSONObject jsonObject = new JSONObject(RequestUtil.convertStreamToString(conn.getInputStream()));
            return jsonObject;
        }catch (JSONException | IOException err){
            err.printStackTrace();
        }

        return null;

//        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
//                new Response.Listener<JSONObject>() {
//                    @Override
//                    public void onResponse(JSONObject response) {
//                        try {
//                            timeSpentSitting = response.getInt("time_spent_sitting");
//                            System.out.println(timeSpentSitting);
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                        try {
//                            percentSpentProper = response.getString("percent_spent_proper");
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                        try {
//                            mostCommonImproper = response.getString("most_common_improper");
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                        try {
//                            percentMostCommonImproper = response.getString("most_common_improper");
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }, error -> {
//            error.printStackTrace();
//        });
//        mQueue.add(request);
    }

    @Override
    protected void onPostExecute(JSONObject response) {
        try {
            listener.onTaskCompleted(response);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
