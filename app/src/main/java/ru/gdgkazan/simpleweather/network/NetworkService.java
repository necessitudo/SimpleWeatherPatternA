package ru.gdgkazan.simpleweather.network;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import ru.arturvasilov.sqlite.core.SQLite;
import ru.arturvasilov.sqlite.core.Where;
import ru.gdgkazan.simpleweather.BuildConfig;
import ru.gdgkazan.simpleweather.data.GsonHolder;
import ru.gdgkazan.simpleweather.data.model.City;
import ru.gdgkazan.simpleweather.data.model.SetCityWeather;
import ru.gdgkazan.simpleweather.data.model.WeatherCity;
import ru.gdgkazan.simpleweather.data.tables.CityTable;
import ru.gdgkazan.simpleweather.data.tables.RequestTable;
import ru.gdgkazan.simpleweather.data.tables.WeatherCityTable;
import ru.gdgkazan.simpleweather.network.model.NetworkRequest;
import ru.gdgkazan.simpleweather.network.model.Request;
import ru.gdgkazan.simpleweather.network.model.RequestStatus;

import static android.content.ContentValues.TAG;

/**
 * @author Artur Vasilov
 */
public class NetworkService extends IntentService {

    private static final String REQUEST_KEY = "request";
    private static final String CITY_NAME_KEY = "city_name";
    public static final String REQUEST_KEY_CITY_LIST = "city_list";

    public static void start(@NonNull Context context, @NonNull Request request, @NonNull String cityName) {
        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra(REQUEST_KEY, GsonHolder.getGson().toJson(request));
        intent.putExtra(CITY_NAME_KEY, cityName);
        context.startService(intent);
    }

    public static void start(@NonNull Context context, @NonNull Request request) {
        Intent intent = new Intent(context, NetworkService.class);
        intent.putExtra(REQUEST_KEY, GsonHolder.getGson().toJson(request));
       // intent.putExtra(REQUEST_KEY_CITY_LIST, cityName);
        context.startService(intent);
    }

    @SuppressWarnings("unused")
    public NetworkService() {
        super(NetworkService.class.getName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Request request = GsonHolder.getGson().fromJson(intent.getStringExtra(REQUEST_KEY), Request.class);
        Request savedRequest = SQLite.get().querySingle(RequestTable.TABLE,
                Where.create().equalTo(RequestTable.REQUEST, request.getRequest()));


        if (TextUtils.equals(NetworkRequest.CITY_WEATHER, request.getRequest())) {

            if (savedRequest != null && request.getStatus() == RequestStatus.IN_PROGRESS) {
                return;
            }

            setStatusRequest(request, RequestStatus.IN_PROGRESS);

            String cityName = intent.getStringExtra(CITY_NAME_KEY);

            executeCityRequest(request, cityName);
        }

        if (TextUtils.equals(NetworkRequest.CITY_LIST, request.getRequest())){

            Log.d("Happy", "onHandleIntent");

            if (savedRequest != null && (request.getStatus() == RequestStatus.IN_PROGRESS_LIST_CITY || request.getStatus() == RequestStatus.IN_PROGRESS_LIST_CITY_WEATHER)) {
                return;
            }

            setStatusRequest(request, RequestStatus.IN_PROGRESS_LIST_CITY);
            executeAllCity(request);

            Log.d("Happy", "IN_PROGRESS_LIST_CITY");

            setStatusRequest(request, RequestStatus.IN_PROGRESS_LIST_CITY_WEATHER);
            executeAllCityWeather(request);

            Log.d("Happy", "IN_PROGRESS_LIST_CITY_WEATHER");
        }
    }


    private void executeCityRequest(@NonNull Request request, @NonNull String cityName) {
        try {
            City city = ApiFactory.getWeatherService(BuildConfig.API_ENDPOINT)
                    .getWeather(cityName)
                    .execute()
                    .body();
            SQLite.get().delete(CityTable.TABLE);
            SQLite.get().insert(CityTable.TABLE, city);
            request.setStatus(RequestStatus.SUCCESS);
        } catch (IOException e) {
            request.setStatus(RequestStatus.ERROR);
            request.setError(e.getMessage());
        } finally {
            SQLite.get().insert(RequestTable.TABLE, request);
            SQLite.get().notifyTableChanged(RequestTable.TABLE);
        }
    }

    private void executeAllCity(Request request) {

        try{

            List<WeatherCity> listWeather = SQLite.get().query(WeatherCityTable.TABLE);

            if(listWeather.size()==0) {

                Log.d("Happy", "prepared to download");
                ResponseBody body = ApiFactory.getWeatherService(BuildConfig.API_ENDPOINT_ALLCITY)
                        .downloadFileWithFixedUrl()
                        .execute()
                        .body();
                if (writeResponseBodyToDisk(body)==true) {
                    writeCityToBase();
                }
             }
        } catch (IOException e) {
            request.setStatus(RequestStatus.ERROR);
            request.setError(e.getMessage());
        } finally {
            SQLite.get().insert(RequestTable.TABLE, request);
            SQLite.get().notifyTableChanged(RequestTable.TABLE);
        }

    }

    private void executeAllCityWeather(@NonNull Request request) {

        //Проверка на то, пуста ли таблица
        List<WeatherCity> listWeather = SQLite.get().query(WeatherCityTable.TABLE);

        String ids = "";


        for (WeatherCity w : listWeather) {

            ids = ids + (!ids.equals("") ? "," : "") + w.getCityId();

        }

        try {
            SetCityWeather setCityWeather = ApiFactory.getWeatherService(BuildConfig.API_ENDPOINT)
                    .getAllWeather(ids)
                    .execute()
                    .body();

            List<ru.gdgkazan.simpleweather.data.model.List> listCity = setCityWeather.getList();
            SQLite.get().delete(CityTable.TABLE);

            for (ru.gdgkazan.simpleweather.data.model.List currentCity : listCity) {

                City city = new City(currentCity.getName(), currentCity.getMain());
                SQLite.get().insert(CityTable.TABLE, city);

            }

            request.setStatus(RequestStatus.SUCCESS);

        } catch (IOException e) {
            request.setStatus(RequestStatus.ERROR);
            request.setError(e.getMessage());
        }finally{
            SQLite.get().insert(RequestTable.TABLE, request);
            SQLite.get().notifyTableChanged(RequestTable.TABLE);
        }


    }

    private boolean writeCityToBase() {

        try{
            SQLite.get().delete(WeatherCityTable.TABLE);//на время отладки
            FileInputStream fstream = new FileInputStream(BuildConfig.PATH_TO_FILE_CITY_LIST);
            BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
            String strLine;
            int countIt = 1;
            while ((strLine = br.readLine()) != null){

                if (countIt==16){
                    break;
                }
                if (countIt!=1){
                    String[] s = strLine.split("\t");
                    WeatherCity weatherCity = new WeatherCity(Integer.parseInt(s[0]), s[1]);
                    SQLite.get().insert(WeatherCityTable.TABLE, weatherCity);

                }
                countIt++;
            }
            SQLite.get().notifyTableChanged(WeatherCityTable.TABLE);
            return true;
        }catch (IOException e){
            return false;
        }

    }

    private boolean writeResponseBodyToDisk(ResponseBody body) {

            try {
                // todo change the file location/name according to your needs
                File futureStudioIconFile = new File(BuildConfig.PATH_TO_FILE_CITY_LIST);

                InputStream inputStream = null;
                OutputStream outputStream = null;

                try {
                    byte[] fileReader = new byte[4096];

                    long fileSize = body.contentLength();
                    long fileSizeDownloaded = 0;

                    inputStream = body.byteStream();
                    outputStream = new FileOutputStream(futureStudioIconFile);

                    while (true) {
                        int read = inputStream.read(fileReader);

                        if (read == -1) {
                            break;
                        }

                        outputStream.write(fileReader, 0, read);

                        fileSizeDownloaded += read;

                        //Log.d(TAG, "file download: " + fileSizeDownloaded + " of " + fileSize);
                    }

                    outputStream.flush();

                    return true;
                } catch (IOException e) {
                    return false;
                } finally {
                    if (inputStream != null) {
                        inputStream.close();
                    }

                    if (outputStream != null) {
                        outputStream.close();
                    }
                }
            } catch (IOException e) {
                return false;
            }
        }

    private void setStatusRequest(Request request, RequestStatus status){

        request.setStatus(status);
        SQLite.get().insert(RequestTable.TABLE, request);
        SQLite.get().notifyTableChanged(RequestTable.TABLE);

    }

}

