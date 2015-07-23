package net.samsung.emergencybff;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import net.samsung.emergencybff.database.helper.DatabaseHelper;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


public class MainActivity extends Activity implements View.OnClickListener {

    View mBtn117, mBtnPolice, mBtnHospital, mBtnFireDept, mBtnAmbulance, mBtnSamsung;
    TextView mTvPoliceName, mTvHospitalName, mTvFireName, mTvAmbulanceName, mTvSamsungName;
    TextView mTvPoliceInfo, mTvHospitalInfo, mTvFireInfo, mTvAmbulanceInfo, mTvSamsungInfo;
    TextView mTvPoliceNum, mTvHospitalNum, mTvFireNum, mTvAmbulanceNum, mTvSamsungNum;
    TextView mTvPoliceDistance, mTvHospitalDistance, mTvFireDistance, mTvAmbulanceDistance, mTvSamsungDistance;
    TextView mTvLocation, ivPanel, ivHide;
    ImageButton mIbOptions;
    ImageView ivMap;
    FrameLayout flMap;
    AutoCompleteTextView searchView;
    LocationManager locationManager;
    LocationListener locationListener;
    DatabaseHelper databaseHelper;
    protected static final String STATIC_MAP_API_ENDPOINT = "http://maps.google.com/maps/api/staticmap?size=300x200&center=";
    protected String MAP_MARKERS = "&markers=color:blue%7C", MARKED_IN_MAP = "";
    private static final String API_KEY = "AIzaSyD-rVOkZ6HXvy5oP61CNTKxhLcuKUbWGtQ";
    private static final String TAG = "BFF.MainActivity";
    protected String MAP_COORDINATES = "";

    boolean isMapLoaded = false;
    boolean isConnectionReliable = true;

    //FOR AWS
    boolean isUsingAWS = !false;
    RequestQueue mRequestQueue;
    List<JSONObject> mHospitalList;
    List<JSONObject> mFireDeptList;
    List<JSONObject> mPoliceList;
    List<JSONObject> mAmbulanceList;
    Response.Listener<String> mVolleyListener;
    Response.ErrorListener mVolleyErrorListener;
    String mLocationAddress;
    final String PREF_ID = "net.samsung.emergencybff.PREF";
    final String PREF_LOCAL_LIST = "LOCAL_LIST";
    final String PREF_LAST_LOCATION = "LAST_LOCATION";


    private String[] cities = {
            "Taguig",
            "Pasig",
            "Makati",
            "Manila",
            "Caloocan",
            "Quezon City",
            "Valenzuela",
            "Paranaque",
            "Las Piñas",
            "Muntinlupa",
            "Mandaluyong",
            "Pasay",
            "Navotas",
            "San Juan",
            "Marikina",
            "Malabon",
            "Pateros"
    };

    private double[][] coordinates = {
            {14.517609, 121.052166},
            {14.576568, 121.085456},
            {14.554668, 121.024385},
            {14.59965, 120.984973},
            {14.651019, 120.971705},
            {14.675866, 121.043741},
            {14.701038, 120.983217},
            {14.479404, 121.016714},
            {14.444585, 120.994291},
            {14.408129, 121.041506},
            {14.579422, 121.035828},
            {14.53753, 121.000245},
            {14.671380, 120.939936},
            {14.599487, 121.036833},
            {14.650516, 121.102871},
            {14.662293, 120.956213},
            {14.548278, 121.070915}
    };

    private int[][] topIds = new int[5][4];
    private double[][] topDistance = new double[5][4];
    private boolean mHasCalled = false;
    private double mLat;
    private double mLong;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        /*isUsingAWS = isUsingAWS && isNetworkAvailable(this);*/
        init();
        if(isUsingAWS){
            initAWS();
            //For last location
            SharedPreferences preferences = getSharedPreferences(PREF_ID, MODE_PRIVATE);
            String str = preferences.getString(PREF_LOCAL_LIST, null);
            if(str != null){
                try {
                    populateLists(str);
                    updateButtonsFromAWS();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            String location = preferences.getString(PREF_LAST_LOCATION, null);
            if (location != null){
                ((TextView) findViewById(R.id.location)).setText("Your last known location is at " + location);
            }
        }
        try {
            databaseHelper = new DatabaseHelper(this, "bff_bundle.db", 1);
            databaseHelper.createDataBase();
        } catch (IOException e) {
            e.printStackTrace();
        }
        databaseHelper.openDataBase();

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Updating emergency contacts");
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);

        getUserLocation();
    }

    private void initAWS(){
        mRequestQueue = Volley.newRequestQueue(getApplicationContext());
        mVolleyListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String s) {
                try {
                    Log.e(TAG, s);
                    SharedPreferences preferences = getSharedPreferences(PREF_ID, MODE_PRIVATE);
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putString(PREF_LOCAL_LIST, s);
                    editor.putString(PREF_LAST_LOCATION, mLocationAddress);
                    editor.commit();

                    populateLists(s);
                    updateButtonsFromAWS();
                    DatabaseHelper.createInstance(getApplication());
                    DatabaseHelper.getInstance().addContactsIfNotExists(s);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        mVolleyErrorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Toast.makeText(getApplicationContext(), "Error on Volley!", Toast.LENGTH_LONG).show();
                volleyError.printStackTrace();
            }
        };
    }

    private void populateLists(String s) throws JSONException {
        JSONObject list = new JSONObject(s);
        mHospitalList = new ArrayList<>();
        mPoliceList = new ArrayList<>();
        mFireDeptList = new ArrayList<>();
        mAmbulanceList = new ArrayList<>();
        String[] types = {"hospital", "police", "firedept", "ambulance"};
        for(int c = 0 ; c < types.length; c++){
            JSONArray array = list.getJSONArray(types[c]);
            for (int x = 0 ; x < array.length(); x++) {
                switch (c) {
                    case 0:
                        try{
                            mHospitalList.add(array.getJSONObject(x));
                        }catch (Exception ex){
                            continue;
                        }
                        break;
                    case 1:
                        try {
                            mPoliceList.add(array.getJSONObject(x));
                        }catch (Exception ex){
                            continue;
                        }
                        break;
                    case 2:
                        try {
                            mFireDeptList.add(array.getJSONObject(x));
                        }catch (Exception ex){
                            continue;
                        }
                        break;
                    case 3:
                        try {
                            mAmbulanceList.add(array.getJSONObject(x));
                        }catch (Exception ex){
                            continue;
                        }
                        break;
                }
            }
        }
    }

    private void updateButtonsFromAWS() {
        JSONObject policeObject = mPoliceList.get(0);
        JSONObject fireDeptObject = mFireDeptList.get(0);
        JSONObject hospitalObject = mHospitalList.get(0);
        JSONObject ambulanceObject = mAmbulanceList.get(0);
        try {
            mBtnPolice.setTag(policeObject.getString("number"));
            mTvPoliceName.setText(policeObject.getString("name"));
            Log.e(TAG, "lat: " + policeObject.getDouble("latitude") + " ; long : " + policeObject.getDouble("longitude"));
            mTvPoliceDistance.setText(Html.fromHtml("<i> " + String.format("%.2f", getDistance(policeObject.getDouble("latitude"), policeObject.getDouble("longitude"))) + " km away</i>"));
            mTvPoliceInfo.setText(textLimit(policeObject.getString("address"), 0));
            mTvPoliceNum.setText(textLimit(policeObject.getString("number"), 1));
            mBtnPolice.setVisibility(View.VISIBLE);
            mBtnPolice.setOnClickListener(this);

            mBtnFireDept.setTag(fireDeptObject.getString("number"));
            mTvFireName.setText(fireDeptObject.getString("name"));
            mTvFireDistance.setText(Html.fromHtml("<i> " + String.format("%.2f", getDistance(fireDeptObject.getDouble("latitude"), fireDeptObject.getDouble("longitude"))) + " km away</i>"));
            mTvFireInfo.setText(textLimit(fireDeptObject.getString("address"), 0));
            mTvFireNum.setText(textLimit(fireDeptObject.getString("number"), 1));
            mBtnFireDept.setVisibility(View.VISIBLE);
            mBtnFireDept.setOnClickListener(this);

            mBtnHospital.setTag(hospitalObject.getString("number"));
            mTvHospitalName.setText(hospitalObject.getString("name"));
            mTvHospitalDistance.setText(Html.fromHtml("<i> " + String.format("%.2f", getDistance(hospitalObject.getDouble("latitude"), hospitalObject.getDouble("longitude"))) + " km away</i>"));
            mTvHospitalInfo.setText(textLimit(hospitalObject.getString("address"), 0));
            mTvHospitalNum.setText(textLimit(hospitalObject.getString("number"), 1));
            mBtnHospital.setVisibility(View.VISIBLE);
            mBtnHospital.setOnClickListener(this);

            mBtnAmbulance.setTag(ambulanceObject.getString("number"));
            mTvAmbulanceName.setText(ambulanceObject.getString("name"));
            mTvAmbulanceDistance.setText(Html.fromHtml("<i> " + String.format("%.2f", getDistance(ambulanceObject.getDouble("latitude"), ambulanceObject.getDouble("longitude"))) + " km away</i>"));
            mTvAmbulanceInfo.setText(textLimit(ambulanceObject.getString("address"), 0));
            mTvAmbulanceNum.setText(textLimit(ambulanceObject.getString("number"), 1));
            mBtnAmbulance.setVisibility(View.VISIBLE);
            mBtnAmbulance.setOnClickListener(this);
        } catch (Exception ex){
            ex.printStackTrace();
        }
    }

    private void init() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        if ((getResources().getBoolean(R.bool.portrait_only)))
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_main);

        mTvLocation = (TextView) findViewById(R.id.location);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, cities);
        final Drawable x = getResources().getDrawable(R.drawable.ic_action_remove);
        final Drawable search = getResources().getDrawable(R.drawable.searchbtn);
        searchView = (AutoCompleteTextView) findViewById(R.id.auto_search);
        searchView.setAdapter(adapter);
        searchView.setOnTouchListener(
                new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (searchView.getCompoundDrawables()[2] == null) {
                            return false;
                        }
                        if (event.getAction() != MotionEvent.ACTION_UP) {
                            return false;
                        }
                        if (event.getX() > searchView.getWidth() - searchView.getPaddingRight() - x.getIntrinsicWidth()) {
                            searchView.setText("");
                            searchView.setCompoundDrawablesWithIntrinsicBounds(null, null, search, null);
                        }
                        if (!searchView.getText().equals(null) && !searchView.getText().equals("")) {
                            searchView.setCompoundDrawablesWithIntrinsicBounds(null, null, x, null);
                            /*Toast.makeText(getApplicationContext(),"Search View "+searchView.getText(),Toast.LENGTH_SHORT).show();*/
                        }
                        searchView.setHint("Search City");
                        return false;
                    }
                }

        );
        searchView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View view, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_SEARCH || keyCode == KeyEvent.KEYCODE_ENTER) {
                    String text = ((TextView) view).getText().toString();
                    for (int i = 0; i < cities.length; i++) {
                        if ((cities[i].toLowerCase()).equals(text.toLowerCase().replace(" ", ""))) {
                            Location l = new Location("MyLoc");
                            l.setLatitude(coordinates[i][0]);
                            l.setLongitude(coordinates[i][1]);
                            updateContacts(l);
                            updateMap(l);
                            InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                            in.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                            searchView.clearFocus();
                            searchView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.searchbtn, 0);
                            return false;
                        }
                    }
                    Toast.makeText(getApplicationContext(), "No City found", Toast.LENGTH_SHORT).show();
                }
                if (!searchView.getText().equals(null) && !searchView.getText().equals(""))
                    searchView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_action_remove, 0);

                ivPanel.setVisibility(View.VISIBLE);
                ivHide.setVisibility(View.INVISIBLE);
                flMap.setVisibility(View.GONE);
                return false;
            }
        });
        searchView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                searchView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.searchbtn, 0);
                String text = ((TextView) view).getText().toString();
                for (int i = 0; i < cities.length; i++) {
                    if (cities[i].equals(text)) {
                        Location l = new Location("MyLoc");
                        l.setLatitude(coordinates[i][0]);
                        l.setLongitude(coordinates[i][1]);
                        updateContacts(l);
                        updateMap(l);
                        InputMethodManager in = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        in.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                        searchView.clearFocus();
                        searchView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.searchbtn, 0);
                        break;
                    }
                }
                ivPanel.setVisibility(View.VISIBLE);
                ivHide.setVisibility(View.INVISIBLE);
                flMap.setVisibility(View.GONE);
            }
        });

        mBtn117 = findViewById(R.id.buttonAmbulance117);
        mBtn117.setTag("117");
        mBtn117.setOnClickListener(this);

        mBtnPolice = findViewById(R.id.buttonPolice);
        mBtnFireDept = findViewById(R.id.buttonFireDepartment);
        mBtnHospital = findViewById(R.id.buttonHospital);
        mBtnAmbulance = findViewById(R.id.buttonAmbulance);
        mBtnSamsung = findViewById(R.id.buttonSamsung);

        mTvPoliceName = (TextView) findViewById(R.id.txtPoliceName);
        mTvHospitalName = (TextView) findViewById(R.id.txtHospitalName);
        mTvFireName = (TextView) findViewById(R.id.txtFireName);
        mTvAmbulanceName = (TextView) findViewById(R.id.txtAmbulanceName);
        mTvSamsungName = (TextView) findViewById(R.id.txtSamsungName);

        mTvPoliceInfo = (TextView) findViewById(R.id.txtPoliceInfo);
        mTvHospitalInfo = (TextView) findViewById(R.id.txtHospitalInfo);
        mTvFireInfo = (TextView) findViewById(R.id.txtFireDeptInfo);
        mTvAmbulanceInfo = (TextView) findViewById(R.id.txtAmbulanceInfo);
        mTvSamsungInfo = (TextView) findViewById(R.id.txtSamsungInfo);

        mTvPoliceNum = (TextView) findViewById(R.id.txtPoliceNumber);
        mTvHospitalNum = (TextView) findViewById(R.id.txtHospitalNumber);
        mTvFireNum = (TextView) findViewById(R.id.txtFireDeptNumber);
        mTvAmbulanceNum = (TextView) findViewById(R.id.txtAmbulanceNumber);
        mTvSamsungNum = (TextView) findViewById(R.id.txtSamsungNumber);

        mTvPoliceDistance = (TextView) findViewById(R.id.txtPoliceDistance);
        mTvHospitalDistance = (TextView) findViewById(R.id.txtHospitalDistance);
        mTvFireDistance = (TextView) findViewById(R.id.txtFireDeptDistance);
        mTvAmbulanceDistance = (TextView) findViewById(R.id.txtAmbulanceDistance);
//        mTvSamsungDistance = (TextView) findViewById(R.id.txtSamsungDistance);

        findViewById(R.id.btn_pol_more).setOnClickListener(this);
        findViewById(R.id.btn_fire_more).setOnClickListener(this);
        findViewById(R.id.btn_amb_more).setOnClickListener(this);
        findViewById(R.id.btn_hosp_more).setOnClickListener(this);
        findViewById(R.id.btn_sam_more).setOnClickListener(this);

        findViewById(R.id.btn_refresh).setOnClickListener(this);

        ivMap = (ImageView) findViewById(R.id.img_map);
        flMap = (FrameLayout) findViewById(R.id.fl_map);

        mIbOptions = (ImageButton) findViewById(R.id.imgbtn_options);
        mIbOptions.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Creating the instance of PopupMenu
                PopupMenu popup = new PopupMenu(MainActivity.this, mIbOptions);
                //Inflating the Popup using xml file
                popup.getMenuInflater().inflate(R.menu.popup_menu, popup.getMenu());

                //registering popup with OnMenuItemClickListener
                popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.item_sync:
                                final String url = "https://s3-ap-southeast-1.amazonaws.com/bffbundle/bff_bundle.db";
                                final DownloadTask downloadTask = new DownloadTask(MainActivity.this);
                                downloadTask.execute(url);
                                mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        downloadTask.cancel(true);
                                    }
                                });
                                break;
                            case R.id.item_about:
                                Toast.makeText(MainActivity.this, "This is Samsung 321.", Toast.LENGTH_SHORT).show();
                                break;
                            case R.id.item_help:
                                Toast.makeText(MainActivity.this, "Help.", Toast.LENGTH_SHORT).show();
                                break;
                        }

                        return true;
                    }
                });
                Object menuHelper;
                Class[] argTypes;
                try {
                    Field fMenuHelper = PopupMenu.class.getDeclaredField("mPopup");
                    fMenuHelper.setAccessible(true);
                    menuHelper = fMenuHelper.get(popup);
                    argTypes = new Class[]{boolean.class};
                    menuHelper.getClass().getDeclaredMethod("setForceShowIcon", argTypes).invoke(menuHelper, true);
                } catch (Exception e) {
                    // Possible exceptions are NoSuchMethodError and NoSuchFieldError
                    //
                    // In either case, an exception indicates something is wrong with the reflection code, or the
                    // structure of the PopupMenu class or its dependencies has changed.
                    //
                    // These exceptions should never happen since we're shipping the AppCompat library in our own apk,
                    // but in the case that they do, we simply can't force icons to display, so log the error and
                    // show the menu normally.

                    Log.w(TAG, "error forcing menu icons to show", e);
                    popup.show();
                    return;
                }
                popup.show();//showing popup menu

                // Dim out all the other list items if they exist
            }

        });


        ivMap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = "http://maps.google.com/maps?q=" + MAP_COORDINATES;
                Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(intent);
            }
        });

        ivPanel = (TextView) findViewById(R.id.panelMap);
        ivPanel.setOnClickListener(this);

        ivHide = (TextView) findViewById(R.id.panelHide);
        ivHide.setOnClickListener(this);

        // Define a listener that responds to location updates

        locationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                getAddressFromLocation(location.getLatitude(), location.getLongitude(), getApplicationContext(), new GeocoderHandler());
                if(isUsingAWS){
                    /*updateContactsFromAWS(location);*/
                    mLat = location.getLatitude();
                    mLong = location.getLongitude();
                }else {
                    updateContacts(location);
                }
                updateMap(location);
                /*getAddressFromLocation(location.getLatitude(), location.getLongitude(), getApplicationContext(), new GeocoderHandler());*/
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
    }

    private void updateContactsFromAWS() {
        if(mRequestQueue != null){
            int n = 0;
            /*while((mLocationAddress == null || mLocationAddress.isEmpty()) && n < 5){
                try {
                    Thread.sleep(1000);
                    n++;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }*/
            if(mLat == 0.0 || mLong == 0.0 || mLocationAddress == null || mLocationAddress.isEmpty()){
                Log.e(TAG, "Errorrrr");
                Toast.makeText(getApplicationContext(), "ERROR", Toast.LENGTH_LONG).show();
                return;
            }

            locationManager.removeUpdates(locationListener);
            Log.e("ASD", "lat : " + mLat + " ; lon : " + mLong + " ; city : " + mLocationAddress);
            ((TextView) findViewById(R.id.textLocation)).setText("Coordinates: " + mLat + "," + mLong);
            String url = "http://awsbffservlet-env.elasticbeanstalk.com/fetchdata?lat="+mLat+"&long="+mLong+"&city="+mLocationAddress+"";
            StringRequest request = new StringRequest(
                    Request.Method.GET,
                    url,
                    mVolleyListener,
                    mVolleyErrorListener);
            mRequestQueue.add(request);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHasCalled = false;
    }

    @Override
    protected void onDestroy() {
        locationManager.removeUpdates(locationListener);
        super.onDestroy();
    }

    private void getUserLocation() {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        boolean isEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (!isEnabled) {
            new AlertDialog.Builder(this)
                    .setTitle("Turn Location on")
                    .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getApplicationContext(), "Please turn on Location in Settings", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setMessage("Please select High accuracy location mode in Menu > Settings > Location. ")
                    .show();
        }
        Criteria criteria = new Criteria();
        String provider = locationManager.getBestProvider(criteria, false);
        // Get last known location first
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            updateContacts(location);
            updateMap(location);
            getAddressFromLocation(location.getLatitude(), location.getLongitude(), getApplicationContext(), new GeocoderHandler());
        }

        // Register the listener with the Location Manager to receive location updates
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000, 0, locationListener);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 0, locationListener);
    }

    @Override
    public void onBackPressed() {
        if (searchView.isFocused()) {
            searchView.clearFocus();
            searchView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.searchbtn, 0);
        } else new AlertDialog.Builder(this)
                .setTitle("Are you sure you want to exit?")
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    private void updateMap(Location location) {
        //findViewById(R.id.panelLoad).setVisibility(View.VISIBLE);
        AsyncTask<Location, Void, Bitmap> setImageFromUrl = new AsyncTask<Location, Void, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Location... locations) {
                String strLoc = locations[0].getLatitude() + "," + locations[0].getLongitude();
                MAP_COORDINATES = strLoc;
                Bitmap bmp = null;

                try {
                    HttpParams httpParams = new BasicHttpParams();

                    ConnManagerParams.setTimeout(httpParams, 50000);
                    HttpConnectionParams.setConnectionTimeout(httpParams, 50000);
                    HttpConnectionParams.setSoTimeout(httpParams, 50000);

                    HttpClient httpclient = new DefaultHttpClient(httpParams);
                    HttpGet request = new HttpGet(STATIC_MAP_API_ENDPOINT + strLoc + "&markers=" + strLoc + MARKED_IN_MAP + "&key=" + API_KEY);

                    InputStream in;
                    try {
                        in = httpclient.execute(request).getEntity().getContent();
                        bmp = BitmapFactory.decodeStream(in);
                        in.close();
                    }
/*                    catch (ConnectTimeoutException e) {
                        Log.e(TAG, "Connection Timed out");
                        Toast.makeText(getApplicationContext(), "Connection Timed out", Toast.LENGTH_SHORT).show();
                        return null;
                    } */
                    catch (Exception e){
                        Log.e(TAG, "Timout Exception "+e.toString());}
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return bmp;
            }

            protected void onPostExecute(Bitmap bmp) {
                if (bmp != null) {
                    //findViewById(R.id.panelLoad).setVisibility(View.GONE);
                    final ImageView iv = (ImageView) findViewById(R.id.img_map);
                    iv.setImageBitmap(bmp);
                    flMap.setVisibility(View.VISIBLE);
                    ivPanel.setText("Show Map");
                    ivPanel.setVisibility(View.INVISIBLE);
                    ivHide.setVisibility(View.VISIBLE);
                    isConnectionReliable = true;
                }
                else
                {
                    Toast.makeText(getApplicationContext(), "Connect to a reliable internet connection to view map", Toast.LENGTH_SHORT).show();
                    ivPanel.setText("Show Map");
                    isConnectionReliable = false;
                    //locationManager.removeUpdates(locationListener);
                }
            }
        };
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        //if (!isMapLoaded)
        if (isConnected) {
            Log.e(TAG, "Load Map");
            ivPanel.setText("Loading Map...");
            findViewById(R.id.tv_load_map).setVisibility(View.INVISIBLE);
            isMapLoaded = true;
            setImageFromUrl.execute(location);
        } else {
            ivPanel.setVisibility(View.VISIBLE);
            ivHide.setVisibility(View.INVISIBLE);
            flMap.setVisibility(View.GONE);
            ivPanel.setText("Show Map");
            //findViewById(R.id.panelLoad).setVisibility(View.GONE);
        }
    }

    private void updateContacts(Location location) {
        //Stop Listening!
        locationManager.removeUpdates(locationListener);
        MARKED_IN_MAP = "";

        ((TextView) findViewById(R.id.textLocation)).setText("Coordinates: " + location.getLatitude() + "," + location.getLongitude());
        Cursor[] nearestContacts = new Cursor[5];

        for (int i = 0; i < 5; i++) {
            nearestContacts[i] = getNearestContact(location, (i + 1));
            if (nearestContacts[i] != null) {
                nearestContacts[i].moveToFirst();
                String latlong = nearestContacts[i].getString(nearestContacts[i].getColumnIndex(DatabaseHelper._LAT)) + "," + nearestContacts[i].getString(nearestContacts[i].getColumnIndex(DatabaseHelper._LONG));
                String number = nearestContacts[i].getString(nearestContacts[i].getColumnIndex(DatabaseHelper._NUMBER));
                String name = (nearestContacts[i].getString(nearestContacts[i].getColumnIndex(DatabaseHelper._NAME)) != null) ? nearestContacts[i].getString(nearestContacts[i].getColumnIndex(DatabaseHelper._NAME)) : "";
                String address = ((nearestContacts[i].getString(nearestContacts[i].getColumnIndex(DatabaseHelper._ADDRESS)) != null) ? nearestContacts[i].getString(nearestContacts[i].getColumnIndex(DatabaseHelper._ADDRESS)) : "");
                String cleanNumber = number.split("[/]", 2)[0];
                cleanNumber = cleanNumber.replaceAll("[\\p{P}\\p{S}]", "");
                cleanNumber = cleanNumber.replaceAll("[ ]", "");
                switch (i) {
                    case 0:
                        mBtnPolice.setTag(cleanNumber);
                        mTvPoliceName.setText(name);
                        mTvPoliceDistance.setText(Html.fromHtml("<i> " + String.format("%.2f", topDistance[i][0] / 1000) + " km away</i>"));
                        mTvPoliceInfo.setText(textLimit(address, 0));
                        mTvPoliceNum.setText(textLimit(number, 1));
                        mBtnPolice.setVisibility(View.VISIBLE);
                        mBtnPolice.setOnClickListener(this);
                        latlong = MAP_MARKERS + "label:P%7C" + latlong;
                        break;
                    case 1:
                        mBtnFireDept.setTag(cleanNumber);
                        mTvFireName.setText(name);
                        mTvFireDistance.setText(Html.fromHtml("<i> " + String.format("%.2f", topDistance[i][0] / 1000) + " km away</i>"));
                        mTvFireInfo.setText(textLimit(address, 0));
                        mTvFireNum.setText(textLimit(number, 1));
                        mBtnFireDept.setVisibility(View.VISIBLE);
                        mBtnFireDept.setOnClickListener(this);
                        latlong = MAP_MARKERS + "label:F%7C" + latlong;
                        break;
                    case 2:
                        mBtnHospital.setTag(cleanNumber);
                        mTvHospitalName.setText(name);
                        mTvHospitalDistance.setText(Html.fromHtml("<i> " + String.format("%.2f", topDistance[i][0] / 1000) + " km away</i>"));
                        mTvHospitalInfo.setText(textLimit(address, 0));
                        mTvHospitalNum.setText(textLimit(number, 1));
                        mBtnHospital.setVisibility(View.VISIBLE);
                        mBtnHospital.setOnClickListener(this);
                        latlong = MAP_MARKERS + "label:H%7C" + latlong;
                        break;
                    case 3:
                        mBtnAmbulance.setTag(cleanNumber);
                        mTvAmbulanceName.setText(name);
                        mTvAmbulanceDistance.setText(Html.fromHtml("<i> " + String.format("%.2f", topDistance[i][0] / 1000) + " km away</i>"));
                        mTvAmbulanceInfo.setText(textLimit(address, 0));
                        mTvAmbulanceNum.setText(textLimit(number, 1));
                        mBtnAmbulance.setVisibility(View.VISIBLE);
                        mBtnAmbulance.setOnClickListener(this);
                        latlong = MAP_MARKERS + "label:A%7C" + latlong;
                        break;
                    case 4:
                        mBtnSamsung.setTag(cleanNumber);
                        mTvSamsungName.setText(name);
                        mTvSamsungInfo.setText(textLimit(address, 0));
                        mTvSamsungNum.setText(textLimit(number, 1));
                        mBtnSamsung.setVisibility(View.VISIBLE);
                        mBtnSamsung.setOnClickListener(this);
                        latlong = MAP_MARKERS + "label:S%7C" + latlong;
                        break;
                }

                nearestContacts[i].close();
                MARKED_IN_MAP += latlong;
            }
        }

    }

    private Cursor getNearestContact(Location location, int type) {
        Log.e(TAG, "Getting nearest contact for type " + type);
        Cursor cursor = databaseHelper.getInfo(type);
        double[] topDist = new double[]{
                999999999.0,
                999999999.0,
                999999999.0,
                999999999.0
        };
        int[] topId = new int[]{0, 0, 0, 0};
        double distance = 999999999.0, tempDist = 0;
        double tempLat, tempLong;
        Location tempLoc = new Location("TempLoc");
        int nearestId = -1;

        if (cursor.moveToFirst()) {
            do {
                if (cursor.getString(cursor.getColumnIndex(DatabaseHelper._NUMBER)) != null) {
                    tempLat = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper._LAT));
                    tempLong = cursor.getDouble(cursor.getColumnIndex(DatabaseHelper._LONG));
                    tempLoc.setLongitude(tempLong);
                    tempLoc.setLatitude(tempLat);
                    tempDist = location.distanceTo(tempLoc);
/*                    if (Math.abs(tempLong - location.getLongitude()) < distance &&
                            Math.abs(tempLat - location.getLatitude()) < distance) {
                    normal dist formula
                    tempDist = Location.distanceBetween(tempLat,tempLong,location.getLatitude(),location.getLongitude()));
                        tempDist = Math.sqrt(
                                (tempLong - location.getLongitude()) * (tempLong - location.getLongitude()) +
                                        (tempLat - location.getLatitude()) * (tempLat - location.getLatitude())
                        );
                    }*/
                    for (int i = 0; i < topDist.length; i++) {
                        if (topDist[i] > tempDist) {
                            for (int j = topDist.length - 2; j >= i; j--) {
                                topDist[j + 1] = topDist[j];
                                topId[j + 1] = topId[j];
                            }
                            topDist[i] = tempDist;
                            topId[i] = cursor.getInt(cursor.getColumnIndex(DatabaseHelper._ID));
                            break;
                        }
                    }
                    /*if (distance > tempDist) {
                        distance = tempDist;
                        nearestId = cursor.getInt(cursor.getColumnIndex(DatabaseHelper._ID));
                    }*/
                }

            } while (cursor.moveToNext());
        }
        cursor.close();
        for (int i = 0; i < 4; i++) {
            topIds[type - 1][i] = topId[i];
            topDistance[type - 1][i] = topDist[i];
        }
        //return (nearestId > 0) ? databaseHelper.getContact(nearestId) : null;
        return (topId[0] > 0) ? databaseHelper.getContact(topId[0]) : null;
    }

    public static void getAddressFromLocation(final double latitude, final double longitude, final Context context, final Handler handler) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                String result = null;
                try {
                    List<Address> addressList = geocoder.getFromLocation(
                            latitude, longitude, 1);
                    if (addressList != null && addressList.size() > 0) {
                        Address address = addressList.get(0);
                        StringBuilder sb = new StringBuilder();
/*                        for (int i = 0; i < address.getMaxAddressLineIndex(); i++) {
                            sb.append(address.getAddressLine(i)).append("\n");
                        }*/
                        sb.append(address.getLocality());
/*                        sb.append(address.getPostalCode()).append("\n");
                        sb.append(address.getCountryName());*/
                        result = sb.toString();
                    }
                } catch (IOException e) {
                    Log.e("LocationAddress", "Unable connect to Geocoder", e);
                } finally {
                    Message message = Message.obtain();
                    message.setTarget(handler);
                    if (result != null) {
                        message.what = 1;
                        Bundle bundle = new Bundle();
/*                        result = "Latitude: " + latitude + " Longitude: " + longitude +
                                "\n\nAddress:\n" + result;*/
                        bundle.putString("address", result);
                        message.setData(bundle);
                    } else {
                        message.what = 1;
                        Bundle bundle = new Bundle();
//                        result = "Latitude: " + latitude + " Longitude: " + longitude +
//                                "\n Unable to get address for this lat-long.";
                        result = "";
                        bundle.putString("address", result);
                        message.setData(bundle);
                    }
                    message.sendToTarget();
                }
            }
        };
        thread.start();
    }

    private class GeocoderHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            String locationAddress;
            switch (message.what) {
                case 1:
                    Bundle bundle = message.getData();
                    locationAddress = bundle.getString("address");
                    break;
                default:
                    locationAddress = null;
            }
            if (!locationAddress.equals(null) && locationAddress != "") {
                /*searchView.setHint(locationAddress);*/
                ((TextView) findViewById(R.id.location)).setText("You're currently at " + locationAddress);
                mLocationAddress = locationAddress;
                if (isUsingAWS)
                    updateContactsFromAWS();
            } else {
/*                searchView.setHint("Search City");
                ((TextView) findViewById(R.id.location)).setText("Location");*/

                searchView.setText("");
            }
        }
    }

    public String textLimit(String text, int type) {
        if (type == 0) {
            if (text.length() > 80) {
                text = text.substring(0, 80 - 1) + "...";
            }
        } else {
            if (text.length() > 30) {
                text = text.substring(0, 30 - 1) + "...";
            }
        }
        return text;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.panelMap) {
            ConnectivityManager cm = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null &&
                    activeNetwork.isConnectedOrConnecting();
            if (isConnected) {
                if (flMap.getVisibility() == View.GONE && ivPanel.getText() == "Show Map" && isConnectionReliable == true) {
                    findViewById(R.id.panelHide).setVisibility(View.VISIBLE);
                    v.setVisibility(View.INVISIBLE);
                    flMap.setVisibility(View.VISIBLE);
                }else {
                    Toast.makeText(getApplicationContext(), "Connect to a reliable internet connection to view map", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getApplicationContext(), "Internet connection required to view map", Toast.LENGTH_SHORT).show();
            }
        } else if (v.getId() == R.id.btn_refresh) {
            ivPanel.setVisibility(View.VISIBLE);
            ivHide.setVisibility(View.INVISIBLE);
            flMap.setVisibility(View.GONE);
            getUserLocation();
            searchView.setText("");
            isConnectionReliable = false;
        } else if (v.getId() == R.id.panelHide) {
            if (flMap.getVisibility() == View.VISIBLE) {
                findViewById(R.id.panelMap).setVisibility(View.VISIBLE);
                v.setVisibility(View.INVISIBLE);
                flMap.setVisibility(View.GONE);
            }
        } else if (v.getId() == R.id.btn_pol_more) {
            createDialog(0);
        } else if (v.getId() == R.id.btn_fire_more) {
            createDialog(1);
        } else if (v.getId() == R.id.btn_hosp_more) {
            createDialog(2);
        } else if (v.getId() == R.id.btn_amb_more) {
            createDialog(3);
        } else if (v.getId() == R.id.btn_sam_more) {
            createDialog(4);
        } else {
            if (!mHasCalled) {
                Intent intent = new Intent(Intent.ACTION_CALL);
                //Toast.makeText(this, v.getTag().toString(), Toast.LENGTH_SHORT).show();
                intent.setData(Uri.parse("tel:" + v.getTag().toString()));
                startActivity(intent);
            }
            mHasCalled = true;
        }
    }

    protected void createDialog(int tag) {

        LayoutInflater inflater = this.getLayoutInflater();
        View layout = inflater.inflate(R.layout.dialog, null);
        String facility = "";
        switch (tag) {
            case 0:
                facility = "Police stations";
                ((ImageView) layout.findViewById(R.id.btn_call_1)).setImageResource(R.drawable.btn_call_pol);
                ((ImageView) layout.findViewById(R.id.btn_call_2)).setImageResource(R.drawable.btn_call_pol);
                ((ImageView) layout.findViewById(R.id.btn_call_3)).setImageResource(R.drawable.btn_call_pol);
                break;
            case 1:
                facility = "Fire stations";
                ((ImageView) layout.findViewById(R.id.btn_call_1)).setImageResource(R.drawable.btn_call_fire);
                ((ImageView) layout.findViewById(R.id.btn_call_2)).setImageResource(R.drawable.btn_call_fire);
                ((ImageView) layout.findViewById(R.id.btn_call_3)).setImageResource(R.drawable.btn_call_fire);
                break;
            case 2:
                facility = "Medical facilities";
                ((ImageView) layout.findViewById(R.id.btn_call_1)).setImageResource(R.drawable.btn_call_hosp);
                ((ImageView) layout.findViewById(R.id.btn_call_2)).setImageResource(R.drawable.btn_call_hosp);
                ((ImageView) layout.findViewById(R.id.btn_call_3)).setImageResource(R.drawable.btn_call_hosp);
                break;
            case 3:
                facility = "Ambulance services";
                ((ImageView) layout.findViewById(R.id.btn_call_1)).setImageResource(R.drawable.btn_call_amb);
                ((ImageView) layout.findViewById(R.id.btn_call_2)).setImageResource(R.drawable.btn_call_amb);
                ((ImageView) layout.findViewById(R.id.btn_call_3)).setImageResource(R.drawable.btn_call_amb);
                break;
            case 4:
                facility = "Service centers";
                ((ImageView) layout.findViewById(R.id.btn_call_1)).setImageResource(R.drawable.btn_call_sam);
                ((ImageView) layout.findViewById(R.id.btn_call_2)).setImageResource(R.drawable.btn_call_sam);
                ((ImageView) layout.findViewById(R.id.btn_call_3)).setImageResource(R.drawable.btn_call_sam);
                break;
        }

        if(isUsingAWS){
            try {
                for (int i = 1; i < 4; i++) {
                    JSONObject object = null;
                    if (facility.toLowerCase().contains("police")) {
                        object = mPoliceList.get(i);
                    } else if (facility.toLowerCase().contains("fire")) {
                        object = mFireDeptList.get(i);
                    } else if (facility.toLowerCase().contains("ambulance")) {
                        object = mAmbulanceList.get(i);
                    } else if (facility.toLowerCase().contains("medical")) {
                        object = mHospitalList.get(i);
                    }
                    if (object != null) {
                        String number = object.getString("number");
                        String name = object.getString("name");
                        String address = object.getString("address");
                        String cleanNumber = object.getString("number");
                        switch (i) {
                            case 1:
                                ((TextView) layout.findViewById(R.id.txtItem1Name)).setText(name);
                                ((TextView) layout.findViewById(R.id.txtItem1Distance)).setText(Html.fromHtml("<i> " + String.format("%.2f", getDistance(object.getDouble("latitude"), object.getDouble("longitude"))) + " km away</i>"));
                                ((TextView) layout.findViewById(R.id.txtItem1Info)).setText(textLimit(address, 0));
                                ((TextView) layout.findViewById(R.id.txtItem1Number)).setText(textLimit(number, 1));
                                layout.findViewById(R.id.item1).setTag(cleanNumber);
                                layout.findViewById(R.id.item1).setOnClickListener(this);
                                break;
                            case 2:
                                ((TextView) layout.findViewById(R.id.txtItem2Name)).setText(name);
                                ((TextView) layout.findViewById(R.id.txtItem2Distance)).setText(Html.fromHtml("<i> " + String.format("%.2f", getDistance(object.getDouble("latitude"), object.getDouble("longitude"))) + " km away</i>"));
                                ((TextView) layout.findViewById(R.id.txtItem2Info)).setText(textLimit(address, 0));
                                ((TextView) layout.findViewById(R.id.txtItem2Number)).setText(textLimit(number, 1));
                                layout.findViewById(R.id.item2).setTag(cleanNumber);
                                layout.findViewById(R.id.item2).setOnClickListener(this);
                                break;
                            case 3:
                                ((TextView) layout.findViewById(R.id.txtItem3Name)).setText(name);
                                ((TextView) layout.findViewById(R.id.txtItem3Distance)).setText(Html.fromHtml("<i> " + String.format("%.2f", getDistance(object.getDouble("latitude"), object.getDouble("longitude"))) + " km away</i>"));
                                ((TextView) layout.findViewById(R.id.txtItem3Info)).setText(textLimit(address, 0));
                                ((TextView) layout.findViewById(R.id.txtItem3Number)).setText(textLimit(number, 1));
                                layout.findViewById(R.id.item3).setTag(cleanNumber);
                                layout.findViewById(R.id.item3).setOnClickListener(this);
                                break;
                        }
                    }
                }
            } catch (Exception ex){
                ex.printStackTrace();
            }
        }else {
            for (int i = 1; i < 4; i++) {
                Cursor top = databaseHelper.getContact(topIds[tag][i]);
                if (top != null)
                    if (top.moveToFirst()) {
                        String number = top.getString(top.getColumnIndex(DatabaseHelper._NUMBER));
                        String name = (top.getString(top.getColumnIndex(DatabaseHelper._NAME)) != null) ? top.getString(top.getColumnIndex(DatabaseHelper._NAME)) : "";
                        String address = ((top.getString(top.getColumnIndex(DatabaseHelper._ADDRESS)) != null) ? top.getString(top.getColumnIndex(DatabaseHelper._ADDRESS)) : "");
                        String cleanNumber = number.split("[/]", 2)[0];
                        cleanNumber = cleanNumber.replaceAll("[\\p{P}\\p{S}]", "");
                        cleanNumber = cleanNumber.replaceAll("[ ]", "");
                        switch (i) {
                            case 1:
                                ((TextView) layout.findViewById(R.id.txtItem1Name)).setText(name);
                                ((TextView) layout.findViewById(R.id.txtItem1Distance)).setText(Html.fromHtml("<i> " + String.format("%.2f", topDistance[tag][1] / 1000) + " km away</i>"));
                                ((TextView) layout.findViewById(R.id.txtItem1Info)).setText(textLimit(address, 0));
                                ((TextView) layout.findViewById(R.id.txtItem1Number)).setText(textLimit(number, 1));
                                layout.findViewById(R.id.item1).setTag(cleanNumber);
                                layout.findViewById(R.id.item1).setOnClickListener(this);
                                break;
                            case 2:
                                ((TextView) layout.findViewById(R.id.txtItem2Name)).setText(name);
                                ((TextView) layout.findViewById(R.id.txtItem2Distance)).setText(Html.fromHtml("<i> " + String.format("%.2f", topDistance[tag][2] / 1000) + " km away</i>"));
                                ((TextView) layout.findViewById(R.id.txtItem2Info)).setText(textLimit(address, 0));
                                ((TextView) layout.findViewById(R.id.txtItem2Number)).setText(textLimit(number, 1));
                                layout.findViewById(R.id.item2).setTag(cleanNumber);
                                layout.findViewById(R.id.item2).setOnClickListener(this);
                                break;
                            case 3:
                                ((TextView) layout.findViewById(R.id.txtItem3Name)).setText(name);
                                ((TextView) layout.findViewById(R.id.txtItem3Distance)).setText(Html.fromHtml("<i> " + String.format("%.2f", topDistance[tag][3] / 1000) + " km away</i>"));
                                ((TextView) layout.findViewById(R.id.txtItem3Info)).setText(textLimit(address, 0));
                                ((TextView) layout.findViewById(R.id.txtItem3Number)).setText(textLimit(number, 1));
                                layout.findViewById(R.id.item3).setTag(cleanNumber);
                                layout.findViewById(R.id.item3).setOnClickListener(this);
                                break;
                        }
                    }
                top.close();
            }
        }


        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout)
                .setTitle(facility + " near you")
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .show();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }


    // Download Stuff

    // declare the dialog as a member field of your activity
    ProgressDialog mProgressDialog;

    // instantiate it within the onCreate method
    // onCreate -


    // execute this when the downloader must be fired

    /*
    onClick sync
*/
    class DownloadTask extends AsyncTask<String, Integer, String> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected String doInBackground(String... sUrl) {
            InputStream input = null;
            OutputStream output = null;
            HttpURLConnection connection = null;
            try {
                URL url = new URL(sUrl[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                output = new FileOutputStream("/data/data/net.samsung.emergencybff/databases/bff_bundle.db");

                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                return e.toString();
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            mProgressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }

        @Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            mProgressDialog.dismiss();
            if (result != null)
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            else
                Toast.makeText(context, "File downloaded", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isAvailable()
                && cm.getActiveNetworkInfo().isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public double getDistance(double latitude, double longitude){
        double dist = Math.sqrt((mLong - longitude) * (mLong - longitude) + (mLat - latitude) * (mLat - latitude));
        return dist;
    }
}

