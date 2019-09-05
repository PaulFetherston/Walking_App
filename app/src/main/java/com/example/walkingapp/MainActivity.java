package com.example.walkingapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.Toast;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;


public class MainActivity  extends AppCompatActivity  implements LocationListener{
    //TAG for this activity
    private static final String TAG = "This is MainActivity" ;
    //Buttons
    Button btnStart, btnEnd;
    //Array List of Locations
    public ArrayList<Location> location_List = new ArrayList<>();
    //Permissions
    public static int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private MainActivity thisActivity;
    private LocationManager lm;
    //Variables for the timer
    private Chronometer chronometer;
    private long pauseOffset;
    private boolean running;
    private boolean tracking;

    /**
     * Initialse all requirments for this activity
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //Initialise the timer
        chronometer = findViewById(R.id.chronometer);
        chronometer.setFormat("Time: %s");
        chronometer.setBase(SystemClock.elapsedRealtime());
        //Initialise the buttons
        btnStart = findViewById(R.id.btnStart);
        btnEnd = findViewById(R.id.btnEnd);
        //Required for permissions
        ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 123);
        //Initialise the LocationManager
        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        thisActivity = this;
        //Check external storage is available
        checkExternalMedia();
        //initialise tracking boolean as false
        tracking = false;

        //Start button listener
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //Check if tracking has begun
                if (!tracking) {
                    //set tracking boolean
                    tracking =true;
                    //Start Timer
                    startChronometer();
                    // add in the location listener and begin tracking locations
                    addLocationListener();
                    Toast.makeText(MainActivity.this, "Tracking has begun", Toast.LENGTH_SHORT).show();
                    Log.e(TAG, "START button ");
                }
                //If tracking has begun notify the user
                else  Toast.makeText(MainActivity.this, "Tracking has begun", Toast.LENGTH_LONG).show();
            }
        });

        //End button Listener
        btnEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //If tracking has begun
                if(tracking) {
                    //Set tracking boolean to false
                    tracking = false;
                    //Stop tracking
                    stopOnClick();
                    //Stop and Reset the timer
                    resetChronometer();
                    //If there is at least two locations recorded write to gpx file and pass
                    //Location array list to the display activity
                    if (location_List.size() > 1) {
                        //Log array list size for testing
                        Log.v(TAG, "END Button location_list size = " + location_List.size());

                        //Call method to write to the gpx file
                        writeToSDFile();

                        //Create an intent to pass the location list to the display activity
                        Intent intent = new Intent(MainActivity.this, DisplayActivity.class);
                        intent.putParcelableArrayListExtra("location_List", location_List);
                        startActivity(intent);
                        finish();
                    }
                    else {
                        //Set tracking boolean to false
                        tracking = false;
                        //Stop tracking
                        stopOnClick();
                        //Stop and Reset the timer
                        resetChronometer();
                        //Remove all Locations from the Location arraylist
                        location_List.clear();
                        Toast.makeText(MainActivity.this, "Not enough Locations Recorded!!! \n Please start again", Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Not enough Locations Recorded!!!");
                    }
                } //If tracking has not begun notify user
                else  Toast.makeText(MainActivity.this, "Please start tracking", Toast.LENGTH_SHORT).show();
            }
        });

    } //END OnCreate

    /**
     * Method to stop tracking locations
     */
    private void stopOnClick() {
        lm.removeUpdates(this);
    }

    /**
     * Method to start the timer to indicate to the user the tracking has begun
     */
    public void startChronometer() {
        if (!running) {
            chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
            chronometer.start();
            running = true;
        }
    }

    /**
     * Method to stop and reset the timer when tracking is ended
     */
    public void resetChronometer() {
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.stop();
        running = false;
        pauseOffset = 0;
    }

    /**
     * Inflate action bar
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate the menu and return true
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Method to add location listener and to begin tracking of the device
     */
    private void addLocationListener() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED) {
            Log.e(TAG,"Permission is not granted");
            // Permission is not granted
            // Should we show an explanation?
            Log.i(TAG, "addLocationListener: Permision to track removed ");
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                // MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }
        else {
            Log.i("Permissions", "Permission have been granted");
            // Permission has already been granted

            //set frequency of location updated
            lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 1,this);
        }

    }//END addLocationListener()

    /**
     * Method to check there is available memory on the device
     */
    private void checkExternalMedia(){
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
        }
        Log.v(TAG,"\n\nExternal Media: readable="
                +mExternalStorageAvailable+" writable="+mExternalStorageWriteable);
    }

    /**
     * Method to create and write to the gpx file
     */
    private void writeToSDFile(){

        // Find the root of the external storage.
        File root = android.os.Environment.getExternalStorageDirectory();
        //Log the root to the external file
        //External file system root: storage/emulated/0/download/trackerApp.gpx
        Log.v(TAG, "\nExternal file system root: "+root);

        File dir = new File (root.getAbsolutePath() + "/download");
        dir.mkdirs();
        File file = new File(dir, "trackerApp.gpx");

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);

            //Header tags needed for the gpx file
            pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            pw.println("<gpx version=\"1.1\" \n" +
                    "creator=\"ViewRanger/8.4.10 http://www.viewranger.com\"\n" +
                    "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
                    "xmlns=\"http://www.topografix.com/GPX/1/1\"\n" +
                    "xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\"\n" +
                    "xmlns:viewranger=\"http://www.viewranger.com/xmlschemas/GpxExtensions/v2\"\n" +
                    "xmlns:gpx_style=\"http://www.topografix.com/GPX/gpx_style/0/2\"\n" +
                    "xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.topografix.com/GPX/gpx_style/0/2\n" +
                    "http://www.topografix.com/GPX/gpx_style/0/2/gpx_style.xsd http://www.viewranger.com/xmlschemas/GpxExtensions/v2 http://www.viewranger.com/xmlschemas/GpxExtensions/v2/GpxExtensionsV2.xsd\">");

            pw.println("<trk>\n" +
                    "  <name><![CDATA[Paul Fetherston 2898842 - Drive to Work]]></name>\n" +
                    "<trkseg>");

            //for Loop to write each locations latitude, longitude, altitude and the time and date of each location
            for(Location l: location_List){
                //Format the time and date
                String date = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(l.getTime());
                pw.println("<trkpt lat=\"" +l.getLatitude()+"\" lon=\"" +l.getLongitude() + "\"> <ele>" +l.getAltitude() +"</ele><time>" +date+ "</time> </trkpt>");
            }

            //Closing tags for the gpx file
            pw.println(" </trkseg>\n" +
                    "</trk>\n" +
                    "</gpx>");

            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.i(TAG, "******* File not found. Did you" +
                    " add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Log confirmation of the file name gpx
        Log.v(TAG, "\n\nGPX File written to "+file);
    }

    /**
     * Method to continually record Locations
     * @param location Location passed in
     */
    @Override
    public void onLocationChanged(Location location) {
        // the location of the device has changed so update the

        if(tracking){
            location_List.add(location);
        }
        Log.e(TAG,"onLocationChange Array size ===== " +location_List.size());
    }

    /**
     * Method to log a message if the GPS provider is removed
     * @param provider provider
     */
    @Override
    public void onProviderDisabled(String provider) {
        // if GPS has been disabled log it
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            Log.v(TAG,"Gps Has been disabled ---------------");
        }
    }

    /**
     *Method to get the last know address when gps provider is enabled
     * @param provider procider
     */
    @Override
    public void onProviderEnabled(String provider) {
        // if there is a last known location
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            Location l;
            if (ActivityCompat.checkSelfPermission(thisActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(thisActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            l = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(l != null) {
                //add location to array list
                location_List.add(l);
                Log.e(TAG,"onProviderEnabled Array size ===== " +location_List.size());

            }
        }
    }

    /**
     * Method for when the status changes
     * @param provider provide
     * @param status status
     * @param extras extras
     */
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

}
