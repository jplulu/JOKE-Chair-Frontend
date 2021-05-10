package com.example.jokechair;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener {
    private final String TAG = "LoginActivity";

    private UserLocalStore userLocalStore;
    private Button bLogin;
    private EditText etUsername, etPassword;
    private TextView tvRegisterLink, tvWarnUsername, tvWarnPassword;

    private RequestQueue mQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = (EditText) findViewById(R.id.etUsername);
        etPassword = (EditText) findViewById(R.id.etPassword);
        bLogin = (Button) findViewById(R.id.bLogin);
        tvRegisterLink = (TextView) findViewById(R.id.tvRegisterLink);
        tvWarnUsername = (TextView) findViewById(R.id.tvWarnUsername);
        tvWarnPassword = (TextView) findViewById(R.id.tvWarnPassword);

        bLogin.setOnClickListener(this);
        tvRegisterLink.setOnClickListener(this);

        userLocalStore = new UserLocalStore(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.bLogin:
                processLogin();
                break;

            case R.id.tvRegisterLink:
                startActivity(new Intent(this, RegisterActivity.class));
                break;
        }
    }

    // TODO: Add authentication with server and retrieve user data
    private void processLogin() {
        if (validateData()) {
            mQueue = Volley.newRequestQueue(getApplicationContext());
            String url = String.format("http://localhost:3333/user/get_userlogin?email=%s&password=%s",
                    etUsername.getText(),
                    etPassword.getText());
            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            User user;
                            user = null;
                            try {
                                user = new User(response.getString("email"),
                                        response.getString("password"),
                                        response.getInt("uid"));
                                System.out.println(response.getString("password"));
                                if (user != null) {
                                    userLocalStore.storeUserData(user);
                                    userLocalStore.setUserLoggedIn(true);
                                    startActivity(new Intent(getApplicationContext(),
                                            MainActivity.class));
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    error.printStackTrace();
                }
            });
            mQueue.add(request);
        }
        if (validateData()) {
            User user = new User(null, null, 1);
            userLocalStore.storeUserData(user);
            userLocalStore.setUserLoggedIn(true);

            Log.d(TAG, etUsername.getText().toString() + " " + etPassword.getText().toString());

            startActivity(new Intent(this, MainActivity.class));
        }
    }

    private boolean validateData() {

        tvWarnUsername.setVisibility(View.GONE);
        tvWarnPassword.setVisibility(View.GONE);

        boolean valid = true;
        if (etUsername.getText().toString().equals("")) {
            tvWarnUsername.setVisibility(View.VISIBLE);
            tvWarnUsername.setText("Field must not be empty");
            valid = false;
        }

        if (etPassword.getText().toString().equals("")) {
            tvWarnPassword.setVisibility(View.VISIBLE);
            tvWarnPassword.setText("Field must not be empty");
            valid = false;
        }

        return valid;
    }

}