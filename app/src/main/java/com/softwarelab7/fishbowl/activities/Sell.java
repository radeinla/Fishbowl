package com.softwarelab7.fishbowl.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.softwarelab7.fishbowl.R;
import com.softwarelab7.fishbowl.models.Sale;
import com.softwarelab7.fishbowl.models.Session;
import com.softwarelab7.fishbowl.sqlite.DbHelper;


public class Sell extends Activity {

    DbHelper dbHelper;
    Session activeSession;
    long sold = 0;
    String location;
    Double lat = null;
    Double lon = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dbHelper = new DbHelper(getApplicationContext());
        setContentView(R.layout.activity_sell);
        initializeContentView();
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
    }

    private void initializeContentView() {
        activeSession = dbHelper.getLatestSession();
        setSold(dbHelper.getSoldForSession(activeSession));
        Sale latestTransaction = dbHelper.getLatestTransaction();
        if (latestTransaction == null) {
            setLocation("Location");
        } else {
            setLocation(latestTransaction.location);
        }
    }

}
