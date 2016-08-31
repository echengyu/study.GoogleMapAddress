package study.googlemapaddress;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

public class GmsLocationUtil implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    protected GoogleApiClient mGoogleApiClient;
    private LocationListener mLocationListener;
    private boolean needToReStart = false;
    private boolean mRequestingLocationUpdates = false;

    public GmsLocationUtil(Context context) {
        this(context, null);
    }

    public GmsLocationUtil(Context context, GoogleApiClient googleApiClient) {
        mGoogleApiClient = googleApiClient;
        initGoogleApiClient(context);
    }

    private void initGoogleApiClient(Context context) {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(context.getApplicationContext())
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        if (needToReStart) {
            startUpdateLocation(mLocationListener);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {
        if (mLocationListener != null) {
            mLocationListener.onLocationChanged(location);
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
    }

    private LocationRequest createLocationRequest() {
        LocationRequest request = new LocationRequest();
        request.setInterval(5000);
        request.setFastestInterval(2500);
        request.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return request;
    }

    public void startUpdateLocation(LocationListener locationListener) throws SecurityException {
        mLocationListener = locationListener;
        mRequestingLocationUpdates = true;

        if (mGoogleApiClient.isConnected()) {
            LocationRequest request = createLocationRequest();
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, request, this);
        } else {
            needToReStart = true;
        }
    }

    public void stopUpdateLocation(LocationListener mockListener) {
        if (mLocationListener == mockListener) {
            mLocationListener = null;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        mRequestingLocationUpdates = false;
    }

    public boolean isRequestLocationUpdates() {
        return mRequestingLocationUpdates;
    }

    public boolean isConnected() {
        if (mGoogleApiClient != null) {
            return mGoogleApiClient.isConnected();
        }
        return false;
    }

    public void connect() {
        mGoogleApiClient.connect();
    }

    public void disConnect() {
        mGoogleApiClient.disconnect();
    }

    public void getLastLocation(LocationListener listener) throws SecurityException {
        if (mGoogleApiClient.isConnected()) {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            listener.onLocationChanged(location);
        }
    }
}