package ru.gdgkazan.simpleweather.data.model;

import android.support.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

/**
 * @author Artur Vasilov
 */
public class Weather implements Serializable {

    @SerializedName("main")
    private String mMain;

    @SerializedName("icon")
    private String mIcon;

    public Weather(String main){
        mMain = main;
    }

    @NonNull
    public String getMain() {
        return mMain;
    }

    public void setMain(@NonNull String main) {
        mMain = main;
    }

    @NonNull
    public String getIcon() {
        return mIcon;
    }

    public void setIcon(@NonNull String icon) {
        mIcon = icon;
    }
}
