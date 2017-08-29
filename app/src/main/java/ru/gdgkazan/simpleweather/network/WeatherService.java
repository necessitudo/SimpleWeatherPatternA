package ru.gdgkazan.simpleweather.network;

import android.support.annotation.NonNull;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;
import ru.gdgkazan.simpleweather.data.model.City;
import ru.gdgkazan.simpleweather.data.model.SetCityWeather;

/**
 * @author Artur Vasilov
 */
public interface WeatherService {

    @GET("data/2.5/weather?units=metric")
    Call<City> getWeather(@NonNull @Query("q") String query);

    @GET("help/city_list.txt")
    Call<ResponseBody> downloadFileWithFixedUrl();

    @GET("data/2.5/group?units=metric")
    Call<SetCityWeather> getAllWeather(@NonNull @Query("id") String query);

}
