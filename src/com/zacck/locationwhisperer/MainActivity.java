package com.zacck.locationwhisperer;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
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
	
	//add some textviews to display data
	TextView tvacc,tvalt,tvdire,tvspeed;
	public TextView tvaddr;
	
	
	
	//this is a shared preference module for storing the locations
	SharedPreferences LocaList;
	Set<String> prevLocs;

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
		mUpdatesRequested = true;
		
		//for storing locations 
		LocaList = getPreferences(MODE_PRIVATE);
		//vodoo hack for storing locations in shared prefrence as a list 
		prevLocs = LocaList.getStringSet("locs", null);
		if(prevLocs == null)
		{
			prevLocs = new HashSet<String>();
		}
		
;
		// connect our map to the ui
		// Get a handle to the Map Fragment
		mGoogleMap = ((MapFragment) getFragmentManager().findFragmentById(
				R.id.map)).getMap();
		tvacc = (TextView)findViewById(R.id.tvAccu);
		tvaddr = (TextView)findViewById(R.id.tvaddr);
		tvalt = (TextView)findViewById(R.id.tvAlt);
		tvdire = (TextView)findViewById(R.id.tvBea);
		tvspeed = (TextView)findViewById(R.id.tvSpe);

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

	@SuppressLint("NewApi") @Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//save and display the locat
		switch(item.getItemId())
		{
		case R.id.action_settings:
			//list of locs
			if(prevLocs.isEmpty())
			{
				Toast.makeText(MainActivity.this, "no saved locations yet", Toast.LENGTH_LONG).show();
			}
			else
			{
			String[] locations = prevLocs.toArray(new String[prevLocs.size()]);	
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
		    builder.setTitle("Previous Missile targets")
		           .setItems(locations, new DialogInterface.OnClickListener() {
		               public void onClick(DialogInterface dialog, int which) {
		               // The 'which' argument contains the index position
		               // of the selected item
		           }
		    });
		    builder.create();
		    builder.show();
			}
			break;
		case R.id.action_save_location:
			//put a location in a set then save it 
			SharedPreferences.Editor locsed = LocaList.edit();
			if(mLocation !=null)
			{
				prevLocs.add(mLocation.toString());
				locsed.putStringSet("locs", prevLocs);	
				locsed.commit();
				Toast.makeText(MainActivity.this, "Current location has been stored", Toast.LENGTH_LONG).show();
			}
			else
			{
				Toast.makeText(MainActivity.this, "Location hasnt been set yet try later", Toast.LENGTH_LONG).show();
			}
			break;
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
		
		//populate the info texts
		GetAddress ga = new GetAddress(MainActivity.this);
		ga.execute(mLocation);
		tvacc.setText("Accurate to "+mLocation.getAccuracy()+" metres");
		tvalt.setText("Altitude of "+mLocation.getAltitude()+" metres");
		tvdire.setText("heading in a "+mLocation.getBearing()+" direction");
		tvspeed.setText(mLocation.getSpeed()+" metres/second");
		LatLng pos = new LatLng(mLocation.getLatitude(),
				mLocation.getLongitude());
		mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 13));

		mGoogleMap.addMarker(new MarkerOptions().title("Current Location")
				.snippet("Please stay stationary as we aim missile")
				.position(pos));
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
		LatLng pos = new LatLng(loc.getLatitude(), loc.getLongitude());
		//populate the info texts
				GetAddress ga = new GetAddress(MainActivity.this);
				ga.execute(loc);
				tvacc.setText("Accurate to "+loc.getAccuracy()+" metres");
				tvalt.setText("Altitude of "+loc.getAltitude()+" metres");
				tvdire.setText("heading in a "+loc.getBearing()+" direction");
				tvspeed.setText(loc.getSpeed()+" metres/second");
		mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 13));

		mGoogleMap.addMarker(new MarkerOptions().title("Current Location")
				.snippet("Please stay stationary as we aim missile")
				.position(pos));
		// if the user wants updates lets give them
		if (mUpdatesRequested) {
			mLocationClient.requestLocationUpdates(mLocationRequest, this);
		}

	}
	
	public class GetAddress extends AsyncTask<Location, Void, String> {
		Context mlocContext;

		public GetAddress(Context ctx) {
			super();
			mlocContext = ctx;
		}
		
		@Override
		protected void onPostExecute(String result) {
			tvaddr.setText(result);
		}

		@Override
		protected String doInBackground(Location... params) {
			Geocoder geocoder = new Geocoder(mlocContext, Locale.getDefault());
			// Get the current location from the input parameter list
			Location loc = params[0];
			// Create a list to contain the result address
			List<Address> addresses = null;
			try {
				/*
				 * Return 1 address.
				 */
				addresses = geocoder.getFromLocation(loc.getLatitude(),
						loc.getLongitude(), 1);
			} catch (IOException e1) {
				Log.e("LocationSampleActivity", "IO Exception in getFromLocation() "+e1.toString());
				
				return ("Address "+e1.toString());
			} catch (IllegalArgumentException e2) {
				// Error message to post in the log
				String errorString = "Illegal arguments "
						+ Double.toString(loc.getLatitude()) + " , "
						+ Double.toString(loc.getLongitude())
						+ " passed to address service";
				Log.e("LocationSampleActivity", errorString);
				e2.printStackTrace();
				return errorString;
			}
			// If the reverse geocode returned an address
			if (addresses != null && addresses.size() > 0) {
				// Get the first address
				Address address = addresses.get(0);
				/*
				 * Format the first line of address (if available), city, and
				 * country name.
				 */
				String addressText = String.format(
						"%s, %s, %s",
						// If there's a street address, add it
						address.getMaxAddressLineIndex() > 0 ? address
								.getAddressLine(0) : "",
						// Locality is usually a city
						address.getLocality(),
						// The country of the address
						address.getCountryName());
				// Return the text
				return addressText;
			} else {
				return "No address found";
			}
		}
	}


}
