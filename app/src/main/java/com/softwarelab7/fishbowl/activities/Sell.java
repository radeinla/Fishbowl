package com.softwarelab7.fishbowl.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.softwarelab7.fishbowl.R;
import com.softwarelab7.fishbowl.models.Sale;
import com.softwarelab7.fishbowl.models.Session;
import com.softwarelab7.fishbowl.sqlite.DbHelper;

import java.util.Date;


public class Sell extends Activity {

    DbHelper dbHelper;
    Session activeSession;
    long sold = 0;
    String location = "UP Diliman";
    Double lat = 14.6549;
    Double lon = 121.0645;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getApplicationContext());
        setContentView(R.layout.activity_sell);
        initializeContentView();
        findViewById(R.id.sale).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseSoldCount();
                dbHelper.insert(createSale());
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.sell, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setSold(long sold) {
        this.sold = sold;
        ((TextView)findViewById(R.id.sold)).setText(Long.toString(sold));
    }

    private void setLocation(String location) {
        this.location = location;
        ((TextView)findViewById(R.id.location)).setText(location);
    }

    private void initializeContentView() {
        activeSession = dbHelper.getLatestSession();
        setSold(dbHelper.getSoldForSession(activeSession));
        Sale latestTransaction = dbHelper.getLatestTransaction();
        if (latestTransaction != null) {
            setLocation(latestTransaction.location);
        }
    }

    private Sale createSale() {
        Sale sale = new Sale();
        sale.lat = lat;
        sale.lon = lon;
        sale.location = location;
        sale.session = activeSession.id;
        sale.dateCreated = new Date();
        return sale;
    }

    private void increaseSoldCount() {
        setSold(sold+1);
    }

}
