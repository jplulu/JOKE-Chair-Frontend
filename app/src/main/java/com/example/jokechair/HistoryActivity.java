package com.example.jokechair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentTransaction;
import androidx.viewpager.widget.ViewPager;


import android.app.ActionBar;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.tabs.TabItem;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jpmml.evaluator.Evaluator;

public class HistoryActivity extends FragmentActivity {

    TabLayout tabLayout;
    ViewPager viewPager;
    FragmentManager fragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // Assign tab variables
        tabLayout = findViewById(R.id.tabsHistory);
        TabItem tDaily = findViewById(R.id.tDaily);
        viewPager = findViewById(R.id.viewPager);
        PagerAdapter pagerAdapter = new PagerAdapter(
                getSupportFragmentManager(),
                tabLayout.getTabCount()
        );

        viewPager.setAdapter(pagerAdapter);

        fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.historyFragment, DailyFragment.newInstance(), null)
                .setReorderingAllowed(true)
                .addToBackStack("Daily") // name can be null
                .commit();
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
                int position = tab.getPosition();
                switch (position) {
                    case 0:
                        fragmentManager.beginTransaction()
                                .replace(R.id.historyFragment, DailyFragment.newInstance(), null)
                                .setReorderingAllowed(true)
                                .addToBackStack("Daily") // name can be null
                                .commit();
                        break;
                    case 1:
                        fragmentManager.beginTransaction()
                                .replace(R.id.historyFragment, WeeklyFragment.newInstance(), null)
                                .setReorderingAllowed(true)
                                .addToBackStack("Weekly") // name can be null
                                .commit();
                        break;
                    case 2:
                        fragmentManager.beginTransaction()
                                .replace(R.id.historyFragment, AllFragment.newInstance(), null)
                                .setReorderingAllowed(true)
                                .addToBackStack("All Time") // name can be null
                                .commit();
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                switch(position) {
                    case 0:
                        fragmentManager.beginTransaction()
                                .replace(R.id.historyFragment, DailyFragment.newInstance(), null)
                                .addToBackStack("Daily")
                                .commit();
                        break;
                    default:
                        break;
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
    }


}