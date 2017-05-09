package com.example.mychatbot;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.Image;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.mychatbot.Adapters.MessageListAdapter;
import com.example.mychatbot.Adapters.RestaurantListAdapter;
import com.example.mychatbot.Entities.Message;
import com.example.mychatbot.Entities.Restaurant;
import com.example.mychatbot.Utilities.EndPoints;
import com.example.mychatbot.Utilities.MyVolley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;

/**
 * Created by david on 08/05/2017.
 */

public class ResultsActivity extends AppCompatActivity implements LocationListener {

    private EditText place;
    private ImageButton gps;
    private ImageButton search;
    private ListView list;

    private ArrayList<Restaurant> restaurantList;
    private RestaurantListAdapter rlAdapter;
    private Activity context;
    private LocationManager locationManager;

    private String intento;
    private String chatid;
    private String chatname;
    private String lat = "46.0741662"; //initialized for trento
    private String lon = "11.1204982";
    private String gps_lat = ""; //initialized for trento
    private String gps_lon = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        place = (EditText) findViewById(R.id.place);
        gps = (ImageButton) findViewById(R.id.gps);
        search = (ImageButton) findViewById(R.id.search);
        list = (ListView) findViewById(R.id.list);

        restaurantList = new ArrayList<>();
        context = this;

        Intent intent = getIntent();
        intento = intent.getStringExtra(getPackageName() + ".intent");
        chatid = intent.getStringExtra(getPackageName() + ".chatid");
        chatname = intent.getStringExtra(getPackageName() + ".chatname");

        initLocationManager();

        loadListWrapper();

        gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    System.out.println("No permissions");
                    return;
                }
                System.out.println("On clickLatitude:" + lat + ", Longitude:" + lon);
                System.out.println("gps " + locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
                if(gps_lat.equals("")){
                    promptLocationUnavailable();
                } else {
                    lat=gps_lat;
                    lon=gps_lon;
                    setAddressFromLocation();
                    fetchListWrapper();
                }
            }
        });

        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLocationFromAddress(place.getText().toString());
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        fetchListWrapper();
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

    }

    private void fetchListWrapper(){
        if(intento.equals("restaurant")) {
            fetchRestaurantList();
        } else if (intento.equals("cinema")){
            fetchCinemaList();
        } else {
            Toast.makeText(ResultsActivity.this, "Intent can't be matched to rest or cin", Toast.LENGTH_LONG).show();
        }
    }

    private void fetchRestaurantList(){
        //progressDialog = new ProgressDialog(this);
        //progressDialog.setMessage("Fetching messages...");
        //progressDialog.show();

        StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.POST, EndPoints.URL_GET_RESTAURANTS,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //progressDialog.dismiss();
                        restaurantList.clear();
                        rlAdapter.reset();
                        try {
                            JSONObject obj = new JSONObject(response);
                            JSONArray arr= obj.getJSONArray("restaurants");
                            for(int i=0;i<arr.length();i++) {
                                JSONObject r = arr.getJSONObject(i);
                                restaurantList.add(new Restaurant(r.getString("id"),r.getString("name"), "", "", "",
                                        r.getString("street"), r.getString("number"), r.getString("city"),
                                        "", "", "",r.getDouble("distance")));
                            }
                            loadRestaurantList();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        //progressDialog.dismiss();
                        Toast.makeText(ResultsActivity.this, error.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("lat", lat);
                params.put("lon", lon);
                return params;
            }
        };

        MyVolley.getInstance(this).addToRequestQueue(stringRequest);

    }

    private void fetchCinemaList(){

    }

    private void loadListWrapper(){
        if(intento.equals("restaurant")) {
            loadRestaurantList();
        } else if (intento.equals("cinema")){
            loadCinemaList();
        } else {
            Toast.makeText(ResultsActivity.this, "Intent can't be matched to rest or cin", Toast.LENGTH_LONG).show();
        }

    }

    private void loadRestaurantList(){
        rlAdapter = new RestaurantListAdapter(this, R.layout.restaurantrowlayout, restaurantList);
        list.setAdapter(rlAdapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent openRestaurantActivityIntent = new Intent(ResultsActivity.this,
                        RestaurantActivity.class);
                openRestaurantActivityIntent.putExtra(getPackageName() + ".chatid",chatid);
                openRestaurantActivityIntent.putExtra(getPackageName() + ".restaurantid",restaurantList.get(position).getId());
                openRestaurantActivityIntent.putExtra(getPackageName() + ".distance",String.valueOf(restaurantList.get(position).getDistance()));
                System.out.println("my restaurant chosen is"+ restaurantList.get(position).getId());
                openRestaurantActivityIntent.putExtra(getPackageName() + ".chatname",chatname);
                startActivity(openRestaurantActivityIntent);
            }
        });
        list.setSelection(rlAdapter.getCount() - 1);
        list.setSelectionAfterHeaderView();
    }

    private void loadCinemaList(){

    }

    private void initLocationManager(){
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
            Toast.makeText(ResultsActivity.this, "Enable location permissions in your app settings to use this function", Toast.LENGTH_LONG).show();
            return;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 10, this);
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000, 10, this);
    }

    private void setAddressFromLocation(){
        Geocoder geocoder;
        List<Address> addresses = null;
        String address;
        geocoder = new Geocoder(context, Locale.getDefault()); //asks for context i give it activity

        try {
            addresses = geocoder.getFromLocation(Double.parseDouble(lat), Double.parseDouble(lon), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(addresses!=null) {
            address = addresses.get(0).getAddressLine(0)+" "+addresses.get(0).getLocality();;
        }else{
            address = "Unknown Location name";
        }
        place.setText(address);
    }

    public void setLocationFromAddress(String strAddress){

        Geocoder coder = new Geocoder(this);
        List<Address> address;
        try {
            address = coder.getFromLocationName(strAddress,5);
            if (address==null) {
                return;
            }
            Address location=address.get(0);
            lat= String.valueOf(location.getLatitude());
            lon= String.valueOf(location.getLongitude());
            System.out.println("Address "+strAddress+" matched to "+lat+" "+lon);
            setAddressFromLocation();
            fetchListWrapper();

        } catch (Exception e){
            Toast.makeText(ResultsActivity.this, "Input address can't be matched to an actual location", Toast.LENGTH_LONG).show();
        }
    }


    private void promptLocationUnavailable() {
        int off = 0;
        try {
            off = Settings.Secure.getInt(getContentResolver(), Settings.Secure.LOCATION_MODE);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        if (off == 0) {
            Toast.makeText(ResultsActivity.this, "Current location not found, consider turning on GPS", Toast.LENGTH_LONG).show();
            Intent onGPS = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(onGPS);
        } else {
            Toast.makeText(ResultsActivity.this, "Current location not found, try again in few seconds", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        gps_lat = String.valueOf(location.getLatitude());
        gps_lon = String.valueOf(location.getLongitude());
        System.out.println("Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
        Toast.makeText(getApplicationContext(), "Position received, consider turning off GPS to save battery", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude", "disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude", "enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.d("Latitude", "status");
    }

}