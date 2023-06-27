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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
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
    private RecyclerView recyclerView;
    private WeatherAdapter weatherAdapter;
    private List<Weather> weatherList;

    private static final String[] CITIES = {
            "New York", "Singapore", "Mumbai", "Delhi", "Sydney", "Melbourne"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTextView = findViewById(R.id.locationTextView);
        weatherTextView = findViewById(R.id.weatherTextView);
        recyclerView = findViewById(R.id.cityRecyclerView);

        sharedPreferences = getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE);

        // Retrieve and display the last fetched weather information for the current location
        String savedLocation = sharedPreferences.getString(PREFERENCE_LOCATION_KEY, null);
        if (savedLocation != null) {
            locationTextView.setText(savedLocation);
            weatherTextView.setText(getSavedWeatherText());
        }

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        }

        weatherList = new ArrayList<>();
        weatherAdapter = new WeatherAdapter(weatherList, this);

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(weatherAdapter);

        fetchWeatherDataForCities();
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

        reverseGeocode(latitude, longitude);

        String url = "https://api.openweathermap.org/data/2.5/weather?lat=" + latitude + "&lon=" + longitude + "&appid=" + API_KEY;

        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            JSONArray weatherArray = response.getJSONArray("weather");
                            JSONObject weatherObject = weatherArray.getJSONObject(0);
                            String description = weatherObject.getString("description");

                            JSONObject mainObject = response.getJSONObject("main");
                            float temperature = (float) (mainObject.getDouble("temp") - 273.15); // Convert temperature from Kelvin to Celsius
                            int humidity = mainObject.getInt("humidity");
                            int pressure = mainObject.getInt("pressure");

                            JSONObject windObject = response.getJSONObject("wind");
                            float windSpeed = (float) windObject.getDouble("speed");

                            JSONObject rainObject = response.optJSONObject("rain");
                            float rainVolume = 0.0f;
                            if (rainObject != null && rainObject.has("1h")) {
                                rainVolume = (float) rainObject.getDouble("1h");
                            }

                            JSONObject cloudsObject = response.getJSONObject("clouds");
                            int cloudiness = cloudsObject.getInt("all");

                            String weatherText = "Current weather:\n" +
                                    "Temperature: " + temperature + "°C\n" +
                                    "Humidity: " + humidity + "%\n" +
                                    "Pressure: " + pressure + " hPa\n" +
                                    "Wind Speed: " + windSpeed + " m/s\n" +
                                    "Rain Volume (1h): " + rainVolume + " mm\n" +
                                    "Cloudiness: " + cloudiness + "%";

                            weatherTextView.setText(weatherText);

                            // Save the weather information to SharedPreferences
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(PREFERENCE_LOCATION_KEY, locationTextView.getText().toString());
                            editor.putFloat("temperature", temperature);
                            editor.putInt("humidity", humidity);
                            editor.putInt("pressure", pressure);
                            editor.putFloat("windSpeed", windSpeed);
                            editor.putFloat("rainVolume", rainVolume);
                            editor.putInt("cloudiness", cloudiness);
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

    private void reverseGeocode(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses.size() > 0) {
                Address address = addresses.get(0);
                StringBuilder addressBuilder = new StringBuilder();
                for (int i = 0; i <= address.getMaxAddressLineIndex(); i++) {
                    addressBuilder.append(address.getAddressLine(i));
                    if (i < address.getMaxAddressLineIndex()) {
                        addressBuilder.append(", ");
                    }
                }
                String fullAddress = addressBuilder.toString();
                locationTextView.setText(fullAddress);

                // Save the location to SharedPreferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(PREFERENCE_LOCATION_KEY, fullAddress);
                editor.apply();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void fetchWeatherDataForCities() {
        for (String city : CITIES) {
            String url = "https://api.openweathermap.org/data/2.5/weather?q=" + city + "&appid=" + API_KEY;

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            try {
                                JSONArray weatherArray = response.getJSONArray("weather");
                                JSONObject weatherObject = weatherArray.getJSONObject(0);
                                String description = weatherObject.getString("description");

                                JSONObject mainObject = response.getJSONObject("main");
                                float temperature = (float) (mainObject.getDouble("temp") - 273.15); // Convert temperature from Kelvin to Celsius
                                int humidity = mainObject.getInt("humidity");
                                int pressure = mainObject.getInt("pressure");

                                Weather weather = new Weather(city, description, temperature, humidity, pressure);
                                weatherList.add(weather);
                                weatherAdapter.notifyDataSetChanged();
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
    }

    private String getSavedWeatherText() {
        float savedTemperature = sharedPreferences.getFloat("temperature", 0.0f);
        int savedHumidity = sharedPreferences.getInt("humidity", 0);
        int savedPressure = sharedPreferences.getInt("pressure", 0);
        float savedWindSpeed = sharedPreferences.getFloat("windSpeed", 0.0f);
        float savedRainVolume = sharedPreferences.getFloat("rainVolume", 0.0f);
        int savedCloudiness = sharedPreferences.getInt("cloudiness", 0);

        return "Current weather:\n" +
                "Temperature: " + savedTemperature + "°C\n" +
                "Humidity: " + savedHumidity + "%\n" +
                "Pressure: " + savedPressure + " hPa\n" +
                "Wind Speed: " + savedWindSpeed + " m/s\n" +
                "Rain Volume (1h): " + savedRainVolume + " mm\n" +
                "Cloudiness: " + savedCloudiness + "%";
    }
}
