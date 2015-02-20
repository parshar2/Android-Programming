package com.learning.self.parikshit.locationtracker;

import android.app.IntentService;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.ResultReceiver;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by admin on 2/20/15.
 */
public class FetchAddressIntentService extends IntentService {

    private static final String TAG = FetchAddressIntentService.class.getSimpleName();
    protected ResultReceiver mReceiver;
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public FetchAddressIntentService(String name) {
        super(name);
    }

    public FetchAddressIntentService() {
        super("FetchAddressIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG," Fetch address service started");

        mReceiver = intent.getParcelableExtra(Constants.RECEIVER);
        ArrayList<String> errorMessage = new ArrayList<String>();
        Geocoder mGeocoder = new Geocoder(this, Locale.getDefault());

        Location location = intent.getParcelableExtra(Constants.LOCATION_DATA_EXTRA);
        List<Address> addresses = null;
        try {
            addresses = mGeocoder.getFromLocation(
                    location.getLatitude(),
                    location.getLongitude(),
                    3);


        } catch (IOException ioException) {
            // Catch network or other I/O problems.
            errorMessage.add(getString(R.string.service_not_available));
        } catch (IllegalArgumentException illegalArgumentException) {
            // Catch invalid latitude or longitude values.
            errorMessage.add(getString(R.string.invalid_lat_long_used));
            Log.e(TAG, errorMessage + ". " +
                    "Latitude = " + location.getLatitude() +
                    ", Longitude = " +
                    location.getLongitude(), illegalArgumentException);
        }


        // Handle case where no address was found.
        if (addresses == null || addresses.size() == 0) {
            if (errorMessage.isEmpty()) {
                errorMessage.add(getString(R.string.no_address_found));
            }
            deliverResultToReceiver(Constants.FAILURE_RESULT, errorMessage);
        } else {
            Address address = addresses.get(0);
            ArrayList<String> addressFragments = new ArrayList<String>();

            // Fetch the address lines using getAddressLine,
            // join them, and send them to the thread.
            for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                addressFragments.add(address.getAddressLine(i));
            }
            Log.i(TAG, "Address found");
            deliverResultToReceiver(Constants.SUCCESS_RESULT,
                            addressFragments);

        }
    }

        private void deliverResultToReceiver(int resultCode, ArrayList<String> message) {
            Bundle bundle = new Bundle();
            bundle.putStringArrayList(Constants.RESULT_DATA_KEY, message);
            mReceiver.send(resultCode, bundle);
        }
}
