package com.example.uma_gps;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, AdapterView.OnItemSelectedListener
{
    public static String deviceIpAddress = "";
    String email;
    EditText emailInput;
    LinearLayout layout1;
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
    private static final String PATH_TO_SERVER = "http://provost.uma.edu/api/";


    //RelativeLayout layout1 = (RelativeLayout) findViewById(R.id.button_window); // Declaring this here didn't work
                               // Had to declare it in the onItemSelected method

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

        //if (firstStart)
        //{
            //showStartDialog(); // App wouldn't run if i put this here
        //}

        //getIpAddress(this); // Only works if connected to wifi. Saved the code in notepad
        getIPAddress(); // This works but showing wrong ip address(found out phones can have several)
        //Toast.makeText(getApplicationContext(), deviceIpAddress, Toast.LENGTH_SHORT).show();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //mUrlDisplayTextView = (TextView) findViewById(R.id.tv_url_display);
        getLocationPermission();
        showStartDialog(); // Had to put this down here for the app to run

        emailInput = (EditText) findViewById(R.id.input_email);
    }

    //    @NonNull
    public void getIPAddress() {
        if (TextUtils.isEmpty(deviceIpAddress))
            new PublicIPAddress().execute();
        //return deviceIpAddress;
    }


    public class PublicIPAddress extends AsyncTask<String, Void, String> {
        InetAddress localhost = null;

        protected String doInBackground(String... urls) {
            try {
                localhost = InetAddress.getLocalHost();
                URL url_name = new URL("http://bot.whatismyipaddress.com");
                BufferedReader sc = new BufferedReader(new InputStreamReader(url_name.openStream()));
                deviceIpAddress = sc.readLine().trim();
            } catch (Exception e) {
                deviceIpAddress = "";
            }
            //Toast.makeText(getApplicationContext(), deviceIpAddress, Toast.LENGTH_SHORT).show(); // App wouldn't start
                                                                                  // with this enabled
            return deviceIpAddress;
        }

        protected void onPostExecute(String string) {
            Log.d("deviceIpAddress", string);
            Toast.makeText(getApplicationContext(), deviceIpAddress, Toast.LENGTH_SHORT).show();
        }
    }



    // Got this from YouTube 'Do Something on First App Start Only - Android Studio Tutorial'  9/19/19
    public void showStartDialog()
    {
        layout1 = (LinearLayout) findViewById(R.id.firstAppStart);
        Toast.makeText(getApplicationContext(), "Testing first app start", Toast.LENGTH_SHORT).show();
        TextView test = (TextView) findViewById(R.id.test);
        test.setVisibility(View.VISIBLE);
        layout1.setBackgroundColor(Color.parseColor("#A4D65E"));
        layout1.setVisibility(View.VISIBLE);

        /*
        new AlertDialog.Builder(this)
                .setTitle("One time dialog")
                .setMessage("This should only be shown once")
                .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int i) {
                        dialog.dismiss();
                    }
                })
                .create().show();
       */
        //SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        //SharedPreferences.Editor editor = prefs.edit();
        //editor.putBoolean("firstStart", false);
        //editor.apply();
    }

    public class Reminder
    {
        Timer timer;

        public Reminder(int seconds)
        {
            timer = new Timer();
            timer.schedule(new RemindTask(), seconds * 1000);
        }

        class RemindTask extends TimerTask
        {
            public void run()
            {
                spin1.setSelection(0);
                //System.out.println("Time's up!");
                timer.cancel(); //Terminate the timer thread
            }
        }
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
        Toast.makeText(getApplicationContext(), "Clear Map", Toast.LENGTH_SHORT).show();
        mMap.clear();
        spin1.setSelection(0);
        spin2.setVisibility(View.GONE);
    }

    // This is for the 'send' button in the firstAppStart view
    public void send(View v)
    {
        // Get the address typed in and assign it to the variable 'email'
        email = emailInput.getText().toString();
        Toast.makeText(getApplicationContext(), email, Toast.LENGTH_SHORT).show();// For testing
        layout1.setVisibility(View.GONE);
        // Make send button disappear here
        Button button1 = (Button) findViewById(R.id.send);
        button1.setVisibility(View.GONE);
    }

    private void createListAdapter3(Spinner s)
    {
        s = (Spinner) findViewById(R.id.spinner2); // Declared the spinner globally so I could use it in onItemSelected method
        s.setOnItemSelectedListener(this);
        //Creating the ArrayAdapter instance having the bank name list
        ArrayAdapter bb = new ArrayAdapter(this, android.R.layout.simple_spinner_item, choice);
        bb.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        s.setAdapter(bb);
        s.setVisibility(View.GONE);
    }

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
                Manifest.permission.ACCESS_COARSE_LOCATION};

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

