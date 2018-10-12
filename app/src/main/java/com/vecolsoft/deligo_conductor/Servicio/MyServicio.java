package com.vecolsoft.deligo_conductor.Servicio;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

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
