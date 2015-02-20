package com.learning.self.parikshit.locationtracker;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.util.ArrayList;


public class MainActivity extends ActionBarActivity implements
        com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks,
        com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = MainActivity.class.getSimpleName();



    LocationManager mLocationManager;
    GoogleApiClient mGoogleApiClient;
    LocationRequest mLocationRequest; // location request object passed to fused location manager
    Location mLastLocation; // stores the last retrieved address
    AddressResultReceiver mResultReceiver;  // receiver to get result from intent service
    ArrayAdapter<String> mArrayAdapter;

    // UI elements declaration

    Button lastKnownLocationButton;
    Button startTrackingButton;
    Button stopTrackingButton;
    Button getAddressButton;
    RadioGroup locationProvider;
    TextView latLongView;
    ListView addressListView;

    // Global variables
    String currLocationProvider;
    boolean isPlayServiceActive; // checks if google play services are active
    boolean isTrackingEnabled; // is tracking currently on
    ArrayList<String> addressList;  // list of address to be displayed in list view


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // perform initializations
        buildGoogleApiClient();
        createLocationRequest();

        // initialize UI elements
        latLongView = (TextView)findViewById(R.id.textViewLatLong);
        lastKnownLocationButton = (Button) findViewById(R.id.buttonLast);
        startTrackingButton = (Button)findViewById(R.id.buttonStart);
        stopTrackingButton = (Button)findViewById(R.id.buttonStop);
        locationProvider = (RadioGroup)findViewById(R.id.locationProviderGroup);
        getAddressButton = (Button) findViewById(R.id.buttonGetAddress);
        addressListView = (ListView) findViewById(R.id.addressListView);

        // initialize global variables
        currLocationProvider = LocationManager.NETWORK_PROVIDER;    // default location provider
        addressList = new ArrayList<String>();
        mResultReceiver = new AddressResultReceiver(new Handler());
        mArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, addressList);
        addressListView.setAdapter(mArrayAdapter);

        // get the radio group and register a listener for change in enabled radio button
        locationProvider.setOnCheckedChangeListener(new locationProviderChanged());

        // set the default location manager to network reported location
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // register listeners for button pressed events
        startTrackingButton.setOnClickListener(new startTrackingPressed());
        stopTrackingButton.setOnClickListener(new stopTrackingPressed());
        getAddressButton.setOnClickListener(new getReverseGeocodedAddress());

        // initialize last known location button and handle click events to get and update last
        // known location
        lastKnownLocationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (isPlayServiceActive) {
                    Location lastLocation = LocationServices.FusedLocationApi.getLastLocation(
                            mGoogleApiClient);
                    if (lastLocation != null) {
                        updateTextView(lastLocation);
                    }
                }
                else
                    Toast.makeText(getApplicationContext(), "Google play service is not active",
                            Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mGoogleApiClient != null)
            mGoogleApiClient.connect();
    }

    /**
     * Create an instance of the Google Play Services API client
     */
    protected void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        Toast.makeText(getApplicationContext(),"Google play service API requested", Toast.LENGTH_SHORT).show();
    }

    /**
     * Create a location request object which specifies the interval between location updates
     * and the accuracy and power constraints
     */

    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }


    /**
     * Listener for button press events for getting reverse geo coded addresses
     */
    public class getReverseGeocodedAddress implements Button.OnClickListener {

        @Override
        public void onClick(View v) {
            if (mLastLocation != null)
                startIntentService();
            else
                Toast.makeText(getApplicationContext(), "Last known location is null", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Start intent service to get reverse geocoded address for last known location
     */
    protected void startIntentService() {
        Intent intent = new Intent(this, FetchAddressIntentService.class);
        intent.putExtra(Constants.RECEIVER, mResultReceiver);
        intent.putExtra(Constants.LOCATION_DATA_EXTRA, mLastLocation);
        startService(intent);
    }


    class AddressResultReceiver extends ResultReceiver {


        /**
         * Create a new ResultReceive to receive results.  Your
         * {@link #onReceiveResult} method will be called from the thread running
         * <var>handler</var> if given, or from an arbitrary thread if null.
         *
         * @param handler
         */
        public AddressResultReceiver(Handler handler) {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            Toast.makeText(getApplicationContext(),"Reverse geo coding results received", Toast.LENGTH_SHORT).show();
            // Display the address string
            // or an error message sent from the intent service.
            if (resultCode == Constants.SUCCESS_RESULT) {
                ArrayList<String> mAddressOutput = resultData.getStringArrayList(Constants.RESULT_DATA_KEY);
                updateAddressListView(mAddressOutput);
            }

            // Show a toast message if an address was not found.
            else if (resultCode == Constants.FAILURE_RESULT) {
                Toast.makeText(getApplicationContext(), "Could not retrieve address", Toast.LENGTH_SHORT).show();
            }
        }
    }


    /**
     * Updates the list view with the new addresses
     * @param addresses : List of addresses
     */
    private void updateAddressListView(ArrayList<String> addresses) {
        mArrayAdapter.clear();
        for (String address : addresses)
            mArrayAdapter.add(address);
        mArrayAdapter.notifyDataSetChanged();
    }

    /**
     * Listener for radio button check/un-check events
     * De-registers the previous location request and registers for a new location update
     */
    public class locationProviderChanged implements RadioGroup.OnCheckedChangeListener {

        public void onCheckedChanged(RadioGroup group, int checkedId) {
            // un register any previously registered listener
            if (isTrackingEnabled)
                startTracking(checkedId);
        }
    }

    /**
     * Start tracking user location based on the provider enabled
     * @param checkedId
     */
    private void startTracking(int checkedId) {
        if (isTrackingEnabled) {
            unregisterListener();
            switch (checkedId) {
                case R.id.radioButtonNetwork:
                    registerListener(LocationManager.NETWORK_PROVIDER);
                    break;
                case R.id.radioButtonGPS:
                    registerListener(LocationManager.GPS_PROVIDER);
                    break;
                case R.id.radioButtonFused:
                    requestFusedLocationUpdate();
                    break;
                default:
                    Log.e(TAG, "Radio group checkedId is not recognized : " + checkedId);
            }
        }
    }

    /**
     * Unregister all location listeners
     */
    private void stopTracking() {
        unregisterListener();
        stopFusedLocationUpdates();
    }

    /**
     * Handler for click event on start tracking button
     */

     private class startTrackingPressed implements Button.OnClickListener  {

        @Override
        public void onClick(View v) {
            isTrackingEnabled = true;
            startTracking(locationProvider.getCheckedRadioButtonId());
        }
    };


    private class stopTrackingPressed implements Button.OnClickListener {

        @Override
        public void onClick(View v) {
            isTrackingEnabled = false;
            stopTracking();
        }
    }


    /**
     * Declare a location listener for new location updates
     */

    LocationListener mLocationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            updateTextView(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {
            Toast.makeText(getApplicationContext(), provider + " has been enabled", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderDisabled(String provider) {
            Toast.makeText(getApplicationContext(), provider + " is disabled. Kindly enable it ", Toast.LENGTH_SHORT).show();
        }
    };


    /**
     * Declare a location listener for fused location updates for google play services
     */

    com.google.android.gms.location.LocationListener mFusedLocationListener =
            new com.google.android.gms.location.LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
            updateTextView(location);
        }
    };

    /**
     * Update text view with the latitude, longitude and accuracy
     * @param location : Contains the latitude, longitude and accuracy of location reported
     */
    private void updateTextView(Location location) {
        if (location != null) {
            mLastLocation = location;
            double lat = location.getLatitude();
            double lon = location.getLongitude();
            double accuracy = location.getAccuracy();
            String text = "Lat : " + lat + ", Lon : " + lon + ", Accuracy : " + accuracy;
            // underline the text view
            latLongView.setPaintFlags(latLongView.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            latLongView.setText(text);
        }

    }


    /**
     * Register for a location update from the given provider
     * @param provider : Either Network, WiFi or GPS
     */
    private void registerListener(String provider) {
        mLocationManager.requestLocationUpdates(provider, 0, 0, mLocationListener);
    }


    /**
     * Register for location updates from the fused location API
     */
    private void requestFusedLocationUpdate() {
        if (isPlayServiceActive)
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest
                , mFusedLocationListener);
    }

    /**
     * Remove request for location updates
     */

    protected void stopFusedLocationUpdates() {
        try {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, mFusedLocationListener);
        }
        catch(Exception e) {
            Log.e(TAG, "Error in removing fused location update request : " + e.getMessage());
        }
    }


    /**
     * Remove any location update that you are getting from the location manager
     */
    private void unregisterListener() {
        try {
            mLocationManager.removeUpdates(mLocationListener);
        }
        catch(Exception e) {
            Log.e(TAG, "Error in unregistering location listener : " + e.getMessage() );
        }

    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected())
            mGoogleApiClient.disconnect();
        // un register your listener when app dies
        stopTracking();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Toast.makeText(getApplicationContext(), "Connected to google play services", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Connected to google play services");
        isPlayServiceActive = true;
    }

    @Override
    public void onConnectionSuspended(int i) {
        Toast.makeText(getApplicationContext(), "Google play service request suspended", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Google play service request suspended");
        isPlayServiceActive = false;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        isPlayServiceActive = false;
        Toast.makeText(getApplicationContext(),"Sorry! Connection to Google Play services failed", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "Sorry! Connection to Google Play services failed");
    }
}
