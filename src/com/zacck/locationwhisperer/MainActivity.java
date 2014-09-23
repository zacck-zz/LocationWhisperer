package com.zacck.locationwhisperer;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.internal.hy;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBarActivity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements
		GooglePlayServicesClient.ConnectionCallbacks,
		GooglePlayServicesClient.OnConnectionFailedListener,
		com.google.android.gms.location.LocationListener {
	// vars
	// this code we use to relate with google play services
	private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
	LocationClient mLocationClient;
	Location mLocation;
	LocationRequest mLocationRequest;
	boolean mUpdatesRequested;

	// sharedprefs for getting updates
	SharedPreferences mPrefs;
	SharedPreferences.Editor mEditor;

	// Milliseconds per second
	private static final int MILLISECONDS_PER_SECOND = 1000;
	// Update frequency in seconds
	public static final int UPDATE_INTERVAL_IN_SECONDS = 5;
	// Update frequency in milliseconds
	private static final long UPDATE_INTERVAL = MILLISECONDS_PER_SECOND
			* UPDATE_INTERVAL_IN_SECONDS;
	// The fastest update frequency, in seconds
	private static final int FASTEST_INTERVAL_IN_SECONDS = 1;
	// A fast frequency ceiling in milliseconds
	private static final long FASTEST_INTERVAL = MILLISECONDS_PER_SECOND
			* FASTEST_INTERVAL_IN_SECONDS;

	// lets add map stuff
	GoogleMap mGoogleMap;

	// Define a DialogFragment that displays the error dialog in case of one
	public static class ErrorDialogFragment extends DialogFragment {
		private Dialog mDialog;

		public ErrorDialogFragment() {
			super();
			mDialog = null;
		}

		public void setDialog(Dialog dialog) {
			mDialog = dialog;
		}

		@Override
		public Dialog onCreateDialog(Bundle savedInstanceState) {
			return mDialog;
		}
	}

	// call on activity result to check if we are connecting to google play
	// services
	@Override
	protected void onActivityResult(int reqcode, int rescode, Intent dataHolder) {
		super.onActivityResult(reqcode, rescode, dataHolder);
		switch (reqcode) {
		// did we init this request ?
		case CONNECTION_FAILURE_RESOLUTION_REQUEST:
			// checkresult code
			switch (rescode) {
			case Activity.RESULT_OK:
				// request again
				break;
			}
			break;

		}
	}

	// make a var to check if we good
	private boolean servicesConnected() {
		// Check that Google Play services is available
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		// If Google Play services is available
		if (ConnectionResult.SUCCESS == resultCode) {
			// In debug mode, log the status
			Log.d("Location Updates", "Google Play services is available.");
			// Continue
			return true;
			// Google Play services was not available for some reason.
			// resultCode holds the error code.
		} else {
			// Get the error dialog from Google Play services
			Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
					resultCode, this, CONNECTION_FAILURE_RESOLUTION_REQUEST);

			// If Google Play services can provide an error dialog
			if (errorDialog != null) {
				// Create a new DialogFragment for the error dialog
				ErrorDialogFragment errorFragment = new ErrorDialogFragment();
				// Set the dialog in the DialogFragment
				errorFragment.setDialog(errorDialog);
				// Show the error dialog in the DialogFragment
				errorFragment.show(getSupportFragmentManager(),
						"Location Updates");
			}
			return false;
		}

	}

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		// wohoo let the fun begin
		mLocationClient = new LocationClient(this, this, this);

		// Create the LocationRequest object
		mLocationRequest = LocationRequest.create();
		// Use high accuracy
		mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		// Set the update interval to 5 seconds
		mLocationRequest.setInterval(UPDATE_INTERVAL);
		// Set the fastest update interval to 1 second
		mLocationRequest.setFastestInterval(FASTEST_INTERVAL);

		// vodoo
		// Open the shared preferences
		mPrefs = getSharedPreferences("SharedPreferences", Context.MODE_PRIVATE);
		// Get a SharedPreferences editor
		mEditor = mPrefs.edit();
		/*
		 * Create a new location client, using the enclosing class to handle
		 * callbacks.
		 */
		mLocationClient = new LocationClient(this, this, this);
		// Start with updates turned off
		mUpdatesRequested = false;

		// connect our map to the ui
		// Get a handle to the Map Fragment
		mGoogleMap = ((MapFragment) getFragmentManager().findFragmentById(
				R.id.map)).getMap();

	}

	@Override
	protected void onPause() {
		// Save the current setting for updates
		mEditor.putBoolean("KEY_UPDATES_ON", mUpdatesRequested);
		mEditor.commit();

		super.onPause();
	}

	@Override
	protected void onStart() {
		// when the activity actually starts lets connect
		mLocationClient.connect();
		super.onStart();
	}

	@Override
	protected void onResume() {
		if (mPrefs.contains("KEY_UPDATES_ON")) {
			mUpdatesRequested = mPrefs.getBoolean("KEY_UPDATES_ON", false);

			// Otherwise, turn off location updates
		} else {
			mEditor.putBoolean("KEY_UPDATES_ON", false);
			mEditor.commit();
		}
		super.onResume();
	}

	// implement onstop to kill connection client when no longer needed ie when
	// activity is dead
	@Override
	protected void onStop() {
		// disconnect the connection client to save battery
		if (mLocationClient.isConnected()) {
			mLocationClient.removeLocationUpdates(this);
		}
		mLocationClient.disconnect();
		super.onStop();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
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

	// these are the connection callbacks

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		if (connectionResult.hasResolution()) {
			try {
				// Start an Activity that tries to resolve the error
				connectionResult.startResolutionForResult(this,
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
			/*
			 * If no resolution is available, display a dialog to the user with
			 * the error.
			 */
			Toast.makeText(MainActivity.this, connectionResult.getErrorCode(),
					Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public void onConnected(Bundle arg0) {
		Toast.makeText(this, "Connected to google Play!!! let the games begin",
				Toast.LENGTH_SHORT).show();
		// lets toast a location so them geezers know we are serious
		mLocation = mLocationClient.getLastLocation();
		// Toast.makeText(MainActivity.this, mLocation.toString(),
		// Toast.LENGTH_LONG).show();
		LatLng pos = new LatLng(mLocation.getLatitude(),mLocation.getLongitude());
		mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 13));

		mGoogleMap.addMarker(new MarkerOptions().title("Current Location")
				.snippet("Please stay stationary as we aim missile").position(pos));
		// if the user wants updates lets give them
		if (mUpdatesRequested) {
			mLocationClient.requestLocationUpdates(mLocationRequest, this);
		}

	}

	@Override
	public void onDisconnected() {
		Toast.makeText(
				this,
				"Disconnected. Please re-connect.\n Check your internet did you enable location also \n if you are indoors try walking outside and looking at the sun ",
				Toast.LENGTH_SHORT).show();

	}

	// lets use this to get when the location is changed
	@Override
	public void onLocationChanged(Location loc) {

		String msg = "Updated Location: " + Double.toString(loc.getLatitude())
				+ "," + Double.toString(loc.getLongitude());
		Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
		LatLng pos = new LatLng(loc.getLatitude(),loc.getLongitude());
		mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 13));

		mGoogleMap.addMarker(new MarkerOptions().title("Current Location")
				.snippet("Please stay stationary as we aim missile").position(pos));
		// if the user wants updates lets give them
		if (mUpdatesRequested) {
			mLocationClient.requestLocationUpdates(mLocationRequest, this);
		}

	}
}
