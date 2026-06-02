package com.example.epostats.models;

import com.google.gson.annotations.SerializedName;

public class Player {
    @SerializedName("id")
    private int id;

    @SerializedName("name")
    private String name;

    @SerializedName("position")
    private String position;

    private boolean isStartingEleven = false;

    public int getId() { return id; }
    public String getName() { return name; }
    public String getPosition() { return position; }
    public boolean isStartingEleven() { return isStartingEleven; }
    public void setStartingEleven(boolean startingEleven) { isStartingEleven = startingEleven; }
}