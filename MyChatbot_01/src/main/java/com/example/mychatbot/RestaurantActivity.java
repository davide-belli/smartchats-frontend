package com.example.mychatbot;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.example.mychatbot.Entities.Restaurant;
import com.example.mychatbot.Utilities.DownloadImageTask;
import com.example.mychatbot.Utilities.EndPoints;
import com.example.mychatbot.Utilities.IntentUtils;
import com.example.mychatbot.Utilities.MyVolley;
import com.example.mychatbot.Utilities.OnSwipeTouchListener;
import com.example.mychatbot.Utilities.SharedPrefManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by david on 08/05/2017.
 *
 * User can see details about the chosen restaurant.
 * User can access its location on google map, its number on phone dialer,
 * its email address on email app, its website on browser
 * User can share restaurant by swiping up (not on the map preview)
 */

public class RestaurantActivity extends AppCompatActivity {

    private TextView name;
    private TextView address;
    private TextView distance;
    private TextView desc;
    private ImageView map;
    private RelativeLayout infos;
    private ImageButton phone;
    private ImageButton email;
    private ImageButton url;
    private RelativeLayout layout_activity;

    private Restaurant restaurant;
    private ProgressDialog progressDialog;
    private Activity context;

    private String chatid;
    private String chatname;
    private String restaurantid;
    private String distance_value;
    private String map_preview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restaurant);

        name = (TextView) findViewById(R.id.name);
        address = (TextView) findViewById(R.id.address);
        distance = (TextView) findViewById(R.id.distance);
        desc = (TextView) findViewById(R.id.desc);
        map = (ImageView) findViewById(R.id.mappa);
        infos = (RelativeLayout) findViewById(R.id.infos);
        phone = (ImageButton) findViewById(R.id.phone);
        email = (ImageButton) findViewById(R.id.email);
        url = (ImageButton) findViewById(R.id.url);
        layout_activity = (RelativeLayout) findViewById(R.id.layout_activity);

        context = this;

        Intent intent = getIntent();
        restaurantid = intent.getStringExtra(getPackageName() + ".restaurantid");
        System.out.println("restid "+restaurantid);
        chatid = intent.getStringExtra(getPackageName() + ".chatid");
        chatname = intent.getStringExtra(getPackageName() + ".chatname");
        distance_value = null;
        try{
            distance_value = intent.getStringExtra(getPackageName()+".distance");
        }catch(Exception e){}

        getRestaurant();

        //set swipe listener to share restaurant with friend
        layout_activity.setOnTouchListener(new OnSwipeTouchListener(context){

            public void onSwipeRight() {
            }

            public void onSwipeLeft() {
            }

            public void onSwipeTop() {
                if (chatid!=null){
                    sendMessage();
                }
            }
            public void onSwipeBottom() {
            }
        });
    }

    //gets chosen restaurant from DB
    private void getRestaurant() {

        System.out.println("Querying for restaurant: "+restaurantid);


        StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.POST, EndPoints.URL_GET_SINGLE_RESTAURANT,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        try {
                            JSONObject obj = new JSONObject(response);
                            System.out.println(obj);
                            JSONArray arr= obj.getJSONArray("restaurants");
                            JSONObject r = arr.getJSONObject(0);
                            System.out.println("suggestRestaurant:  " + obj);
                            restaurant =new Restaurant(r.getString("id"), r.getString("name"), r.getString("desc"), r.getString("lat"), r.getString("lon"),
                                    r.getString("street"), r.getString("number"), r.getString("city"),
                                    r.getString("phone"), r.getString("email"), r.getString("url"),0.0);
                            System.out.println("restid parsed "+restaurant.getId());
                            setContent();
                            setListeners();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(RestaurantActivity.this, "Turn on Internet Connection to run this App!", Toast.LENGTH_LONG).show();
                    }
                }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("id", restaurantid);
                return params;
            }
        };

        MyVolley.getInstance(this).addToRequestQueue(stringRequest);

    }

    //set layout content and gets a static preview of the location via google maps
    private void setContent(){
        name.setText(restaurant.getName());
        address.setText(restaurant.getStreet()+", "+restaurant.getNumber()+" "+restaurant.getCity());
        if(distance_value!=null) {
            distance.setText(" (" + distance_value + " km from you)");
        } else {
            distance.setVisibility(View.GONE);
        }
        infos.setVisibility(View.VISIBLE);
        desc.setText(restaurant.getDesc());
        map_preview = "http://maps.google.com/maps/api/staticmap?center="+restaurant.getLat()+","+restaurant.getLon()+
                "&zoom=16&size=500x250&maptype=roadmap&sensor=true&markers=color:red%7Clabel:R%7C"
                +restaurant.getLat()+","+restaurant.getLon();
        new DownloadImageTask(map)
                .execute(map_preview);

    }

    //sets listeners to the contextual buttons/image
    private void setListeners(){

        map.setVisibility(View.VISIBLE);
        map.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntentUtils.showDirections(context,restaurant.getLat(),restaurant.getLon());
            }
        });

        phone.setVisibility(View.VISIBLE);
        phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(restaurant.getPhone().equals("")){
                    Toast.makeText(context, "Phone number unavailable for this restaurant", Toast.LENGTH_LONG).show();
                } else {
                    IntentUtils.dial(context, restaurant.getPhone());
                }
            }
        });

        url.setVisibility(View.VISIBLE);
        url.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(restaurant.getUrl().equals("")){
                    Toast.makeText(context, "Website unavailable for this restaurant", Toast.LENGTH_LONG).show();
                } else {
                    IntentUtils.invokeWebBrowser(context, restaurant.getUrl());
                }
            }
        });

        email.setVisibility(View.VISIBLE);
        email.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(restaurant.getEmail().equals("")){
                    Toast.makeText(context, "Email address unavailable for this restaurant", Toast.LENGTH_LONG).show();
                } else {
                    IntentUtils.sendEmail(context, restaurant.getEmail());
                }
            }
        });
    }

    //sends the current restaurant as a message
    private void sendMessage() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Suggesting restaurant..."+restaurant.getName());
        progressDialog.show();

        final String fb_id = SharedPrefManager.getInstance(this).getFacebookId();
        final String content = "Check out:<u><font color='#0645AD'><br>"+restaurant.getName()+",<br>"+restaurant.getStreet()
                                +", "+restaurant.getNumber()+", "+restaurant.getCity()+"</font></u>";
        System.out.println("Message: " + fb_id + "  " + content + " " + chatid);

        StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.POST, EndPoints.URL_SEND_MESSAGE,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        progressDialog.dismiss();
                        try {
                            JSONObject obj = new JSONObject(response);
                            Toast.makeText(RestaurantActivity.this, obj.getString("message"), Toast.LENGTH_LONG).show();
                            if (obj.getBoolean("error") == false) {
                                sendNotification(content);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Intent openChatActivityIntent = new Intent(RestaurantActivity.this,
                                ChatActivity.class);
                        openChatActivityIntent.putExtra(getPackageName() + ".chatid",chatid);
                        openChatActivityIntent.putExtra(getPackageName() + ".chatname",chatname);
                        startActivity(openChatActivityIntent);
                        overridePendingTransition(R.anim.enter_up, R.anim.exit_up);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        progressDialog.dismiss();
                        Toast.makeText(RestaurantActivity.this, "Error sending message", Toast.LENGTH_LONG).show();
                    }
                }) {

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("sender", fb_id);
                params.put("content", content);
                params.put("chatid", chatid);
                params.put("intent", "");
                params.put("restaurant", restaurantid);
                params.put("cinema", "");
                params.put("image", map_preview);
                return params;
            }
        };

        MyVolley.getInstance(this).addToRequestQueue(stringRequest);
    }

    //creates a push notification when the message is sent succesfully
    private void sendNotification(final String content) {

        final String fb_id = SharedPrefManager.getInstance(this).getFacebookId();
        final String title = getString(R.string.app_name);
        System.out.println("Notification: " + fb_id + "  " + content + " " + title);

        StringRequest stringRequest = new StringRequest(com.android.volley.Request.Method.POST, EndPoints.URL_SEND_NOTIFICATION,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        //Toast.makeText(ChatActivity.this, response, Toast.LENGTH_LONG).show();
                        System.out.println("Firebase notification: " + response);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                    }
                }) {
            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                Map<String, String> params = new HashMap<>();
                params.put("title", title);
                params.put("message", content);
                params.put("fbid", fb_id);
                params.put("chatid", chatid);
                return params;
            }
        };

        MyVolley.getInstance(this).addToRequestQueue(stringRequest);
    }

    //creates a Menu with help option
    public boolean onCreateOptionsMenu(Menu menu){
        super.onCreateOptionsMenu(menu);
        int base=Menu.FIRST;
        MenuItem item1=menu.add(base,1,1,"Help");
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        System.out.println("item="+item.getItemId());
        if (item.getItemId()==1){
            Toast.makeText(this, "Swipe Up on the Restaurant's Name/Description to share it as a message", Toast.LENGTH_LONG).show();
            Toast.makeText(this, "Click on the map preview to find the restaurant on Google Maps.", Toast.LENGTH_LONG).show();
            Toast.makeText(this, "Click on the bottom buttons to open email, dialer or website", Toast.LENGTH_LONG).show();
        }else {
            return super.onOptionsItemSelected(item);
        }return true;
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_left, R.anim.exit_left);
    }
}
