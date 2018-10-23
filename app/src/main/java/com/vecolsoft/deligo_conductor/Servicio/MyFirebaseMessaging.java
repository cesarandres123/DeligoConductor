package com.vecolsoft.deligo_conductor.Servicio;

import android.content.Intent;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.vecolsoft.deligo_conductor.Activitys.Llamada;

public class MyFirebaseMessaging extends FirebaseMessagingService {


    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (remoteMessage.getData() != null) {

            LatLng customer_location = new Gson().fromJson(remoteMessage.getNotification().getBody(), LatLng.class);
            Intent intent = new Intent(getBaseContext(), Llamada.class);
            intent.putExtra("lat", customer_location.getLatitude());
            intent.putExtra("lng", customer_location.getLongitude());
            intent.putExtra("customer", remoteMessage.getNotification().getTitle());
            startActivity(intent);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        }
    }
}
