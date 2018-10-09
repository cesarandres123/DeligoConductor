package com.vecolsoft.deligo_conductor.Activitys;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.vecolsoft.deligo_conductor.Common.Common;
import com.vecolsoft.deligo_conductor.Modelo.Token;
import com.vecolsoft.deligo_conductor.R;
import com.vecolsoft.deligo_conductor.Servicio.MyServicio;
import com.vecolsoft.deligo_conductor.Utils.InternetConnection;
import com.vecolsoft.deligo_conductor.Utils.Utils;

import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Home extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private SharedPreferences prefs;

    //elementos del toolbar
    private Toolbar toolbar;
    private CircleImageView circleImageView;
    private RelativeLayout perfil;
    /////////////////////////////////

    private GoogleMap mMap;
    Marker mCurrent;

    //Play servicios
    private static final int MI_PERMISION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICE_RES_REQUEST = 7001;
    private LocationRequest mLocationrequest;
    private GoogleApiClient mGoogleApiClient;
    private LocationManager locationManager;

    private static int UPDATE_INTERVAL = 5000;
    private static int FASTED_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    DatabaseReference drivers;
    GeoFire geoFire;


    SwitchCompat location_switch;
    SupportMapFragment mapFragment;

    //Contexto
    private Context c = this;

    //verificar internet
    boolean connected = false;

    //Sistema de precencia
    DatabaseReference OnlineRef,CurrentUserRef;

    private static final int INTERVALO = 2000; //2 segundos para salir
    private long tiempoPrimerClick;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Bolquear rotacion
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);

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

        setContentView(R.layout.activity_home);

        prefs = getSharedPreferences("datos", Context.MODE_PRIVATE);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        //perfil
        perfil = (RelativeLayout) findViewById(R.id.perfil);
        perfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(Home.this, PerfilActivity.class));
                //overridePendingTransition( R.anim.slide_in_up, R.anim.slide_out_up );

            }
        });

        //CircleImageView
        circleImageView = (CircleImageView) findViewById(R.id.profile_image);
        circleImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(Home.this, "bruh!!", Toast.LENGTH_SHORT).show();
            }
        });

        //Toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Location switch
        location_switch = (SwitchCompat) findViewById(R.id.switch_Location);


        //obtiene valor.
        boolean estado_switch = Utils.getValuePreference(prefs);
        //asignar valor
        location_switch.setChecked(estado_switch);
        ChangeColorBorderimage();

        location_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isOnline) {
                //guardar valor
                saveValuePreference(getApplicationContext(), isOnline);

                if (isOnline) {
                    CheckGPSStatus();
                    FirebaseDatabase.getInstance().goOnline(); // conectado
                    circleImageView.setBorderColor(getResources().getColor(R.color.colorON));
                    startLocationUpdates();
                    displayLocation();
                    Snackbar.make(Objects.requireNonNull(mapFragment.getView()), "SERVICIO ACTIVO", Snackbar.LENGTH_SHORT).show();
                    startService(new Intent(c, MyServicio.class));

                } else {
                    FirebaseDatabase.getInstance().goOffline(); //Desconectado
                    circleImageView.setBorderColor(getResources().getColor(R.color.colorOFF));
                    stoplocationUpdates();
                    mCurrent.remove();
                    Snackbar.make(Objects.requireNonNull(mapFragment.getView()), "SERVICIO INACTIVO", Snackbar.LENGTH_SHORT).show();
                    stopService(new Intent(c, MyServicio.class));
                }

            }
        });

        CheckGPSStatus();

        //GeoFire
        drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        geoFire = new GeoFire(drivers);



        //verificar internet
        if (InternetConnection.checkConnection(c)) {
            // Its Available...

            //verificar si hay internet con ping
            if (InternetConnection.internetIsConnected(c)) {
                setUpLocation();
                connected = true;
                updateFirebaseToken();

            }else {

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

    private void updateFirebaseToken() {

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.token_tbl);

        Token token = new Token(FirebaseInstanceId.getInstance().getToken());
        tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(token);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case MI_PERMISION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (checkPlayServices()) {

                        buitGoogleApiClient();
                        createLocationResquest();

                        if (location_switch.isChecked()) {
                            displayLocation();
                        }
                    }


                }

        }
    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
            }, MI_PERMISION_REQUEST_CODE);
        } else {
            if (checkPlayServices()) {

                buitGoogleApiClient();
                createLocationResquest();

                if (location_switch.isChecked()) {


                    if (MyServicio.isServiceCreated()) {


                        Log.e("logogo","servicio no ejecutandoce");

                    }else {

                        startService(new Intent(c, MyServicio.class));
                    }

                    displayLocation();
                }
            }
        }
    }

    private void createLocationResquest() {
        mLocationrequest = new LocationRequest();
        mLocationrequest.setInterval(UPDATE_INTERVAL);
        mLocationrequest.setFastestInterval(FASTED_INTERVAL);
        mLocationrequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationrequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buitGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    private boolean checkPlayServices() {

        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICE_RES_REQUEST).show();
            } else {
                Toast.makeText(this, "Este dispositivo no es soportado.", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }

        return true;
    }

    private void stoplocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

    }

    private void displayLocation() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Common.MyLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (Common.MyLocation != null) {
            if (location_switch.isChecked()) {
                final double latitude = Common.MyLocation.getLatitude();
                final double longitude = Common.MyLocation.getLongitude();

                //update to firebase

                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        //añadir marcador marker
                        if (mCurrent != null)

                            mCurrent.remove();
                        mCurrent = mMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.circlemo))
                                .position(new LatLng(latitude, longitude))
                                .title("Tu"));

                        //Mover la camara a esta pocicion
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 16.5f));
                    }
                });
            }
        } else {
            Log.d("ERROR", "No se puede obtener la localisacion.");
        }

    }

    private void startLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationrequest, this);

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

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(false);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(false);

        //Add move camera to colombia
        LatLng colombia = new LatLng(5, -74);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(colombia, 6));

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        displayLocation();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        Common.MyLocation = location;
        displayLocation();
    }

    private void logout() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    public void saveValuePreference(Context context, boolean valor) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("estado_switch", valor);
        editor.apply();
    }

    public void ChangeColorBorderimage() {

        if (location_switch.isChecked()) {
            circleImageView.setBorderColor(getResources().getColor(R.color.colorON));
        } else {
            circleImageView.setBorderColor(getResources().getColor(R.color.colorOFF));
        }
    }

    public void CheckGPSStatus() {

        //verificar si el gps esta activo
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if ( !locationManager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
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
    public void onBackPressed(){
        if (tiempoPrimerClick + INTERVALO > System.currentTimeMillis()){
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }else {
            Toast.makeText(this, "Vuelve a presionar para salir", Toast.LENGTH_SHORT).show();
        }
        tiempoPrimerClick = System.currentTimeMillis();
    }

}
