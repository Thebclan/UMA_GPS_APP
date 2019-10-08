package com.example.uma_gps;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, AdapterView.OnItemSelectedListener
{
    String token = "";
    String email;
    String code;
    EditText emailInput;
    EditText codeInput;
    LinearLayout layout1; // Enter/send email. First screen on first app start.
    LinearLayout layout2; // Enter/submit code. Second screen on first app start.
    Spinner spin1;
    Spinner spin2;
    private static final String TAG = "mapsActivity";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final Object FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private GoogleMap mMap;
    private UiSettings mUiSettings; //
    //protected TextView mUrlDisplayTextView;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private static final float DEFAULT_ZOOM = 16f;
    private Boolean mLocationPermissionsGranted = false;
    ArrayList<Double[]> coordArray = new ArrayList<Double[]>();
    ArrayList<Double[]> coordArrayMain = new ArrayList<Double[]>();
    ArrayList<Double[]> coordArrayRandall = new ArrayList<Double[]>();
    ArrayList<String> Randall_Student_Center = new ArrayList<String>();
    ArrayList<String> nameArray = new ArrayList<String>();
    ArrayList<String> jewettHall = new ArrayList<String>();
    ArrayList<String> choice = new ArrayList<String>();
    private static final String PATH_TO_SERVER = "https://provost.uma.edu/api/"; // Used in the background to retrieve the location data
    String ServerURL = "https://provost.uma.edu/api/login.php"; // To send email address to api
    String ValidationURL = "https://provost.uma.edu/api/validate.php"; // To send code to api

    private Geofence myGeofence = null;
    private LocationServices mLocationService;
    public static final String CUSTOM_BROADCAST_ACTION = "com.example.uma_gps.CUSTOM_BROADCAST";

    private GoogleApiClient mApiClient;
    private GeofencingClient myGeofencingClient;
    private GeofencingRequest myRequest;
    private  PendingIntent geofencePendingIntent;  // Stores the PendingIntent used to request geofence monitoring.
    // Used this to test onReceive method in GeofenceBroadcastReceiver class
    protected static int received = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        jewettHall.add("Room_101");
        jewettHall.add("Room_102");
        jewettHall.add("Room_103");

        DownloadFilesTask downloadFilesTask = new DownloadFilesTask();
        downloadFilesTask.execute();

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        boolean firstStart = prefs.getBoolean("firstStart", true);
        token = prefs.getString("token", "");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (!isGooglePlayServicesAvailable()) {
            Log.e(TAG, "Google Play services unavailable.");
            finish();
            return;
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        getLocationPermission();

        layout2 = (LinearLayout) findViewById(R.id.enter_code);
        layout2.setBackgroundColor(Color.parseColor("#A4D65E"));
        layout2.setVisibility(View.GONE);

        showStartDialog(); // Had to put this down here for the app to run

        emailInput = (EditText) findViewById(R.id.input_email);
        codeInput = (EditText) findViewById(R.id.input_code);

        createGeofence();

        myGeofencingClient = LocationServices.getGeofencingClient(this);

        addFences();
    }

    private void createGeofence()
    {
        myGeofence = new Geofence.Builder()
        .setRequestId("UMA_Augusta").setCircularRegion(44.340852, -69.797395,300)
                .setExpirationDuration(myGeofence.NEVER_EXPIRE)
                .setTransitionTypes( myGeofence.GEOFENCE_TRANSITION_ENTER | myGeofence.GEOFENCE_TRANSITION_EXIT )
                .build();
    }

    private GeofencingRequest getGeofencingRequest() {
        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER);
        builder.addGeofence(myGeofence);
        if (myGeofence != null) {
            Log.i(TAG, "This is the getGeofencingRequest method");
        }
        return builder.build();
    }

    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it.
        Log.i(TAG,"This is the 1st getGeofencePendingIntent method");
        if (geofencePendingIntent != null) {
            Log.i(TAG,"This is the 2nd getGeofencePendingIntent method");
            return geofencePendingIntent;
        }
        Intent intent = new Intent(this, GeofenceBroadcastReceiver.class);
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when
        // calling addGeofences() and removeGeofences().
        geofencePendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
        Log.i(TAG,"This is the 3rd getGeofencePendingIntent method");
        return geofencePendingIntent;
    }

    private void addFences()
    {
        myGeofencingClient.addGeofences(getGeofencingRequest(), getGeofencePendingIntent())
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Geofences added
                        // ...
                        // This toast is working  10-3-19 9:13 AM
                        Toast.makeText(getApplicationContext(), "Geofence successfully added!", Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to add geofences
                        // ...
                    }
                });
    }

    // Checks if Google Play services is available.
    // @return true if it is.
    private boolean isGooglePlayServicesAvailable() {
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (ConnectionResult.SUCCESS == resultCode) {
            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Google Play services is available.");
            }
            return true;
        } else {
            Log.e(TAG, "Google Play services is unavailable.");
            return false;
        }
    }

    // Got this from YouTube 'Do Something on First App Start Only - Android Studio Tutorial'  9/19/19
    public void showStartDialog()
    {
        //layout2 = (LinearLayout) findViewById(R.id.enter_code);
        //layout2.setVisibility(View.GONE);
        layout1 = (LinearLayout) findViewById(R.id.firstAppStart);
        //Toast.makeText(getApplicationContext(), "Testing first app start", Toast.LENGTH_SHORT).show();
        TextView test = (TextView) findViewById(R.id.test);
        test.setVisibility(View.GONE);
        layout1.setBackgroundColor(Color.parseColor("#A4D65E"));
        layout1.setVisibility(View.GONE);

        //SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        //SharedPreferences.Editor editor = prefs.edit();
        //editor.putBoolean("firstStart", false);
        //editor.apply();
    }

    private class DownloadFilesTask extends AsyncTask<URL, Void, List<String[]>>
    {
        protected List<String[]> doInBackground(URL... urls)
        {
            return downloadRemoteTextFileContent();
        }

        protected void onPostExecute(List<String[]> result)
        {
            if (result != null)
            {
                convertCsvFile(result);
            }
        }
    }

    private List<String[]> downloadRemoteTextFileContent()
    {
        URL mUrl = null;
        List<String[]> csvLine = new ArrayList<>();
        String[] content = null;
        try {
            mUrl = new URL(PATH_TO_SERVER);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        try {
            assert mUrl != null;
            URLConnection connection = mUrl.openConnection();
            BufferedReader br = new BufferedReader(new
                    InputStreamReader(connection.getInputStream()));
            String line = "";
            while ((line = br.readLine()) != null) {
                content = line.split(",");
                csvLine.add(content);
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return csvLine;
    }

    public void clearMarkers(View v)
    {
        // Used this to test onReceive method in GeofenceBroadcastReceiver class
        //Toast.makeText(getApplicationContext(), received, Toast.LENGTH_SHORT).show();

        mMap.clear();
        spin1.setSelection(0);
        spin2.setVisibility(View.GONE);
    }

    // This is for the 'send' button in the firstAppStart view
    public void sendAddress(View v)
    {
        // Get the address typed in and assign it to the variable 'email'
        email = emailInput.getText().toString();
        Toast.makeText(getApplicationContext(), email, Toast.LENGTH_SHORT).show();// For testing
        layout1.setVisibility(View.GONE);

        TextView code_text = (TextView) findViewById(R.id.code_info);
        code_text.setVisibility(View.GONE);
        layout2.setVisibility(View.GONE);

        InsertData(email, "code", ServerURL);

        // Make 'send' button disappear here
        //Button button1 = (Button) findViewById(R.id.send_email);
        //button1.setVisibility(View.GONE);
    }

    public void sendCode(View v)
    {
        code = codeInput.getText().toString();
        Toast.makeText(getApplicationContext(), "Testing send_code button", Toast.LENGTH_SHORT).show();

        InsertData(email, code, ValidationURL);
    }

    public void InsertData(final String email, final String code, final String url)
    {
        class SendPostReqAsyncTask extends AsyncTask<String, Void, String>
        {
            @Override
            protected String doInBackground(String... params)
            {
                String mEmail = email;
                String mCode = code;
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
                nameValuePairs.add(new BasicNameValuePair("email", mEmail));
                nameValuePairs.add(new BasicNameValuePair("validation_code", mCode)); // Will use this when sending code

                try {
                    HttpClient httpClient = new DefaultHttpClient();

                    HttpPost httpPost = new HttpPost(url);

                    httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse httpResponse;
                    httpResponse = httpClient.execute(httpPost);

                    HttpEntity httpEntity = httpResponse.getEntity(); // Gets the token

                    if (url.equals(ValidationURL)) { // 'if' statement makes sure the token is assigned to the variable after the second 'send' button is pressed
                        token = EntityUtils.toString(httpEntity); // Converts token to a string and assigns it to a variable
                        //httpPost.addHeader("Authorization", "Bearer" + token); // Can't use this here. I need to put it in the method
                                                  // that sends the data in the background
                    }


                } catch (ClientProtocolException e) {

                } catch (IOException e) {

                }
                return "Data Inserted Successfully";
            } // End doInBackground
            @Override
            protected void onPostExecute(String result) {

                super.onPostExecute(result);

                if (url.equals(ValidationURL)) {// 'if' statements make sure the second layout disappears only after the second button is pressed and only
                                                // after the token is created
                    if (!token.equals("")) {

                        layout2.setVisibility(View.GONE);

                        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString("token", token);
                        editor.commit();
                    }
                }


                Toast.makeText(getApplicationContext(), "Data Submit Successfully", Toast.LENGTH_LONG).show();
                Toast.makeText(getApplicationContext(), token, Toast.LENGTH_LONG).show(); // This is showing up as an empty toast
                                   // after pressing the sendAddress button

            } // End onPostExecute
        } // End class

        SendPostReqAsyncTask sendPostReqAsyncTask = new SendPostReqAsyncTask();

        sendPostReqAsyncTask.execute(email, code);

    } // End InsertData method

    private void createArrayAdapter2(ArrayList<String> choice)
    {
        spin2 = (Spinner) findViewById(R.id.spinner2); // Declared the spinner globally so I could use it in onItemSelected method
        spin2.setOnItemSelectedListener(this);
        //Creating the ArrayAdapter instance
        ArrayAdapter bb = new ArrayAdapter(this, android.R.layout.simple_spinner_item, choice);
        bb.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        spin2.setAdapter(bb);
        spin2.setVisibility(View.GONE);
    }

    private void createArrayAdapter(ArrayList<String> name)
    {
        spin1 = (Spinner) findViewById(R.id.spinner1); // Declared the spinner globally so I could use it in onItemSelected method
        spin1.setOnItemSelectedListener(this);
        //Creating the ArrayAdapter instance having the location list
        ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, name);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        spin1.setAdapter(aa);
    }

    @Override
    public void onItemSelected(AdapterView<?> arg0, View arg1, int position, long id)
    {
        LatLng loca;
        if (arg0.getId() == R.id.spinner1)
        {
            if (position > 0) // This is so no marker is shown until something in the list is selected
            {
                //spin2.setVisibility(View.GONE);

                //Toast.makeText(getApplicationContext(), "Touch marker for more info", Toast.LENGTH_SHORT).show();
                if (position == 3)
                {
                    choice = Randall_Student_Center;
                    coordArray = coordArrayRandall;
                    createArrayAdapter2(choice);
                    spin2.setVisibility(View.VISIBLE);
                }

                spin2.setSelection(0);

                // Should I make an 'addMarker' method?
                loca = new LatLng(coordArrayMain.get(position)[0], coordArrayMain.get(position)[1]);
                mMap.addMarker(new MarkerOptions().position(loca).title(nameArray.get(position)));

                //if (position > 0)
                moveCamera(loca, 17f);

                Toast.makeText(getApplicationContext(), nameArray.get(position), Toast.LENGTH_SHORT).show();
                //if (position == 5)
                //{
                //spin2.setVisibility(View.VISIBLE);
            }
        }
        if (arg0.getId() == R.id.spinner2)
        {
            if (position > 0) // This is so no marker is shown until something in the list is selected
            {
                //spin2.setSelection(0);

                Toast.makeText(getApplicationContext(), "Touch marker for more info", Toast.LENGTH_SHORT).show();

                String moreInfo = "";

                if (position == 2)
                {
                    moreInfo = "2nd floor";
                }

                loca = new LatLng(coordArray.get(position)[0], coordArray.get(position)[1]);
                mMap.addMarker(new MarkerOptions().position(loca).title(choice.get(position)).snippet(moreInfo));

                //if (position > 0)
                moveCamera(loca, 19f);

                //spin2.setVisibility(View.GONE); // This hides the spinner.
                //spin1.setSelection(0);
                if (position == 1) // Did this to test visibility
                {
                    //mMap.clear();
                    //Toast.makeText(getApplicationContext(), Randall_Student_Center.get(position), Toast.LENGTH_SHORT).show();
                    spin2.setVisibility(View.GONE); // This hides the spinner.
                    spin1.setSelection(0);

                    //LinearLayout layout1 = (LinearLayout) findViewById(R.id.firstAppStart);
                    //TextView test = (TextView) findViewById(R.id.test);
                    //test.setVisibility(View.VISIBLE);

                    //RelativeLayout layout1 = (RelativeLayout) findViewById(R.id.button_window);
                    //layout1.setVisibility(View.INVISIBLE); // This works
                    //layout1.setBackgroundColor(Color.WHITE);
                    //layout1.setVisibility(View.VISIBLE);
                    // This works
                    //new Reminder(2); // Don't need this
                    //new Handler().postDelayed(new Runnable() { // This creates a delay
                    //@Override
                    //public void run()
                    //{
                    //spin1.setSelection(0);
                    //}
                    //}, 3000);
                }
            }
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> arg0)
    {
        // TODO Auto-generated method stub
    }

    private void convertCsvFile(List<String[]> result)
    {
        for (int i = 0; i < 7; i++) // result.size()
        {
            String[] rows = result.get(i);
            nameArray.add(rows[0]);

            Double[] row = new Double[2];
            row[0] = Double.parseDouble(result.get(i)[1]);
            row[1] = Double.parseDouble(result.get(i)[2]);
            coordArrayMain.add(row);
        }

        for (int i = 7; i < 11; i++)
        {
            String[] rows = result.get(i);
            Randall_Student_Center.add(rows[0]);

            Double[] row = new Double[2];
            row[0] = Double.parseDouble(result.get(i)[1]);
            row[1] = Double.parseDouble(result.get(i)[2]);
            coordArrayRandall.add(row);
        }

        createArrayAdapter(nameArray);
        createArrayAdapter2(jewettHall); // Had to do this so array wouldn't be empty when it gets populated in
                                         // the onItemSelected method
    }

    private void getLocationPermission()
    {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                Manifest.permission.RECEIVE_BOOT_COMPLETED,Manifest.permission.ACCESS_WIFI_STATE};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                (String) FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void getDeviceLocation()
    {
        Log.d(TAG, "getDeviceLocation: getting the device's current location");

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        try {
            if (mLocationPermissionsGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();

                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), DEFAULT_ZOOM);

                            // I used the following lines to show a toast with my current location
                            //Double lat = currentLocation.getLatitude();
                            //Double longi = currentLocation.getLongitude();
                            //Toast.makeText(getApplicationContext(), String.valueOf(lat), Toast.LENGTH_LONG).show();
                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        mMap = googleMap;
        mUiSettings = mMap.getUiSettings(); //

        if (mLocationPermissionsGranted)
        {
            getDeviceLocation();
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false); // This disables the 'center my location' button that is
            // in the upper right hand corner by default
        }
        mUiSettings.setZoomControlsEnabled(true);

        // This creates a circle around my office
        LatLng middle = new LatLng(44.340852, -69.797395);
        CircleOptions circleOptions = new CircleOptions()
                .center(middle)
                .strokeColor(Color.argb(50, 70,70,70))
                .fillColor( Color.argb(50, 150,150,150) )
                .radius( 300 );
                mMap.addCircle(circleOptions);
    }

    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(this);
    }

    private void moveCamera(LatLng latLng, float zoom)
    {
        Log.d(TAG, "move camera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }
}

