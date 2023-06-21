package com.example.planeradar;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlanesService extends Service {

    private IBinder mBinder = new MyBinder();
    private static final long INTERVAL = 10000;
    private Handler handler;
    private Runnable runnable;
    static double myLat = 50.072324; // y "lat":"50.463976",50.07232440535777, 19.955504753480948
    static double myLon = 19.955504; // x "lon":"19.242999"
    JSONObject radarData;

    NotificationCompat.Builder mBuilder;
    NotificationManager notificationManager;
    NotificationCompat.Builder builder;


    int currentlyVisiblePlanes;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    if (radarData != null) {
                        currentlyVisiblePlanes = radarData.getJSONArray("ac").length();
                    } else {
                        currentlyVisiblePlanes = 0;
                    }
                } catch (JSONException e) {
                    currentlyVisiblePlanes = 0;
                }

                builder.setSmallIcon(R.mipmap.ic_launcher)
                        .setOngoing(true)
                        .setSilent(true)
                        .setOnlyAlertOnce(true)
                        .setContentText("Visible planes: " + currentlyVisiblePlanes);

                notificationManager.notify(10, builder.build());

                getRadarData();

                handler.postDelayed(this, INTERVAL);
            }
        };

        handler.post(runnable);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    @SuppressLint("MissingPermission")
    public void onCreate() {
        super.onCreate();
        mBuilder = new NotificationCompat.Builder(this, "id");

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel mChannel = new NotificationChannel("id", "name", NotificationManager.IMPORTANCE_HIGH);
        mChannel.setShowBadge(true);
        notificationManager.createNotificationChannel(mChannel);
        builder = new NotificationCompat.Builder(PlanesService.this, "id");


        LocationRequest mLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
                .setWaitForAccurateLocation(false)
                .setMinUpdateIntervalMillis(5000)
                .setMaxUpdateDelayMillis(10000)
                .build();


        LocationCallback mLocationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        myLat = location.getLatitude();
                        myLon = location.getLongitude();
                    }
                }
            }
        };
        LocationServices.getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.getMainLooper());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    void getRadarData() {

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {

            StringBuilder response = new StringBuilder();

            System.out.println("Starting");

            try {

                // Tworzenie obiektu URL
                URL url = new URL("https://adsbx-flight-sim-traffic.p.rapidapi.com/api/aircraft/json/lat/" +
                        myLat + "/lon/" + myLon + "/dist/25/");
                // Tworzenie połączenia HttpURLConnection
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                // Ustawienie metody żądania na GET
                connection.setRequestMethod("GET");

                connection.setRequestProperty("X-RapidAPI-Key", "CUT");
                connection.setRequestProperty("X-RapidAPI-Host", "adsbx-flight-sim-traffic.p.rapidapi.com");

                // Odczytanie odpowiedzi
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    String line;


                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    reader.close();
                } else {
                    ;
                }

                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }


            handler.post(() -> {

                try {
                    radarData = new JSONObject(response.toString());
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            });
        });

    }

    public class MyBinder extends Binder {
        PlanesService getService() {
            return PlanesService.this;
        }
    }
}