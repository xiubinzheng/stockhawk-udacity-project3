package com.udacity.stockhawk;

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.udacity.stockhawk.adapter.QuoteCursorAdapter;
import com.udacity.stockhawk.adapter.RecyclerViewItemClickListener;
import com.udacity.stockhawk.utility.StockUtility;
import com.udacity.stockhawk.data.QuoteColumns;
import com.udacity.stockhawk.data.QuoteProvider;
import com.udacity.stockhawk.helper.SimpleItemTouchHelperCallback;
import com.udacity.stockhawk.service.StockIntentService;
import com.udacity.stockhawk.service.StockTaskService;


public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int CURSOR_LOADER_ID = 0;

    private Context mContext;
    private CharSequence mTitle;
    private TextView textMessage;
    private Intent mServiceIntent;
    private QuoteCursorAdapter mCursorAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this;

        textMessage = (TextView) findViewById(R.id.text_message);

        mServiceIntent = new Intent(this, StockIntentService.class);
        if (savedInstanceState == null) {

            mServiceIntent.putExtra("tag", "init");
            if (isConnected()) {
                startService(mServiceIntent);
            }
        }
        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.addOnItemTouchListener(new RecyclerViewItemClickListener(this,
                new RecyclerViewItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View v, int position) {
                        String symbol = ((TextView) v.findViewById(R.id.stock_symbol)).getText().toString();
                        Intent intent = new Intent(mContext, StockDetailActivity.class);
                        intent.putExtra("symbol", symbol);
                        startActivity(intent);
                    }
                }));
        mCursorAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                refreshLayout();
            }
        });
        recyclerView.setAdapter(mCursorAdapter);

        FloatingActionButton floatingActionButton = (FloatingActionButton) findViewById(R.id.fab);
        floatingActionButton.attachToRecyclerView(recyclerView);
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isConnected()) {
                    new MaterialDialog.Builder(mContext)
                            .titleColor(getResources().getColor(R.color.colorPrimaryDark))
                            .title(R.string.symbol_search)
                            .contentColor(getResources().getColor(R.color.colorPrimary))
                            .content(R.string.search_title)
                            .inputType(InputType.TYPE_CLASS_TEXT)
                            .input(R.string.input_hint, R.string.input_prefill, new MaterialDialog.InputCallback() {
                                @Override
                                public void onInput(MaterialDialog dialog, CharSequence input) {
                                    
                                    Cursor cursor = getContentResolver().query(QuoteProvider.Quotes.CONTENT_URI,
                                            new String[]{QuoteColumns.SYMBOL}, QuoteColumns.SYMBOL + "= ?",
                                            new String[]{input.toString().toUpperCase()}, null);
                                    if (cursor.getCount() != 0) {
                                        Toast toast =
                                                Toast.makeText(MainActivity.this, R.string.already_saved,
                                                        Toast.LENGTH_LONG);
                                        toast.setGravity(Gravity.CENTER, Gravity.CENTER, 0);
                                        toast.show();
                                        return;
                                    } else {
                                        mServiceIntent.putExtra("tag", "add");
                                        mServiceIntent.putExtra("symbol", input.toString().toUpperCase());
                                        startService(mServiceIntent);
                                    }
                                }
                            })
                            .show();
                } else {
                    networkToast();
                }

            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();
        if (isConnected()) {
            long period = 3600L;
            long flex = 10L;
            String periodicTag = "periodic";

            PeriodicTask periodicTask = new PeriodicTask.Builder()
                    .setService(StockTaskService.class)
                    .setPeriod(period)
                    .setFlex(flex)
                    .setTag(periodicTag)
                    .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                    .setRequiresCharging(false)
                    .build();

            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }


        refreshLayout();
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
    }

    public void refreshLayout() {
        if (isConnected()) {
            if (mCursorAdapter.getItemCount() == 0) {
                textMessage.setText(R.string.none_selected);
                textMessage.setVisibility(View.VISIBLE);
            } else {
                textMessage.setVisibility(View.GONE);
            }
        } else {
            if (mCursorAdapter.getItemCount() == 0) {
                textMessage.setText(R.string.network_toast);
                textMessage.setVisibility(View.VISIBLE);
            } else {
                textMessage.setVisibility(View.GONE);
                Toast.makeText(this, R.string.app_offline, Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void networkToast() {
        Toast.makeText(mContext, getString(R.string.network_toast), Toast.LENGTH_SHORT).show();
    }

    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return (activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting());
    }

    // ActionBar
    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_activity_settings, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();


        if (id == R.id.action_change_units) {

            StockUtility.showPercent = !StockUtility.showPercent;
            this.getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this, QuoteProvider.Quotes.CONTENT_URI,
                new String[]{QuoteColumns._ID, QuoteColumns.SYMBOL, QuoteColumns.BIDPRICE,
                        QuoteColumns.PERCENT_CHANGE, QuoteColumns.CHANGE, QuoteColumns.ISUP},
                QuoteColumns.ISCURRENT + " = ?",
                new String[]{"1"},
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursorAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mCursorAdapter.swapCursor(null);
    }

}
