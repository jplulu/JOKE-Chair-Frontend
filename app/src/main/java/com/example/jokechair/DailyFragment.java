package com.example.jokechair;

import android.annotation.SuppressLint;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.example.jokechair.Requests.DailyMetricsRequest;
import com.example.jokechair.Requests.OnTaskCompleted;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.util.TimerTask;
import java.util.concurrent.ExecutionException;


public class DailyFragment extends Fragment implements OnTaskCompleted {

    private RequestQueue mQueue;
    public DailyMetrics dailyMetrics;

    TextView textView3;
    ProgressBar spinner;
    TextView timeSpentSittingChart;
    TextView timeSpentSittingText;

    ProgressBar percentSpentProperChartOuter;
    ProgressBar percentSpentProperChartInner;
    FrameLayout percentSpentProperFrame;
    TextView percentSpentProperText;

    ProgressBar percentSpentImproperChartOuter;
    ProgressBar percentSpentImproperChartInner;
    FrameLayout percentSpentImproperFrame;
    TextView percentSpentImproperText;
    TextView mostCommonImproperText;

    View view;

    public DailyFragment(){
        super();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_daily, container, false);

        // Connect widget vars to the XML
        textView3 = (TextView) view.findViewById(R.id.textView3);
        spinner = (ProgressBar) view.findViewById(R.id.progressBar1);

        // Time spent sitting section
        timeSpentSittingChart = (TextView) view.findViewById(R.id.timeSpentSittingChart);
        timeSpentSittingText = (TextView) view.findViewById(R.id.timeSpentSittingText);

        // Percent spent proper frame
        percentSpentProperChartOuter = (ProgressBar) view.findViewById(R.id.percentSpentProperChartOuter);
        percentSpentProperChartInner = (ProgressBar) view.findViewById(R.id.percentSpentProperChartInner);
        percentSpentProperFrame = (FrameLayout) view.findViewById(R.id.percentSpentProperFrame);
        percentSpentProperText = (TextView) view.findViewById(R.id.percentSpentProperText);


        // Percent spent improper frame
        percentSpentImproperFrame = (FrameLayout) view.findViewById(R.id.percentSpentImproperFrame);
        percentSpentImproperChartOuter = (ProgressBar) view.findViewById(R.id.percentSpentImproperChartOuter);
        percentSpentImproperChartInner = (ProgressBar) view.findViewById(R.id.percentSpentImproperChartInner);
        percentSpentImproperText = (TextView) view.findViewById(R.id.improperPercentText);
        mostCommonImproperText = (TextView) view.findViewById(R.id.mostCommonImproperText);

        @SuppressLint("UseCompatLoadingForDrawables") Drawable circleOuterProper =  getResources().getDrawable(R.drawable.circle_outer);
        percentSpentProperChartOuter.setProgressDrawable(circleOuterProper);
        @SuppressLint("UseCompatLoadingForDrawables") Drawable circleOuterImproper =  getResources().getDrawable(R.drawable.circle_outer);
        percentSpentImproperChartOuter.setProgressDrawable(circleOuterImproper);

        // start HTTP requests
        DailyMetricsRequest dailyMetricsRequest = new DailyMetricsRequest(this);
        dailyMetricsRequest.execute();



        return view;


    }

    public static DailyFragment newInstance() {
        Bundle args = new Bundle();
        DailyFragment fragment = new DailyFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onTaskCompleted(JSONObject response) throws JSONException {

        String percentSpentProper = response.getString("percent_spent_proper");
        String percentSpentImproper = response.getString("percent_most_common_improper");
        String mostCommonImproper = response.getString("most_common_improper");
        System.out.println(percentSpentImproper);
        spinner.setVisibility(View.GONE);

        // Create time spent sitting section
        timeSpentSittingChart.setText(String.valueOf(response.getInt("time_spent_sitting")));
        timeSpentSittingChart.setVisibility(View.VISIBLE);
        timeSpentSittingText.setVisibility(View.VISIBLE);

        // Create percentSpentProperChart
        percentSpentProperChartInner.setProgress((int) Float.parseFloat(percentSpentProper));
        textView3.setText(response.getString("percent_spent_proper") + "%");
        percentSpentProperFrame.setVisibility(View.VISIBLE);
        percentSpentProperText.setVisibility(View.VISIBLE);

//         Create percentSpentImproperChart
        percentSpentImproperChartInner.setProgress((int) Float.parseFloat(percentSpentImproper));
//        percentSpentImproperChartOuter.setProgress((int) Float.parseFloat(percentSpentImproper));
        percentSpentImproperText.setText(percentSpentImproper + "%");
        percentSpentImproperFrame.setVisibility(View.VISIBLE);
        mostCommonImproperText.setText("OF THE TIME SPENT IN POSTURE: " + mostCommonImproper);
        mostCommonImproperText.setVisibility(View.VISIBLE);

    }
}


