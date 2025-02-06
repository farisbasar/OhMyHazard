package com.example.ohmyhazard;

import com.google.firebase.database.IgnoreExtraProperties;

@IgnoreExtraProperties
public class User {
    private String id;
    private String name;
    private String title;
    private String location;
    private String time;
    private String date;

    private double latitude;
    private double longitude;

    public User(){
    }

    public User(String id, String name, String title, String location, String time, String date){
        this.id = id;
        this.name = name;
        this.title = title;
        this.location = location;
        this.time = time; // Set date and time
        this.date = date;

        if (location != null && !location.isEmpty()) {
            String[] locationArray = location.split(",");
            if (locationArray.length == 2) {
                this.latitude = Double.parseDouble(locationArray[0].trim());
                this.longitude = Double.parseDouble(locationArray[1].trim());
            }
        }
    }

    public String getTitle() {
        return title;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getTime() {
        return time;
    }

    public String getDate() {
        return date;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }


    public void setTime(String time) {
        this.time = time;
    }

    public void setDate(String date) {
        this.date = date;
    }

}

