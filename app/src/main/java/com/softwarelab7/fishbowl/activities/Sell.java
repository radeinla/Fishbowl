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
import com.softwarelab7.fishbowl.utils.Haversine;
import com.softwarelab7.fishbowl.utils.LocationUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;
import java.sql.Time;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;


public class Sell extends Activity implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener,
        com.google.android.gms.location.LocationListener {

    private static float LOCATION_CHANGE_THRESHOLD = 30.0f;

    private static long LOCATION_CHANGE_INTERVAL = TimeUnit.MINUTES.toMillis(5);

    private static long LOCATION_CHANGE_FASTEST_INTERVAL = TimeUnit.MINUTES.toMillis(5);

    private static long SUGGESTION_PERIOD = TimeUnit.MINUTES.toMillis(10);

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

    Timer suggestionToasterTimer;
    SuggestLocationToasterTimerTask suggestionToaster;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "Starting application!!!!");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sell);
        dbHelper = new DbHelper(getApplicationContext());
        mLocationClient = new LocationClient(this, this, this);
        findViewById(R.id.sale).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                increaseSoldCount();
                dbHelper.insert(createSale());
            }
        });
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

    private Location extractNextLocation (Date timeOfNotification, Location location) {
        if (timeOfNotification == null) {
            timeOfNotification = new Date();
        }

        List <Sale> saleList = dbHelper.getPastSales();

        if (saleList.size()> 0) {
            HashMap<Long, List<Sale>> salesBySession = new HashMap<Long, List<Sale>>();

            for (int i = 0; i < saleList.size(); i++) {
                Sale sale = saleList.get(i);
                List<Sale> sales = salesBySession.get(sale.session);
                if (sales == null) {
                    sales = new ArrayList<Sale>();
                }
                sales.add(sale);
                salesBySession.put(sale.session, sales);
            }

            List<SessionData> allSalesData = new ArrayList<SessionData>();

            for (HashMap.Entry<Long, List<Sale>> entry : salesBySession.entrySet()) {
                Long sessionId = entry.getKey();
                List<Sale> sales = entry.getValue();
                SessionData sessionData = new SessionData();
                sessionData.request = timeOfNotification;
                sessionData.requestLocation = location;
                sessionData.sold = sales.size();

                //because that is the location where you got your first sale on that session..
                Sale firstSale = sales.get(0);
                sessionData.sessionId = sessionId;
                sessionData.lat = firstSale.lat;
                sessionData.lon = firstSale.lon;


                for (int i = 0; i < sales.size(); i++) {
                    Sale sale = sales.get(i);

                    if (sessionData.from == null || sale.dateCreated.compareTo(sessionData.from) < 0) {
                        sessionData.from = sale.dateCreated;
                    }

                    if (sessionData.to == null || sale.dateCreated.compareTo(sessionData.to) > 0) {
                        sessionData.to = sale.dateCreated;
                    }
                }

                allSalesData.add(sessionData);
            }
            Collections.sort(allSalesData);
            SessionData best = allSalesData.get(allSalesData.size()-1);
            Log.d(TAG, "Sold here: " + best.sold);
            return LocationUtils.createLocation(best.lat, best.lon);
        } else {
            return null;
        }
    }

    private class SessionData implements Comparable<SessionData> {
        Date request;
        Location requestLocation;
        Date from;
        Date to;
        long sessionId;
        double lat;
        double lon;
        long sold;

        double getScore() {
            double score = 0;
            int sameWeekDayType = 0;
            if (getWeekDayType(getDayOfWeek(request)) == getWeekDayType(getDayOfWeek(from))) {
                sameWeekDayType = 1;
            }
            long dayDifference = TimeUnit.MILLISECONDS.toDays(request.getTime()-from.getTime());
            double locationDifference = Haversine.haversine(requestLocation.getLatitude(), requestLocation.getLongitude(), lat, lon);
            score += (((double)sold)/(to.getTime()-from.getTime())) * 0.4;
            score += ((double)sameWeekDayType) * 0.8;
            score += ((double)dayDifference) * 0.2;
            score += locationDifference * 0.3;
            return score;
        }

        int getDayOfWeek(Date date) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(from);
            return calendar.get(Calendar.DAY_OF_WEEK);
        }

        int getWeekDayType(int dayOfWeek) {
            if (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) {
                return 0;
            } else {
                return 1;
            }
        }

        @Override
        public int compareTo(SessionData rhs) {
            return Double.compare(this.getScore(), rhs.getScore());
        }
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
        suggestionToasterTimer = new Timer();
        suggestionToaster = new SuggestLocationToasterTimerTask();
        suggestionToaster.location = getCurrentLocation();
        suggestionToaster.date = new Date();
        suggestionToasterTimer.schedule(suggestionToaster, 0, SUGGESTION_PERIOD);
    }

    private class SuggestLocationToasterTimerTask extends TimerTask {
        Location location;
        Date date;

        @Override
        public void run() {
            final Location suggestedLocation = extractNextLocation(date, location);
            if (suggestedLocation != null) {
                final String suggestedLocationName = getLocationFromAddress(getAddress(suggestedLocation));
                Sell.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        TextView suggestionTextView = (TextView) Sell.this.findViewById(R.id.suggestion);
                        String suggestion = Sell.this.getString(R.string.new_location_suggestion_text,
                                suggestedLocationName);
                        suggestionTextView.setText(suggestion);
                    }
                });
            }
        }
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
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        suggestionToasterTimer.cancel();
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
                String locationFromAddress = getLocationFromAddress(address);
                setLocation(Sell.this.getString(R.string.sold_in_label, locationFromAddress));
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
        return LocationUtils.toCoordinateString(lat, lon);
    }

    private Location getCurrentLocation() {
        Log.d(TAG, "Current location: " + getCoordinateString());
        return LocationUtils.createLocation(lat, lon);
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
