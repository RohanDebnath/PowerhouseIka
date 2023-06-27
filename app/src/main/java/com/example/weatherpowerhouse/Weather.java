package com.example.weatherpowerhouse;

public class Weather {
    private String city;
    private String description;
    private float temperature;
    private int humidity;
    private int pressure;

    public Weather(String city, String description, float temperature, int humidity, int pressure) {
        this.city = city;
        this.description = description;
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;
    }

    public String getCity() {
        return city;
    }

    public String getDescription() {
        return description;
    }

    public float getTemperature() {
        return temperature;
    }

    public int getHumidity() {
        return humidity;
    }

    public int getPressure() {
        return pressure;
    }
}
