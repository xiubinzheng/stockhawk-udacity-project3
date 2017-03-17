package com.udacity.stockhawk.utility;

import android.content.ContentProviderOperation;
import android.util.Log;

import com.udacity.stockhawk.data.QuoteColumns;
import com.udacity.stockhawk.data.QuoteProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;


public class StockUtility {

    private static String LOG_TAG = StockUtility.class.getSimpleName();

    public static boolean showPercent = true;

    public static ContentProviderOperation buildBatchOperation(JSONObject jsonObject) {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                QuoteProvider.Quotes.CONTENT_URI);
        try {
            String change = jsonObject.getString("Change");
            builder.withValue(QuoteColumns.SYMBOL, jsonObject.getString("symbol"));
            builder.withValue(QuoteColumns.BIDPRICE, formatBidPrice(jsonObject.getString("Bid")));
            builder.withValue(QuoteColumns.PERCENT_CHANGE, formatPercentChange(
                    jsonObject.getString("ChangeinPercent"), true));
            builder.withValue(QuoteColumns.CHANGE, formatPercentChange(change, false));
            builder.withValue(QuoteColumns.ISCURRENT, 1);
            if (change.charAt(0) == '-') {
                builder.withValue(QuoteColumns.ISUP, 0);
            } else {
                builder.withValue(QuoteColumns.ISUP, 1);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return builder.build();
    }

    public static ArrayList getContentProviderOperations(String JSON) {
        ArrayList<ContentProviderOperation> providerOperationList = new ArrayList<>();
        JSONObject quoteJsonObject = null;
        JSONArray resultsJsonArray = null;
        try {
            quoteJsonObject = new JSONObject(JSON);
            if (quoteJsonObject != null && quoteJsonObject.length() != 0) {
                quoteJsonObject = quoteJsonObject.getJSONObject("query");
                int count = Integer.parseInt(quoteJsonObject.getString("count"));
                if (count == 1) {
                    quoteJsonObject = quoteJsonObject.getJSONObject("results")
                            .getJSONObject("quote");
                    providerOperationList.add(buildBatchOperation(quoteJsonObject));
                } else {
                    resultsJsonArray = quoteJsonObject.getJSONObject("results").getJSONArray("quote");

                    if (resultsJsonArray != null && resultsJsonArray.length() != 0) {
                        for (int i = 0; i < resultsJsonArray.length(); i++) {
                            quoteJsonObject = resultsJsonArray.getJSONObject(i);
                            providerOperationList.add(buildBatchOperation(quoteJsonObject));
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(LOG_TAG, "converting string to JSON failed: " + e);
        }
        return providerOperationList;
    }

    public static String formatBidPrice(String bidPrice) {
        bidPrice = String.format("%.2f", Float.parseFloat(bidPrice));
        return bidPrice;
    }

    public static String formatPercentChange(String change, boolean isPercentChange) {
        String weight = change.substring(0, 1);
        String ampersand = "";
        if (isPercentChange) {
            ampersand = change.substring(change.length() - 1, change.length());
            change = change.substring(0, change.length() - 1);
        }
        change = change.substring(1, change.length());
        double round = (double) Math.round(Double.parseDouble(change) * 100) / 100;
        change = String.format("%.2f", round);
        StringBuffer changeBuffer = new StringBuffer(change);
        changeBuffer.insert(0, weight);
        changeBuffer.append(ampersand);
        change = changeBuffer.toString();
        return change;
    }


}