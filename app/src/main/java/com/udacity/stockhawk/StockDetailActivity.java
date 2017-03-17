package com.udacity.stockhawk;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.eazegraph.lib.charts.ValueLineChart;
import org.eazegraph.lib.models.ValueLinePoint;
import org.eazegraph.lib.models.ValueLineSeries;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;


public class StockDetailActivity extends AppCompatActivity {

    private View errorMessage;
    private View progressCircle;
    private ValueLineChart lineChart;

    private boolean isLoaded = false;
    private String companySymbol;
    private String companyName;
    private ArrayList<String> labels;
    private ArrayList<Float> values;

    // Activity life cycle
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stock_detail);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        errorMessage = findViewById(R.id.error_message);
        progressCircle = findViewById(R.id.progress_circle);
        lineChart = (ValueLineChart) findViewById(R.id.line_chart);

        companySymbol = getIntent().getStringExtra("symbol");
        if (savedInstanceState == null) {
            downloadStockDetails();
        }
    }

    //activity state  save or restore
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (isLoaded) {
            outState.putString("company_name", companyName);
            outState.putStringArrayList("labels", labels);

            float[] valuesArray = new float[values.size()];
            for (int i = 0; i < valuesArray.length; i++) {
                valuesArray[i] = values.get(i);
            }
            outState.putFloatArray("values", valuesArray);
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.containsKey("company_name")) {
            isLoaded = true;

            companyName = savedInstanceState.getString("company_name");
            labels = savedInstanceState.getStringArrayList("labels");
            values = new ArrayList<>();

            float[] valuesArray = savedInstanceState.getFloatArray("values");
            for (float f : valuesArray) {
                values.add(f);
            }
            onDownloadCompleted();
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    // Home button click
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        } else {
            return false;
        }
    }

    // json information retrival and parsing
    private void downloadStockDetails() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url("http://chartapi.finance.yahoo.com/instrument/1.0/" + companySymbol + "/chartdata;type=quote;range=5y/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onResponse(Response response) throws IOException {
                if (response.code() == 200) {
                    try {
                        String result = response.body().string();
                        if (result.startsWith("finance_charts_json_callback( ")) {
                            result = result.substring(29, result.length() - 2);
                        }

                        JSONObject object = new JSONObject(result);
                        companyName = object.getJSONObject("meta").getString("Company-Name");
                        labels = new ArrayList<>();
                        values = new ArrayList<>();
                        JSONArray series = object.getJSONArray("series");
                        for (int i = 0; i < series.length(); i++) {
                            JSONObject seriesItem = series.getJSONObject(i);
                            SimpleDateFormat srcFormat = new SimpleDateFormat("yyyyMMdd");
                            String date = android.text.format.DateFormat.
                                    getMediumDateFormat(getApplicationContext()).
                                    format(srcFormat.parse(seriesItem.getString("Date")));
                            labels.add(date);
                            values.add(Float.parseFloat(seriesItem.getString("close")));
                        }

                        onDownloadCompleted();
                    } catch (Exception e) {
                        onDownloadFailed();
                        e.printStackTrace();
                    }
                } else {
                    onDownloadFailed();
                }
            }

            @Override
            public void onFailure(Request request, IOException e) {
                onDownloadFailed();
            }
        });
    }

    private void onDownloadCompleted() {
        StockDetailActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setTitle(companyName);

                progressCircle.setVisibility(View.GONE);
                errorMessage.setVisibility(View.GONE);

                ValueLineSeries series = new ValueLineSeries();
                series.setColor(0xFF56B7F1);
                for (int i = 0; i < labels.size(); i++) {
                    series.addPoint(new ValueLinePoint(labels.get(i), values.get(i)));
                }
                if (!isLoaded) {
                    lineChart.startAnimation();
                }
                lineChart.addSeries(series);
                lineChart.setVisibility(View.VISIBLE);

                isLoaded = true;
            }
        });
    }

    private void onDownloadFailed() {
        StockDetailActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                lineChart.setVisibility(View.GONE);
                progressCircle.setVisibility(View.GONE);
                errorMessage.setVisibility(View.VISIBLE);
                setTitle(R.string.error);
            }
        });
    }
}
