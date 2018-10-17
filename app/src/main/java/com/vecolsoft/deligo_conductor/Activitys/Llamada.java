package com.vecolsoft.deligo_conductor.Activitys;

import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.vecolsoft.deligo_conductor.Common.Common;
import com.vecolsoft.deligo_conductor.Modelo.DataMessage;
import com.vecolsoft.deligo_conductor.Modelo.FCMResponse;
import com.vecolsoft.deligo_conductor.Modelo.Notification;
import com.vecolsoft.deligo_conductor.Modelo.Sender;
import com.vecolsoft.deligo_conductor.Modelo.Token;
import com.vecolsoft.deligo_conductor.R;
import com.vecolsoft.deligo_conductor.Remote.GetGson;
import com.vecolsoft.deligo_conductor.Remote.IFCMService;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Llamada extends AppCompatActivity {

    TextView txtDistance, txtAddress;
    AppCompatButton btnAceptar,btnCancelar;


    MediaPlayer mediaPlayer;

    IFCMService mFCMService;

    String customerId;

    double lat,lng;

    private static final int INTERVALO = 2000; //2 segundos para salir
    private long tiempoPrimerClick;

    //geocider location
    Geocoder geocoder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_llamada);

        mFCMService = Common.getFCMService();


        //intviews
        txtDistance = (TextView) findViewById(R.id.txtDistance);
        txtAddress = (TextView) findViewById(R.id.txtAddress);

        btnAceptar = (AppCompatButton) findViewById(R.id.btnAccept);
        btnCancelar = (AppCompatButton) findViewById(R.id.btnDecline);

        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(customerId)) {
                    cancelBooking(customerId);
                    Common.OnSeguimiento = null;
                }

            }
        });

        btnAceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Llamada.this,HomeBox.class);

                //pasar los datos del cliente
                intent.putExtra("lat",lat);
                intent.putExtra("lng",lng);
                intent.putExtra("customerId",customerId);

                Common.OnSeguimiento = true;

                startActivity(intent);
                finish();
            }
        });

        mediaPlayer = MediaPlayer.create(this, R.raw.betelgeuse);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        if (getIntent() != null) {

             lat = getIntent().getDoubleExtra("lat", -1.0);
             lng = getIntent().getDoubleExtra("lng", -1.0);
            customerId = getIntent().getStringExtra("customer");

            try {
                getDirection(lat, lng);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void cancelBooking(String customerId) {
        Token token = new Token(customerId);

        Notification notification = new Notification("Cancel","El conductor ha cancelado tu solicitud.");
        Sender sender = new Sender(token.getToken(),notification);

        mFCMService.sendMessage(sender)
                .enqueue(new Callback<FCMResponse>() {
                    @Override
                    public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                         if (response.body().success == 1) {
                             Toast.makeText(Llamada.this, "Cancelado.", Toast.LENGTH_SHORT).show();
                             finish();
                         }
                    }

                    @Override
                    public void onFailure(Call<FCMResponse> call, Throwable t) {

                    }
                });
    }
    private void getDirection(double lat, double lng) throws IOException {
        ///Obtener La localisacion
        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        addresses = geocoder.getFromLocation(lat, lng, 1);

        String address = addresses.get(0).getAddressLine(0);

        txtAddress.setText(address);

        //obtener la distancia
        Location loc1 = new Location("");
        loc1.setLatitude(lat);
        loc1.setLongitude(lng);

        Location loc2 = new Location("");
        loc2.setLatitude(Common.MyLocation.getLatitude());
        loc2.setLongitude(Common.MyLocation.getLongitude());

        float distanceInMeters = loc1.distanceTo(loc2);
        DecimalFormat df = new DecimalFormat("#.0");
        txtDistance.setText(df.format(distanceInMeters) + " Metros");

    }

    @Override
    protected void onStop() {
        if (mediaPlayer.isPlaying())
            mediaPlayer.release();
        super.onStop();

    }

    @Override
    protected void onPause() {
        if (mediaPlayer.isPlaying())
            mediaPlayer.pause();
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && !mediaPlayer.isPlaying())
            mediaPlayer.start();
    }

    @Override
    public void onBackPressed(){
        if (tiempoPrimerClick + INTERVALO > System.currentTimeMillis()){
            cancelBooking(customerId);
            super.onBackPressed();
            return;
        }else {
            Toast.makeText(this, "Vuelve a presionar para cancelar", Toast.LENGTH_SHORT).show();
        }
        tiempoPrimerClick = System.currentTimeMillis();
    }
}
