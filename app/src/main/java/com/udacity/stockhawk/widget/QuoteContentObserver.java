package com.udacity.stockhawk.widget;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.database.ContentObserver;
import android.os.Handler;

import com.udacity.stockhawk.R;



public class QuoteContentObserver extends ContentObserver {
    private AppWidgetManager mAppWidgetManager;
    private ComponentName mComponentName;

    QuoteContentObserver(AppWidgetManager appWidgetManager, ComponentName componentName, Handler handler) {
        super(handler);
        this.mAppWidgetManager = appWidgetManager;
        this.mComponentName = componentName;
    }

    @Override
    public void onChange(boolean selfChange) {
        mAppWidgetManager.notifyAppWidgetViewDataChanged(
                mAppWidgetManager.getAppWidgetIds(mComponentName), R.id.widget_list);
    }
}