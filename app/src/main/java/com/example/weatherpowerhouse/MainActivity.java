package com.example.weatherpowerhouse;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String API_KEY = "6dfdcc85199339f6621ce6133e4e8d5b";
    private static final String PREFERENCE_NAME = "WeatherPrefs";
    private static final String PREFERENCE_LOCATION_KEY = "location";

    private LocationManager locationManager;
    private TextView locationTextView;
    private TextView weatherTextView;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTextView = findViewById(R.id.locationTextView);
        weatherTextView = findViewById(R.id.weatherTextView);

        sharedPreferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);
        String savedLocation = sharedPreferences.getString(PREFERENCE_LOCATION_KEY, null);
        if (savedLocation != null) {
            locationTextView.setText(savedLocation);
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
            }
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String cityName = response.getString("name");

                            JSONObject mainObject = response.getJSONObject("main");
                            double temperature = mainObject.getDouble("temp");
                            int humidity = mainObject.getInt("humidity");
                            int pressure = mainObject.getInt("pressure");

                            JSONObject windObject = response.getJSONObject("wind");
                            double windSpeed = windObject.getDouble("speed");

                            JSONArray weatherArray = response.getJSONArray("weather");
                            JSONObject weatherObject = weatherArray.getJSONObject(0);
                            String description = weatherObject.getString("description");

                            JSONObject cloudsObject = response.getJSONObject("clouds");
                            int cloudiness = cloudsObject.getInt("all");

                            double rainVolume = 0.0;
                            if (response.has("rain")) {
                                JSONObject rainObject = response.getJSONObject("rain");
                                if (rainObject.has("1h")) {
                                    rainVolume = rainObject.getDouble("1h");
                                }
                            }

                            double latitude = response.getJSONObject("coord").getDouble("lat");
                            double longitude = response.getJSONObject("coord").getDouble("lon");

                            // Perform reverse geocoding to obtain the full address
                            String address = reverseGeocode(latitude, longitude);

                            String weatherText =
                                    "\nCurrent weather: " + description +
                                    "\nTemperature: " + temperature + "°C" +
                                    "\nHumidity: " + humidity + "%" +
                                    "\nPressure: " + pressure + " hPa" +
                                    "\nWind Speed: " + windSpeed + " m/s" +
                                    "\nRain Volume (1h): " + rainVolume + " mm" +
                                    "\nCloudiness: " + cloudiness + "%";

                            locationTextView.setText(address);
                            weatherTextView.setText(weatherText);

                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(PREFERENCE_LOCATION_KEY, address);
                            editor.apply();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Toast.makeText(MainActivity.this, "Error retrieving weather data", Toast.LENGTH_SHORT).show();
                    }
                });

        RequestQueue queue = Volley.newRequestQueue(this);
        queue.add(request);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }
    private String reverseGeocode(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        String address = "";

        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && addresses.size() > 0) {
                Address fetchedAddress = addresses.get(0);
                StringBuilder addressBuilder = new StringBuilder();

                for (int i = 0; i <= fetchedAddress.getMaxAddressLineIndex(); i++) {
                    addressBuilder.append(fetchedAddress.getAddressLine(i));
                    if (i < fetchedAddress.getMaxAddressLineIndex()) {
                        addressBuilder.append(", ");
                    }
                }

                address = addressBuilder.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return address;
    }

}
