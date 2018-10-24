package com.vecolsoft.deligo_conductor.Servicio;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.google.gson.Gson;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.vecolsoft.deligo_conductor.Activitys.HomeBox;
import com.vecolsoft.deligo_conductor.Activitys.Llamada;
import com.vecolsoft.deligo_conductor.Common.Common;
import com.vecolsoft.deligo_conductor.R;

import java.util.HashMap;
import java.util.Map;

public class MyFirebaseMessaging extends FirebaseMessagingService {

    double lat;
    double lgn;
    String Customer;

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        if (remoteMessage.getData() != null) {

            Map<String, String> data = remoteMessage.getData();
            String customer = data.get("customer");
            String LAT = data.get("lat");
            String LNG = data.get("lng");

            lat = Double.parseDouble( LAT.replace(",",".") );
            lgn = Double.parseDouble( LNG.replace(",",".") );
            Customer = customer;

            //si esta en segundo plano
            //sendNotification(remoteMessage);

            Intent intent = new Intent(getBaseContext(), Llamada.class);
            intent.putExtra("lat", lat);
            intent.putExtra("lng", lgn);
            intent.putExtra("customer", customer);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }

    private void sendNotification(RemoteMessage remoteMessage) {

        if (remoteMessage.getData() != null) {

            RemoteMessage.Notification notification = remoteMessage.getNotification();
            Intent intent = new Intent(this, HomeBox.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT);

            Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.ic_moto)
                    .setContentTitle(Customer)
                    .setContentText(lat + " " + lgn)
                    .setAutoCancel(true)
                    .setSound(defaultSoundUri)
                    .setContentIntent(pendingIntent);

            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            notificationManager.notify(0, notificationBuilder.build());
        }

    }

}
