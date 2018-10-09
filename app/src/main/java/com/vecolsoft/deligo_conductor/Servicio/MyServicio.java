package com.vecolsoft.deligo_conductor.Servicio;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Switch;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.vecolsoft.deligo_conductor.Activitys.Home;
import com.vecolsoft.deligo_conductor.Common.Common;
import com.vecolsoft.deligo_conductor.R;
import com.vecolsoft.deligo_conductor.Splash.SplashActivity;

public class MyServicio extends Service {

    private Context context = this;
    private static MyServicio mInstance = null;

    public static boolean isServiceCreated() {
        try {
            // If instance was not cleared but the service was destroyed an Exception will be thrown
            return mInstance != null && mInstance.ping();
        } catch (NullPointerException e) {
            // destroyed/not-started
            return false;
        }
    }

    private boolean ping() {
        return true;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // Se construye la notificación
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_motorcycle)
                .setContentTitle("Servicio activo")
                .setContentText("Esperando la solicitud de algun cliente.")
                .setShowWhen(false);

        // Crear Intent para iniciar una actividad al presionar la notificación
        Intent notificationIntent = new Intent(this, SplashActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        builder.setContentIntent(pendingIntent);

        // Poner en primer plano
        startForeground(1, builder.build());


        return START_STICKY;
    }

    @Override
    public void onDestroy() {

    }


}
