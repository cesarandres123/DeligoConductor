package com.vecolsoft.deligo_conductor.Activitys;

import android.content.Context;
import android.content.Intent;
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

import java.text.DecimalFormat;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Llamada extends AppCompatActivity {

    TextView txtTime, txtDistance, txtAddress;
    AppCompatButton btnAceptar,btnCancelar;

    CircleImageView circleImageView;

    MediaPlayer mediaPlayer;

    IFCMService mFCMService;

    //////////Elementos de JsonParsing
    GetGson mGetGson;

    Context c = this;

    String CustomerId;

    double lat,lng;

    private static final int INTERVALO = 2000; //2 segundos para salir
    private long tiempoPrimerClick;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_llamada);

        mFCMService = Common.getFCMService();
        mGetGson = Common.getGson();


        //intviews

        txtTime = (TextView) findViewById(R.id.txtTime);
        txtDistance = (TextView) findViewById(R.id.txtDistance);
        txtAddress = (TextView) findViewById(R.id.txtAddress);
        circleImageView = (CircleImageView) findViewById(R.id.map_image);

        btnAceptar = (AppCompatButton) findViewById(R.id.btnAccept);
        btnCancelar = (AppCompatButton) findViewById(R.id.btnDecline);

        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(CustomerId)) {
                    cancelBooking(CustomerId);
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
            CustomerId = getIntent().getStringExtra("customer");

            getDirection(lat, lng);
        }
    }

    private void cancelBooking(String customerId) {
        Token token = new Token(CustomerId);

        Notification notification = new Notification("","El conductor ha cancelado tu solicitud.");
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

    private void getDirection(double lat, double lng) {

        String requestApi = null;
        try {

            requestApi = "https://api.mapbox.com/directions/v5/mapbox/cycling/" +
                    Common.MyLocation.getLongitude() + "," + Common.MyLocation.getLatitude() + ";" +
                    lng + "," + lat +
                    "?steps=" + "true" +
                    "&alternatives=" + "true" +
                    "&access_token=" + getResources().getString(R.string.access_token);

            Log.d("vencolsoft", requestApi); //print url for debug

            mGetGson.getPath(requestApi)
                    .enqueue(new Callback<String>() {
                        @Override
                        public void onResponse(Call<String> call, Response<String> response) {

                            try {
                                JSONObject jsonObject = new JSONObject(response.body().toString());

                                JSONArray routes = jsonObject.getJSONArray("routes");

                                JSONObject object = routes.getJSONObject(0);

                                JSONArray legs = object.getJSONArray("legs");
                                JSONObject legsObject = legs.getJSONObject(0);

//                                Get Distancec
                                double distance = legsObject.getDouble("distance");
                                DecimalFormat df = new DecimalFormat("#.0");
                                txtDistance.setText(df.format(distance) + " Km");

//                                Get Time
                                int time = legsObject.getInt("duration");
                                int hor,min,seg;
                                hor=time/3600;
                                min=(time-(3600*hor))/60;
                                seg=time-((hor*3600)+(min*60));
                                txtTime.setText(hor+"h "+min+"m "+seg+"s");

//                                Get Address
                                String address = legsObject.getString("summary");
                                txtAddress.setText(address);

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onFailure(Call<String> call, Throwable t) {
                            Toast.makeText(c, "" + t.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            super.onBackPressed();
            return;
        }else {
            Toast.makeText(this, "Vuelve a presionar para cancelar", Toast.LENGTH_SHORT).show();
        }
        tiempoPrimerClick = System.currentTimeMillis();
    }
}
