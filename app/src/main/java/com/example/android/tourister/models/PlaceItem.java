package com.example.android.tourister.models;

public class PlaceItem {

    private String title;
    private String value;

    public PlaceItem(String title, String value) {
        this.title = title;
        this.value = value;
    }

    public String getTitle() {
        return title;
    }

    public String getValue() {
        return value;
    }
}
