package com.example.walkingapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

public class DisplayActivity extends AppCompatActivity {
    //Array List to store the Locations received from the main activity
    private ArrayList<Location> location_List = new ArrayList<>();
    TextView tv_time, tv_distance, tv_maxAlt, tv_minAlt, tv_gainedAlt, tv_lostAlt, tv_maxSpeed, tv_minSpeed, tv_avgSpeed;
    //double array to store the distance between each location
    double[] dist;
    //double array to store the time between each point
    double[] time;
    //float array to store the altitudes of each location
    float[] alt;

    /**
     * Initialise all TextViews, receive the arraylist and add to this array list
     * initialist the distance array list to be 1 smaller than the location list
     * Call methods to perform calculations on the datat to populat the text views
     * @param savedInstanceState default
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // call the super class method and set the content for this activity
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_activity);

        tv_time = findViewById(R.id.totalTime);
        tv_distance = findViewById(R.id.totalDistance);
        tv_maxAlt = findViewById(R.id.alt_max);
        tv_minAlt = findViewById(R.id.alt_min);
        tv_gainedAlt = findViewById(R.id.alt_gain);
        tv_lostAlt = findViewById(R.id.alt_loss);
        tv_maxSpeed = findViewById(R.id.speed_max);
        tv_minSpeed = findViewById(R.id.speed_min);
        tv_avgSpeed = findViewById(R.id.speed_average);

        //Get the array list from the intent
        Intent intent = getIntent();
        location_List.addAll((Collection) intent.getSerializableExtra("location_List"));
        //Log the total number of Locations received from the main activity
        Log.v("DisplayActivity == "," ++++++++++++++++ The number of received locations is: " +location_List.size());

        //initialise the distance array list to be 1 element less than the Location list
        dist = new double[location_List.size() - 1];

        //If location list is null make the user aware
        if(location_List == null){
            Toast.makeText(getApplicationContext(),"location_List Empty",Toast.LENGTH_SHORT).show();
        }
        else{
            //Call method to calculate total time duration of activity
            activityDuration(location_List.get(0).getTime(), location_List.get(location_List.size()-1).getTime());

            //Call method to calculate total distance traveled and display it
            activityDistance();

            //Call method to calculate all altitude results and display them for user
            getAltitudeResults();

            //Call method to calculate the max, min and avg speed
            calculateSpeed();

            for(Location l: location_List){
                String time = new java.text.SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(l.getTime());
                Log.v("DisplayActivity == "," 888888888888888  latitude: [" +l.getLatitude()+ "], longitude: [" +l.getLongitude() + "], Altitude: [" +l.getAltitude()+ "], Date:[ " +time+ " ]");
            }

        }

        //**Extra Feature**
        //Access to the CustomView to display the graph
        final CustomView lineChart = findViewById(R.id.graph);
        lineChart.setChartData(getAltitude());
        //Button listener to display the distance between locations
        findViewById(R.id.distance_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lineChart.setChartData(getDistance());
            }
        });
        //Button listener to display the altitudes on the graph
        findViewById(R.id.alt_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lineChart.setChartData(getAltitude());
            }
        });


    }//END onCreate();

    /**
     * Method to loop through the location arrayList and calculate the distance between each point
     * Save each distance to an array to calculate the speeds.
     * Add all distances together and display the total distance covered.
     */
    public void activityDistance(){
        //variables needed for calculations
        int count = 1;
        double totalDistance = 0.0;

        //array to store distances between locations
        int dist_count = 0;

        //loop through each element of the array list
        for(Location lm: location_List){
            //if condition to stop calculating distances after calculating the
            // distance between the 2nd last and the last Location recorded in the array list
            if(count<location_List.size()) {
                //get the current Locations lat and long values
                double firstLat = lm.getLatitude();
                double firstLon = lm.getLongitude();
                //get the next Locations lat and log values in the array list
                double nextLat = location_List.get(count).getLatitude();
                double nextLon = location_List.get(count).getLongitude();
                //add the distance (meter) between each recorded location to an array for calculating the speeds
                dist[dist_count] =  CalculationByDistance(firstLat, firstLon, nextLat, nextLon);
                //add the distances between each Location
                totalDistance += dist[dist_count];
            }
            //Increase count to get the next lat and long
            count++;
            dist_count++;
        }//END for loop

        //Display total distance covered and round the total calculated distance to two decimal places
        tv_distance.setText(String.format("Total Distance = %smeters", (double) Math.round(totalDistance * 100) / 100));
        //Log total distance un-formatted for testing purposes
        Log.v("activityDistance() == "," The total distance calculated is = " +totalDistance+ "meters");

    }//END activityDistance()

    /**
     * Method to calculate the total distance between two locations using the Haversine formula
     * @param initialLat first location's Latitude
     * @param initialLong first location's Longitude
     * @param finalLat second location's Latitude
     * @param finalLong second location's Longitude
     * @return return the distance in Km between the two locations as a double
     */
    public double CalculationByDistance(double initialLat, double initialLong,
                                        double finalLat, double finalLong){
        double R = 6371000; // m (Earth radius)
        double dLat = toRadians(finalLat-initialLat);
        double dLon = toRadians(finalLong-initialLong);
        initialLat = toRadians(initialLat);
        finalLat = toRadians(finalLat);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(initialLat) * Math.cos(finalLat);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c; //distance in meters
    }//End CalculationByDistance()

    /**
     * method that receives a double distance from the CalculateByDistance method
     * and returns the Radian value of that distance
     * @param deg a distance between two latitudes or longitudes
     * @return returns a Radian value as a double
     */
    public double toRadians( double deg) {
        return deg * (Math.PI/180);
    }//END toRadians

    /**
     * Method to calculate and display the MAX, MIN, GAIN and LOST altitudes
     */
    public void getAltitudeResults(){
        //set max and min as the first altitude recorded
        double max = location_List.get(0).getAltitude();
        double min = location_List.get(0).getAltitude();
        double gained = 0.0;
        double lost = 0.0;
        //count variable to get access to the next Location while looping through the Location list
        int count = 1;
        //Initialise altitude array
        alt = new float[location_List.size()];

        //loop through each location
        for(Location lm: location_List){

            //set each Location altitude in the altitude array
            alt[count-1] = (float) lm.getAltitude();

            if(count<location_List.size()){
                //if statement to check if altitude is gained between the current location
                //and the next location in the Location array
                if(location_List.get(count).getAltitude() > lm.getAltitude()){
                    //if the next altitude is greater than the current max altitude. Then set it as the max altitude
                    if(location_List.get(count).getAltitude() > max) {
                        //Set the highest altitude to the max value
                        max = location_List.get(count).getAltitude();
                    }
                    //Add the altitude gained between the two location to the gained variable
                    gained += location_List.get(count).getAltitude() - lm.getAltitude();
                }

                //if statement to check if altitude is lost between the current location
                //and the next location in the Location array
                if(location_List.get(count).getAltitude() < lm.getAltitude()){
                    //if the next altitude is lower than the current min altitude. Then set it as the min altitude
                    if(location_List.get(count).getAltitude() < min) {
                        //Set the lower altitude to the min value
                        min = location_List.get(count).getAltitude();
                    }
                    //Add the altitude Lost between the two location to the lost variable
                    lost += lm.getAltitude() - location_List.get(count).getAltitude();
                }
            }
            count++;
        }//END for Loop

        //Display all altitude results rounded to two decimal places
        tv_maxAlt.setText(String.format("Max Altitude = %smeters", Math.round(max)));
        tv_minAlt.setText(String.format("Min Altitude = %smeters", Math.round(min)));
        tv_gainedAlt.setText(String.format("Altitude Gained = %smeters", Math.round(gained)));
        tv_lostAlt.setText(String.format("Altitude Lost =  -%smeters",  Math.round(lost)));
    }

    /**
     * Method to calculate the MAX, MIN and AVERAGE speed
     */
    public void calculateSpeed(){

        //Initialse variables
        int count = 1;
        double max_speed = 0.0;
        //Set min Speed to an unattainable value
        double min_speed = 80000.0;
        double avg_speed = 0.0;

        //initialise the time between locations array to be 1 smaller than the Location array
        time = new double[location_List.size()-1];

        //loop through each element of the Location array list
        for(Location lm: location_List){
            //if condition to stop calculating time after calculating the
            // time between the 2nd last and the last Location recorded in the array list
            if(count < location_List.size()) {
                //Call method to get the length of time between the current Location and the next Location
                //populate the time array with the times between each recorded location
                time[count-1] = timeBetweenLocations(lm.getTime(), location_List.get(count).getTime());
            }
            //Increase count to get the next lat and long
            count++;
        }//END for loop


        //For loop to calculate the speed between the the recorded Locations
        for(int i = 0; i< dist.length; i++){
            //calculate the speed. s = d / t meter/sec
            double speed = dist[i] / (time[i]/(1000));
            long prep = (int)(time[i] / 1000);
            Log.e("Test Speed", "calculateSpeed: distance = " +dist[i]+ " - Time(sec) = " + prep + " - Speed =" +speed+ " m/sec" );

            //Add all the speeds together
            avg_speed += speed;
            //find the max speed
            if(max_speed < speed){
                max_speed = speed;
            }
            //Find min speed
            if(min_speed > speed){
                min_speed = speed;
            }
        }

        //Final calculation to get the average speed
        avg_speed = avg_speed / (double) dist.length;

        Log.e("Test Speed ", "calculateSpeed: MAX = " +max_speed+ ", Min = " +min_speed+ ", AVG = " +avg_speed );

        //Display results
        tv_maxSpeed.setText(String.format("Max Speed = %s m/sec",  Math.round(max_speed)));
        tv_minSpeed.setText(String.format("Min Speed = %s m/sec",  Math.round(min_speed)));
        tv_avgSpeed.setText(String.format("Avg Speed = %s m/sec",  Math.round(avg_speed)));
    }

    /**
     * Method to calculate the time between recorded locations
     * @param startTime first recorded location time
     * @param endTime next recorded location time
     * @return the length of time between the 2 received times
     */
    public long timeBetweenLocations(Long startTime, Long endTime){
        long mills = 0;
        try {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss aa");

            //Formatting the time
            String myStart = sdf.format(startTime);
            String myEnd = sdf.format(endTime);

            //Real data passed in from the Location ArrayList
            Date Date1 = sdf.parse(myStart);
            Date Date2 = sdf.parse(myEnd);

            //Record the length of time between the locations
            long millsec = Date1.getTime() - Date2.getTime();
            mills = Math.abs(millsec);

        }
        catch(Exception e){
            Log.e("timeBetweenLocations() "," catch exception e");
        }
        //Return the the length of time between the locations
        return mills;
    }

    /**
     * Method to calculate and display the total duration of the recorded activity
     * @param startTime The time of the first location
     * @param endTime The time of the last location
     */
    public void activityDuration(Long startTime, Long endTime){
        //string to display the total time
        String diff;

        long mills = timeBetweenLocations(startTime, endTime);
        Log.e("Mills", "activityDuration: mills = " +mills  );

        int Hours = (int) (mills/(1000 * 60 * 60));
        int Mins = (int) (mills/(1000*60)) % 60;
        long Secs = (int) (mills / 1000) % 60;

        //Format and construct the String of the total time hh:mm:ss
        if(Hours<10){
            diff = "0" +Hours+ ":";
        }
        else diff = Hours + ":";

        if(Mins<10){
            diff += "0" +Mins+ ":";
        }
        else diff+= Mins+ ":";

        if(Secs<10){
            diff += "0" +Secs;
        }
        else diff += Secs;

        //Log display calculations
        Log.v("activityDuration() == "," Calculated hours = " +Hours+ ". min = " +Mins+ ". Secs = " +Secs+ ". Full display = " +diff);

        //Display total time
        tv_time.setText("Total Time = " +diff + " hh:mm:ss");
    }//activityDuration()

    /**
     * Menu button to start a new tracking activity
     * @param item reference to the menu
     * @return return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button,
        int id = item.getItemId();
        if (id == R.id.action_new) {
            //return to the main activity to start a new
            Intent intent = new Intent(DisplayActivity.this, MainActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Method to convert all distances in the dist array to float and return.
     * Used to display on graph
     * @return float array of all distances between each location
     */
    private float[] getDistance() {
        float[] output = new float[dist.length];
        for (int i = 0; i < dist.length; i++)
        {
            output[i] = (float)dist[i];
        }
        return output;
    }

    /**
     * Method to return all altitudes recorded in an array to display on the graph
     * @return float array of altitudes
     */
    private float[] getAltitude() {
        return alt;
    }
    /**
     * Method to inflate the action bar
     * @param menu the menu passed in
     * @return returns true
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // inflate the menu and return true
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

}//END DisplayActivity Class