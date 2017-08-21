
package ru.gdgkazan.simpleweather.data.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class SetCityWeather {

    @SerializedName("cnt")
    @Expose
    private Integer cnt;
    @SerializedName("list")
    @Expose
    private java.util.List<ru.gdgkazan.simpleweather.data.model.List> list = null;

    public Integer getCnt() {
        return cnt;
    }

    public void setCnt(Integer cnt) {
        this.cnt = cnt;
    }

    public java.util.List<ru.gdgkazan.simpleweather.data.model.List> getList() {
        return list;
    }

    public void setList(java.util.List<ru.gdgkazan.simpleweather.data.model.List> list) {
        this.list = list;
    }

}
