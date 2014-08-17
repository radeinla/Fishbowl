package com.softwarelab7.fishbowl.activities;

import android.app.Activity;
import android.content.IntentSender;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.softwarelab7.fishbowl.R;
import com.softwarelab7.fishbowl.models.Sale;
import com.softwarelab7.fishbowl.models.Session;
import com.softwarelab7.fishbowl.sqlite.DbHelper;

import java.io.IOException;
import java.sql.Time;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class Sell extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static float LOCATION_CHANGE_THRESHOLD = 30.0f;

    private static long LOCATION_CHANGE_INTERVAL = TimeUnit.MINUTES.toMillis(5);

    private static long LOCATION_CHANGE_FASTEST_INTERVAL = TimeUnit.MINUTES.toMillis(5);

    private final static int
            CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private static final String TAG = "Sell";

    private LocationClient mLocationClient;

    Date date;
    DbHelper dbHelper;
    Session activeSession;
    long sold = 0;
    String location;
    Double lat = 14.6549;
    Double lon = 121.0645;
    AsyncTask<Location,Void,Address> lastAsyncAddressTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Starting application!!!!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sell);
        dbHelper = new DbHelper(getApplicationContext());
        mLocationClient = new LocationClient(this, this, this);
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
        Sale latestTransaction = dbHelper.getLatestTransaction();
        if (latestTransaction != null) {
            setCoordinates(latestTransaction.lat, latestTransaction.lon);
        }
        setSold(dbHelper.getSoldForSession(activeSession));
        setDate(new Date());
    }

    private void setCoordinates(double lat, double lon) {
        this.lat = lat;
        this.lon = lon;
        Log.d(TAG, "Current location: " + getCoordinateString());
        triggerUpdateLocationTask();
    }

    private Sale createSale() {
        Sale sale = new Sale();
        sale.lat = lat;
        sale.lon = lon;
        sale.session = activeSession.id;
        sale.dateCreated = new Date();
        return sale;
    }

    private void increaseSoldCount() {
        setSold(sold+1);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Log.d(TAG, "GooPS connected!");
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this) == ConnectionResult.SUCCESS) {

            //uncomment when debugging
            LOCATION_CHANGE_THRESHOLD = 5.0f; // 5 meters
            LOCATION_CHANGE_INTERVAL = TimeUnit.SECONDS.toMillis(2); // actively check for location updates every 2 minutes
            LOCATION_CHANGE_FASTEST_INTERVAL = TimeUnit.SECONDS.toMillis(2); // fastest interval is 2 minutes

            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setSmallestDisplacement(LOCATION_CHANGE_THRESHOLD)
                    .setInterval(LOCATION_CHANGE_INTERVAL)
                    .setFastestInterval(LOCATION_CHANGE_FASTEST_INTERVAL);
            Log.d(TAG, "interval check: " + locationRequest.getInterval() + ", fastest: " + locationRequest.getFastestInterval());
            Log.d(TAG, "Expiration! " + locationRequest.getExpirationTime());
            mLocationClient.requestLocationUpdates(locationRequest, this);
            Location currentLocation = mLocationClient.getLastLocation();
            if (currentLocation != null) {
                this.onLocationChanged(currentLocation);
            }
        } else {
            Log.d(TAG, "GooPS not connected!");
            Toast.makeText(this, "GPS not available. Please install GooglePlayServices and enable GPS.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDisconnected() {
        Toast.makeText(this, "GPS not available. Please install GooglePlayServices and enable GPS.", Toast.LENGTH_LONG).show();
        Log.d(TAG, "GooPS disconnected!");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(this, "GPS not available. Please install GooglePlayServices and enable GPS.", Toast.LENGTH_LONG).show();
        Log.d(TAG, "Was not able to connect to google play services");
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        this,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();
    }

    @Override
    protected void onStop() {
        if (mLocationClient.isConnected()) {
            mLocationClient.removeLocationUpdates(this);
        }
        mLocationClient.disconnect();
        super.onStop();
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, "Location changed!!!!!");
        Toast.makeText(this, "You have moved, creating new session!", Toast.LENGTH_SHORT).show();
        if (location.distanceTo(getCurrentLocation()) > LOCATION_CHANGE_THRESHOLD) {
            closeCurrentSession();
            setCoordinates(location.getLatitude(), location.getLongitude());
        }
    }

    private void triggerUpdateLocationTask() {
        setLocation(getString(R.string.location_updating_label));
        if (lastAsyncAddressTask != null) {
            lastAsyncAddressTask.cancel(true);
        }
        lastAsyncAddressTask = new AsyncTask<Location,Void,Address>() {
            @Override
            protected void onPostExecute(Address address) {
                setLocation(getLocationFromAddress(address));
            }

            @Override
            protected Address doInBackground(Location... params) {
                Location location = params[0];
                Log.d(TAG, "async task got location: " + location.getLatitude() + ", " + location.getLongitude());
                return getAddress(params[0]);
            }
        };
        lastAsyncAddressTask.execute(getCurrentLocation());
    }

    private String getLocationFromAddress(Address address) {
        if (address == null) {
            return getCoordinateString();
        } else {
            if (address.getMaxAddressLineIndex() > -1) {
                return address.getAddressLine(0);
            } else {
                if (address.getLocality() == null) {
                    return address.getCountryCode();
                } else {
                    return address.getLocality();
                }
            }
        }
    }

    private String getCoordinateString() {
        return "("+lat+","+lon+")";
    }

    private Location getCurrentLocation() {
        Location location = new Location(Sell.class.getSimpleName());
        Log.d(TAG, "Current location: " + getCoordinateString());
        location.setLatitude(lat);
        location.setLongitude(lon);
        return location;
    }

    private Address getAddress(Location location) {
        Log.d(TAG, "async task geocode got location: " + location.getLatitude() + ", " + location.getLongitude());
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addressList = geocoder.getFromLocation(
                    location.getLatitude(), location.getLongitude(), 1);
            if (addressList != null && !addressList.isEmpty()) {
                Address address = addressList.get(0);
                System.out.println("Locality: " + address.getLocality());
                System.out.println("Country: " + address.getCountryCode());
                return address;
            } else {
                return null;
            }
        } catch (IOException ioException) {
            Log.d(TAG, "Received ioException from geocoding: " + ioException.getMessage(), ioException);
            return null;
        }
    }

    private void setDate(Date date) {
        if (this.date == null) {
            closeCurrentSession();
        } else {
            Date previous = getDateOnly(this.date);
            Date current = getDateOnly(date);
            if (getDateOnly(current).after(previous)) {
                closeCurrentSession();
            }
        }
        this.date = date;
    }

    private void closeCurrentSession() {
        activeSession = dbHelper.closeCurrentSession(activeSession);
        setSold(0);
    }

    private Date getDateOnly(Date date) {
        Calendar calendar = getCalendar(date);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        return calendar.getTime();
    }

    private Calendar getCalendar(Date date) {
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTime(date);
        return calendar;
    }

}
