package com.example.epostats.models;


import com.google.gson.annotations.SerializedName;

public class Championship {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    public int getId() { return id; }
    public String getName() { return name; }
    //public String getPosition() { return position; }
    //public boolean isStartingEleven() { return isStartingEleven; }
    //public void setStartingEleven(boolean startingEleven) { isStartingEleven = startingEleven; }
}
