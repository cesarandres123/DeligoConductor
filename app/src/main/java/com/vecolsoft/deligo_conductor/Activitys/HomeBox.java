package com.vecolsoft.deligo_conductor.Activitys;


import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.SwitchCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.vecolsoft.deligo_conductor.Common.Common;
import com.vecolsoft.deligo_conductor.Modelo.DataMessage;
import com.vecolsoft.deligo_conductor.Modelo.FCMResponse;
import com.vecolsoft.deligo_conductor.Modelo.Token;
import com.vecolsoft.deligo_conductor.R;
import com.vecolsoft.deligo_conductor.Remote.IFCMService;
import com.vecolsoft.deligo_conductor.Servicio.MyServicio;
import com.vecolsoft.deligo_conductor.Utils.InternetConnection;
import com.vecolsoft.deligo_conductor.Utils.Utils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class HomeBox extends AppCompatActivity {

    //Sistema de localisacion
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    private LocationRequest mLocationRequest;
    private LocationManager locationManager;
    private static final int MY_PERMISSION_REQUEST_CODE = 7000;
    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;
    Geocoder geocoder;

    //SharedPreferences para el swicht
    private SharedPreferences prefs;

    //elementos del main
    private CircleImageView circleImageView;
    private LinearLayout perfil;
    private TextView nombre;
    private RatingBar Rate;
    SwitchCompat location_switch;
    /////////////////////////////////

    IFCMService mfcmService;


    //elementos
    private TextView txtLocation;
    private LinearLayout entregado;
    private LinearLayout llamarCustomer;
    private LinearLayout verRuta;


    DatabaseReference drivers;
    GeoFire geoFire;



    //Contexto
    private Context c = this;

    //verificar internet
    boolean connected = false;

    //Sistema de precencia
    DatabaseReference OnlineRef, CurrentUserRef;

    private static final int INTERVALO = 2000; //2 segundos para salir
    private long tiempoPrimerClick;

    //sistema de seguimiento
    double riderlng = 1.0;
    double riderlat = 1.0;
    String customerId;
    public String street;
    public String sid_customer;
    public String simg_customer;
    public String phone_customer;

    ///////////////Voy acomodando lo nuevo

    private TextView ServicioTXV;
    private TextView SinServicioTXV;
    private LinearLayout Recojida;
    private LinearLayout botonesLayout;
    private CircleImageView img_customer;
    private TextView id_customer;
    private TextView telefono_customer;
    private LinearLayout recogido;
    private CardView punto_encuentro;


    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        //Bolquear rotacion
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        //Sistema de precencia
        OnlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected");
        CurrentUserRef = FirebaseDatabase.getInstance().getReference(Common.driver_tbl)
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        OnlineRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //cuando removemos el valor el conductor se desconecta
                CurrentUserRef.onDisconnect().removeValue();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        //      Fuente
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());

        setContentView(R.layout.activity_home_box);


        mfcmService = Common.getFCMService();

        prefs = getSharedPreferences("datos", Context.MODE_PRIVATE);


        entregado = (LinearLayout) findViewById(R.id.entregado);
        entregado.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder dialogo = new AlertDialog.Builder(HomeBox.this);
                dialogo.setTitle("¿Has llegado al punto de encuentro?");
                dialogo.setCancelable(false);
                dialogo.setIcon(R.drawable.ic_check);

                dialogo.setPositiveButton("SI", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        EnviarNotificacionDeFinalizado(customerId);
                        OnServicioFinish();
                    }
                });

                dialogo.setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                dialogo.show();

            }
        });

        //perfil
        perfil = (LinearLayout) findViewById(R.id.perfil_layout);
        perfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HomeBox.this, PerfilActivity.class));
            }
        });

        //CircleImageView
        circleImageView = (CircleImageView) findViewById(R.id.img_main);
        //nombre
        nombre = (TextView) findViewById(R.id.nombre_main);

        if (Common.CurrentUser.getName() != null) {
            nombre.setText(Common.CurrentUser.getName());
        }

        //load Avatar
        if (Common.CurrentUser.getAvatarUrl() != null && !TextUtils.isEmpty(Common.CurrentUser.getAvatarUrl())) {

            Glide.with(this)
                    .load(Common.CurrentUser.getAvatarUrl())
                    .into(circleImageView);
        }

        Rate = (RatingBar) findViewById(R.id.ratingbar_main);
        float rating = Float.parseFloat(Common.CurrentUser.getRates().replace(",", "."));
        Rate.setRating(rating);


        //Location switch
        location_switch = (SwitchCompat) findViewById(R.id.switch_Location_main);

        //location TextView
        img_customer = (CircleImageView) findViewById(R.id.img_customer);
        id_customer = (TextView) findViewById(R.id.id_customer);
        telefono_customer = (TextView) findViewById(R.id.telefono_customer);

        txtLocation = (TextView) findViewById(R.id.txtLocation_main);
        ServicioTXV = (TextView) findViewById(R.id.servicioTXV);
        Recojida = (LinearLayout) findViewById(R.id.Recojida);
        SinServicioTXV = (TextView) findViewById(R.id.SinServicioTXV);
        botonesLayout = (LinearLayout) findViewById(R.id.botonesLayout);
        punto_encuentro = (CardView) findViewById(R.id.punto_encuentro);


        //obtiene valor.
        boolean estado_switch = Utils.getValuePreference(prefs);
        //asignar valor
        location_switch.setChecked(estado_switch);
        ChangeColorBorderimage();

        if (Common.OnSERVICIO != null) {
            location_switch.setClickable(false);
        }

        location_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isOnline) {

                //guardar valor
                saveValuePreference(getApplicationContext(), isOnline);

                if (isOnline) {
                    CheckGPSStatus();
                    FirebaseDatabase.getInstance().goOnline(); // conectado
                    circleImageView.setBorderColor(getResources().getColor(R.color.colorON));

                    if (ActivityCompat.checkSelfPermission(HomeBox.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat.checkSelfPermission(HomeBox.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    buildLocationCallBack();
                    buildLocationRequest();
                    fusedLocationProviderClient.requestLocationUpdates(mLocationRequest, locationCallback, Looper.myLooper());

                    Toast.makeText(HomeBox.this, "SERVICIO ACTIVO", Toast.LENGTH_SHORT).show();
                    startService(new Intent(c, MyServicio.class));
                    SinServicioTXV.setText("Esperando solicitud de servicio");


                } else {
                    Toast.makeText(HomeBox.this, "SERVICIO INACTIVO", Toast.LENGTH_SHORT).show();
                    FirebaseDatabase.getInstance().goOffline(); //Desconectado
                    circleImageView.setBorderColor(getResources().getColor(R.color.colorOFF));

                    fusedLocationProviderClient.removeLocationUpdates(locationCallback);

                    txtLocation.setText("Sin ubucacion.");
                    stopService(new Intent(c, MyServicio.class));
                    SinServicioTXV.setText("Servicio detenido");


                }

            }
        });

        CheckGPSStatus();
        verificarinternet();


        //GeoFire
        drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        geoFire = new GeoFire(drivers);


        //SIstema de servicio
        if (getIntent() != null) {

            riderlat = getIntent().getDoubleExtra("lat", -1.0);
            riderlng = getIntent().getDoubleExtra("lng", -1.0);
            customerId = getIntent().getStringExtra("customerId");
            street = getIntent().getStringExtra("direccion");
            sid_customer = getIntent().getStringExtra("sid_customer");
            simg_customer = getIntent().getStringExtra("simg_customer");
            phone_customer = getIntent().getStringExtra("phone_customer");

        }

        if (Common.OnSERVICIO != null) {

            Toast.makeText(this, "En Servicio.", Toast.LENGTH_SHORT).show();

            //getRoute(originPosition, destinationPosition);
            NotificacionDeArrivo();

            //load Avatar
            Glide.with(this)
                    .load(simg_customer)
                    .into(img_customer);

            id_customer.setText(sid_customer);
            telefono_customer.setText(phone_customer);

            Recojida.setVisibility(View.VISIBLE);
            SinServicioTXV.setVisibility(View.INVISIBLE);

            botonesLayout.setVisibility(View.GONE);

            if (street != null) {
                ServicioTXV.setText(street);
            }


        }


    }

    private void EnviarNotificacionDeArribo(String customerId) {

        Token token = new Token(customerId);
        //Notification notification = new Notification("Esta aqui!",String.format("El conductor %s ha llegado.",Common.CurrentUser.getName()));
        //Sender sender = new Sender(token.getToken(), notification);

        Map<String, String> content = new HashMap<>();
        content.put("title", "Esta aqui!");
        content.put("message", String.format("El conductor %s ha llegado.", Common.CurrentUser.getName()));
        DataMessage dataMessage = new DataMessage(token.getToken(), content);

        mfcmService.sendMessage(dataMessage).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if (response.body().success != 1) {
                    Toast.makeText(HomeBox.this, "Error.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {

            }
        });
    }

    private void EnviarNotificacionDeFinalizado(String customerId) {
        Token token = new Token(customerId);
        //Notification notification = new Notification("Esta aqui!",String.format("El conductor %s ha llegado.",Common.CurrentUser.getName()));
        //Sender sender = new Sender(token.getToken(), notification);

        Map<String, String> content = new HashMap<>();
        content.put("title", "Completado");
        content.put("message", customerId);
        DataMessage dataMessage = new DataMessage(token.getToken(), content);

        mfcmService.sendMessage(dataMessage).enqueue(new Callback<FCMResponse>() {
            @Override
            public void onResponse(Call<FCMResponse> call, Response<FCMResponse> response) {
                if (response.body().success != 1) {
                    Toast.makeText(HomeBox.this, "Error.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<FCMResponse> call, Throwable t) {

            }
        });
    }

    private void OnServicioFinish() {

        location_switch.setClickable(true);
        Common.OnSERVICIO = null;


        if (location_switch.isChecked()) {
            displayLocation();
        }

        Recojida.setVisibility(View.INVISIBLE);
        SinServicioTXV.setVisibility(View.VISIBLE);
        botonesLayout.setVisibility(View.VISIBLE);
    }

    private void updateFirebaseToken() {

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.token_tbl);

        Token token = new Token(FirebaseInstanceId.getInstance().getToken());
        tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(token);

    }

    private void NotificacionDeArrivo() {
        if (Common.OnSERVICIO != null) {

            //Sistema de notificar arribo de conductor
            geoFire = new GeoFire(FirebaseDatabase.getInstance().getReference(Common.driver_tbl));
            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(riderlat, riderlng), 0.05f);
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {

                    entregado.setVisibility(View.VISIBLE);
                    EnviarNotificacionDeArribo(customerId);
                    Toast.makeText(HomeBox.this, "Has llegado.", Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onKeyExited(String key) {

                }

                @Override
                public void onKeyMoved(String key, GeoLocation location) {

                }

                @Override
                public void onGeoQueryReady() {

                }

                @Override
                public void onGeoQueryError(DatabaseError error) {

                }
            });

        }
    }

    private void displayLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        fusedLocationProviderClient.getLastLocation()
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(android.location.Location location) {
                        Common.MyLocation = location;

                        if (Common.MyLocation != null) {
                            if (location_switch.isChecked()) {

                                final double latitude = Common.MyLocation.getLatitude();
                                final double longitude = Common.MyLocation.getLongitude();

                                //update to firebase
                                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                                    @Override
                                    public void onComplete(String key, DatabaseError error) {

                                    }
                                });

                                try {
                                    getLocation();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }


                            }
                        } else {
                            Log.d("ERROR", "No se puede obtener la localisacion.");
                        }

                    }
                });
    }

    private void setupLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //Request Permissions
            ActivityCompat.requestPermissions(this, new String[]{

                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_CODE);
        } else {

            buildLocationRequest();
            buildLocationCallBack();
            if (location_switch.isChecked()) {
                displayLocation();
            }
        }

    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                for (android.location.Location location : locationResult.getLocations()) {
                    Common.MyLocation = location;
                }
                displayLocation();

            }
        };

    }

    private void buildLocationRequest() {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Common.OnSERVICIO = null;
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.Opciones:
                Toast.makeText(c, "Hola mundo :*", Toast.LENGTH_SHORT).show();
                break;
            case R.id.Salir:

                AlertDialog.Builder dialogo = new AlertDialog.Builder(this);
                dialogo.setTitle("¿Cerrar secion?");
                dialogo.setCancelable(false);
                dialogo.setIcon(R.drawable.ic_exit);

                dialogo.setPositiveButton("Cerrar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Utils.removesharedpreferencies(prefs);
                        stopService(new Intent(c, MyServicio.class));
                        logout();
                    }
                });
                dialogo.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

                dialogo.show();
                break;

        }

        return true;
    }

    ///////////////// MIOS

    private void verificarinternet() {
        //verificar internet
        if (InternetConnection.checkConnection(c)) {
            // Its Available...

            //verificar si hay internet con ping
            if (InternetConnection.internetIsConnected(c)) {
                setupLocation();
                connected = true;
                updateFirebaseToken();

            } else {

                AlertDialog.Builder dialogo = new AlertDialog.Builder(this);
                dialogo.setTitle("Error de coneccion.");
                dialogo.setMessage("Fue imposible establecer una coneccion a internet");
                dialogo.setCancelable(false);
                dialogo.setIcon(R.drawable.ic_error);

                dialogo.setPositiveButton("Reintentar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                        startActivity(getIntent());
                    }
                });

                dialogo.show();

            }

        } else {
            // Not Available...
            connected = false;

            AlertDialog.Builder dialogo = new AlertDialog.Builder(this);
            dialogo.setTitle("Error de coneccion.");
            dialogo.setMessage("Fue imposible establecer una coneccion a internet");
            dialogo.setCancelable(false);
            dialogo.setIcon(R.drawable.ic_error);

            dialogo.setPositiveButton("Reintentar", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                    startActivity(getIntent());
                }
            });

            dialogo.show();

        }
    }

    private void getLocation() throws IOException {

        final double latitude = Common.MyLocation.getLatitude();
        final double longitude = Common.MyLocation.getLongitude();

        List<Address> addresses;
        geocoder = new Geocoder(this, Locale.getDefault());

        addresses = geocoder.getFromLocation(latitude, longitude, 1);

        String txtLocationn = addresses.get(0).getAddressLine(0);

        txtLocation.setText(txtLocationn);
    }

    private void logout() {

        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void saveValuePreference(Context context, boolean valor) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("estado_switch", valor);
        editor.apply();
    }

    public void ChangeColorBorderimage() {

        if (location_switch.isChecked()) {
            circleImageView.setBorderColor(getResources().getColor(R.color.colorON));
            SinServicioTXV.setText("Esperando solicitud de servicio");

        } else {
            circleImageView.setBorderColor(getResources().getColor(R.color.colorOFF));
            SinServicioTXV.setText("Servicio detenido");

        }
    }

    public void CheckGPSStatus() {

        //verificar si el gps esta activo
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertNoGps();
        }
    }

    private void AlertNoGps() {

        AlertDialog.Builder dialogo = new AlertDialog.Builder(this);
        dialogo.setTitle("El sistema GPS esta desactivado, ¿Desea activarlo?");
        dialogo.setCancelable(false);
        dialogo.setIcon(R.drawable.ic_location_disabled);

        dialogo.setPositiveButton("SI", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        });
        dialogo.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        dialogo.show();
    }

    @Override
    public void onBackPressed() {
        if (tiempoPrimerClick + INTERVALO > System.currentTimeMillis()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        } else {
            Toast.makeText(this, "Vuelve a presionar para salir", Toast.LENGTH_SHORT).show();
        }
        tiempoPrimerClick = System.currentTimeMillis();
    }
}