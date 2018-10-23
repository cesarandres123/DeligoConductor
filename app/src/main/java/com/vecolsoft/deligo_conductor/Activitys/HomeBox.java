package com.vecolsoft.deligo_conductor.Activitys;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.mapbox.android.core.location.LocationEngine;
import com.mapbox.android.core.location.LocationEngineListener;
import com.mapbox.android.core.location.LocationEnginePriority;
import com.mapbox.android.core.location.LocationEngineProvider;
import com.mapbox.android.core.permissions.PermissionsListener;
import com.mapbox.android.core.permissions.PermissionsManager;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncher;
import com.mapbox.services.android.navigation.ui.v5.NavigationLauncherOptions;
import com.mapbox.services.android.navigation.ui.v5.route.NavigationMapRoute;
import com.mapbox.services.android.navigation.v5.navigation.NavigationRoute;
import com.vecolsoft.deligo_conductor.Common.Common;
import com.vecolsoft.deligo_conductor.Modelo.FCMResponse;
import com.vecolsoft.deligo_conductor.Modelo.Notification;
import com.vecolsoft.deligo_conductor.Modelo.Sender;
import com.vecolsoft.deligo_conductor.Modelo.Token;
import com.vecolsoft.deligo_conductor.R;
import com.vecolsoft.deligo_conductor.Remote.IFCMService;
import com.vecolsoft.deligo_conductor.Servicio.MyServicio;
import com.vecolsoft.deligo_conductor.Utils.InternetConnection;
import com.vecolsoft.deligo_conductor.Utils.Utils;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class HomeBox extends AppCompatActivity implements
        OnMapReadyCallback,
        LocationEngineListener,
        PermissionsListener {

    // variables for adding location layer
    private MapboxMap mapboxMap;
    private PermissionsManager permissionsManager;
    private LocationEngine locationEngine;
    private LocationManager locationManager;
    private NavigationMapRoute navigationMapRoute;
    //geocider location
    Geocoder geocoder;

    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    // variable for adding map
    private MapView mapView;
    private Marker mCurrent;

    private SharedPreferences prefs;


    //elementos del toolbar
    private Toolbar toolbar;
    private CircleImageView circleImageView;
    private RelativeLayout perfil;
    private TextView nombre;
    /////////////////////////////////

    IFCMService mfcmService;


    //elementos
    private TextView txtLocation;
    private FloatingActionButton navigationButton;


    DatabaseReference drivers;
    GeoFire geoFire;

    SwitchCompat location_switch;

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
    private Point originPosition;
    private Point destinationPosition;
    private Marker destinationMarker;
    private static final String TAG = "MainActivity";
    private DirectionsRoute currentRoute;
    String customerId;



    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //Bolquear rotacion
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        super.onCreate(savedInstanceState);
        Mapbox.getInstance(this, getString(R.string.access_token));

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


        mapView = (MapView) findViewById(R.id.mapViewBox);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(this);


        //perfil
        perfil = (RelativeLayout) findViewById(R.id.perfil);
        perfil.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startActivity(new Intent(HomeBox.this, PerfilActivity.class));
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
        nombre = (TextView) findViewById(R.id.tvNombre);

        if (Common.CurrentUser.getName() != null) {
            nombre.setText(Common.CurrentUser.getName());
        }

        circleImageView = (CircleImageView) findViewById(R.id.profile_image);

        //load Avatar
        if (Common.CurrentUser.getAvatarUrl() != null && !TextUtils.isEmpty(Common.CurrentUser.getAvatarUrl())) {

            Glide.with(this)
                    .load(Common.CurrentUser.getAvatarUrl())
                    .into(circleImageView);
        }


        //Location switch
        location_switch = (SwitchCompat) findViewById(R.id.switch_Location);

        //location TextView
        txtLocation = (TextView) findViewById(R.id.txtLocation);

        //NavigationButon
        navigationButton = (FloatingActionButton) findViewById(R.id.navigationButton);
        navigationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //showNavigationRoute();
            }
        });

        //obtiene valor.
        boolean estado_switch = Utils.getValuePreference(prefs);
        //asignar valor
        location_switch.setChecked(estado_switch);
        ChangeColorBorderimage();

        if (Common.OnSeguimiento != null) {
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
                    displayLocation();

                    try {
                        getLocation(); //mostar ubicacion en textview
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Snackbar.make(Objects.requireNonNull(mapView), "SERVICIO ACTIVO", Snackbar.LENGTH_SHORT).show();
                    startService(new Intent(c, MyServicio.class));

                } else {
                    Snackbar.make(Objects.requireNonNull(mapView), "SERVICIO INACTIVO", Snackbar.LENGTH_SHORT).show();
                    FirebaseDatabase.getInstance().goOffline(); //Desconectado
                    circleImageView.setBorderColor(getResources().getColor(R.color.colorOFF));
                    stopLocationEngine();

                    if (mCurrent != null) {
                        mCurrent.remove();
                    }

                    txtLocation.setText("Sin ubucacion.");
                    stopService(new Intent(c, MyServicio.class));

                }

            }
        });

        CheckGPSStatus();
        verificarinternet();


        //GeoFire
        drivers = FirebaseDatabase.getInstance().getReference(Common.driver_tbl);
        geoFire = new GeoFire(drivers);


        //SIstema de seguimieno
        if (getIntent() != null) {

            riderlat = getIntent().getDoubleExtra("lat", -1.0);
            riderlng = getIntent().getDoubleExtra("lng", -1.0);
            customerId = getIntent().getStringExtra("customerId");
        }

        if (Common.OnSeguimiento != null) {

            destinationPosition = Point.fromLngLat(riderlng, riderlat);
            originPosition = Point.fromLngLat(Common.MyLocation.getLongitude(), Common.MyLocation.getLatitude());

            getRoute(originPosition, destinationPosition);
            Common.UnaSolaVes = true;

        }


    }

    private void showNavigationRoute() {

        boolean simulateRoute = false;
        NavigationLauncherOptions options = NavigationLauncherOptions.builder()
                .directionsRoute(currentRoute)
                .shouldSimulateRoute(simulateRoute)
                .build();

        // Call this method with Context from within an Activity
        NavigationLauncher.startNavigation(HomeBox.this, options);

        Common.UnaSolaVes = null;
        navigationButton.setVisibility(View.VISIBLE);


    }

    private void getRoute(Point origin, Point destination) {
        NavigationRoute.builder(this)
                .accessToken(Mapbox.getAccessToken())
                .origin(origin)
                .destination(destination)
                .build()
                .getRoute(new Callback<DirectionsResponse>() {
                    @Override
                    public void onResponse(Call<DirectionsResponse> call, Response<DirectionsResponse> response) {
                        // You can get the generic HTTP info about the response
                        Log.d(TAG, "Response code: " + response.code());
                        if (response.body() == null) {
                            Log.e(TAG, "No routes found, make sure you set the right user and access token.");
                            return;
                        } else if (response.body().routes().size() < 1) {
                            Log.e(TAG, "No routes found");
                            return;
                        }

                        currentRoute = response.body().routes().get(0);

                        // Draw the route on the map
                        if (navigationMapRoute != null) {
                            navigationMapRoute.removeRoute();
                        } else {
                            navigationMapRoute = new NavigationMapRoute(null, mapView, mapboxMap, R.style.NavigationMapRoute);
                        }
                        navigationMapRoute.addRoute(currentRoute);

                        setRouteCameraPosition();

                        if (Common.UnaSolaVes != null) {
                            //showNavigationRoute();
                        }


                    }

                    @Override
                    public void onFailure(Call<DirectionsResponse> call, Throwable throwable) {
                        Log.e(TAG, "Error: " + throwable.getMessage());
                    }
                });


    }

    private void setRouteCameraPosition() {

        if (Common.MyLocation != null) {
            if (location_switch.isChecked()) {

                final double latitude = Common.MyLocation.getLatitude();
                final double longitude = Common.MyLocation.getLongitude();

                //update to firebase
                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                        //añadir marcador conductor
                        if (mCurrent != null) {
                            mCurrent.remove();
                        }
                        // Create an Icon object for the marker to use
                        IconFactory iconFactory = IconFactory.getInstance(HomeBox.this);
                        Icon icon = iconFactory.fromResource(R.drawable.circlemo);

                        mCurrent = mapboxMap.addMarker(new MarkerOptions()
                                .position(new LatLng(Common.MyLocation.getLatitude(), Common.MyLocation.getLongitude()))
                                .icon(icon));


                        //añadir marcador destino
                        destinationMarker = mapboxMap.addMarker(new MarkerOptions()
                                .position(new LatLng(riderlat, riderlng)));

                        LatLngBounds latLngBounds = new LatLngBounds.Builder()
                                .include(new LatLng(originPosition.latitude(), originPosition.longitude()))
                                .include(new LatLng(destinationPosition.latitude(), destinationPosition.longitude()))
                                .build();

                        mapboxMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, 50));


                    }
                });
            }
        } else {
            Log.d("ERROR", "No se puede obtener la localisacion.");
        }
    }

    private void EnviarNotificacionDeArribo(String customerId) {
        Token token = new Token(customerId);

        Notification notification = new Notification("Esta aqui!",String.format("El conductor %s ha llegado.",Common.CurrentUser.getName()));
        Sender sender = new Sender(token.getToken(), notification);

        mfcmService.sendMessage(sender).enqueue(new Callback<FCMResponse>() {
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

    private void updateFirebaseToken() {

        FirebaseDatabase db = FirebaseDatabase.getInstance();
        DatabaseReference tokens = db.getReference(Common.token_tbl);

        Token token = new Token(FirebaseInstanceId.getInstance().getToken());
        tokens.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .setValue(token);

    }

    @Override
    public void onMapReady(MapboxMap mapboxMap) {


        this.mapboxMap = mapboxMap;
        mapboxMap.getUiSettings().setAttributionEnabled(false);
        mapboxMap.getUiSettings().setLogoEnabled(false);
        mapboxMap.getUiSettings().setTiltGesturesEnabled(false);
        mapboxMap.getUiSettings().setRotateGesturesEnabled(false);

        if (Common.OnSeguimiento != null) {

            //Sistema de notificar arribo de conductor
            geoFire = new GeoFire(FirebaseDatabase.getInstance().getReference(Common.driver_tbl));
            GeoQuery geoQuery = geoFire.queryAtLocation(new GeoLocation(riderlat, riderlng), 0.05f);
            geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
                @Override
                public void onKeyEntered(String key, GeoLocation location) {
                    EnviarNotificacionDeArribo(customerId);
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

        if (Common.MyLocation != null) {
            if (location_switch.isChecked()) {

                final double latitude = Common.MyLocation.getLatitude();
                final double longitude = Common.MyLocation.getLongitude();

                //update to firebase
                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {

                        //añadir marcador marker
                        if (mapboxMap != null) {

                            if (mCurrent != null) {
                                mCurrent.remove();
                            }
                            // Create an Icon object for the marker to use
                            IconFactory iconFactory = IconFactory.getInstance(HomeBox.this);
                            Icon icon = iconFactory.fromResource(R.drawable.circlemo);

                            mCurrent = mapboxMap.addMarker(new MarkerOptions()
                                    .position(new LatLng(Common.MyLocation.getLatitude(), Common.MyLocation.getLongitude()))
                                    .icon(icon));

                            CameraPosition position = new CameraPosition.Builder()
                                    .target(new LatLng(Common.MyLocation.getLatitude(), Common.MyLocation.getLongitude())) // Sets the new camera position
                                    .zoom(17) // Sets the zoom to level 10
                                    .tilt(20) // Set the camera tilt to 20 degrees
                                    .build(); // Builds the CameraPosition object from the builder

                            mapboxMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
                        }

                    }
                });
            }
        } else {
            Log.d("ERROR", "No se puede obtener la localisacion.");
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void enableLocationPlugin() {
        // Check if permissions are enabled and if not request
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            initializeLocationEngine();

            //autoIniciar el servicio
            if (location_switch.isChecked()) {

                if (MyServicio.isServiceCreated()) {
                    Log.e("logogo", "servicio no ejecutandoce");

                } else {
                    startService(new Intent(c, MyServicio.class));

                }
            }

        } else {
            permissionsManager = new PermissionsManager(this);
            permissionsManager.requestLocationPermissions(this);
        }
    }

    private void stopLocationEngine() {

        if (locationEngine != null) {
            locationEngine.deactivate();
            if (mCurrent != null) {
                mCurrent.remove();
            }
        }
    }

    @SuppressWarnings({"MissingPermission"})
    private void initializeLocationEngine() {
        LocationEngineProvider locationEngineProvider = new LocationEngineProvider(this);
        locationEngine = locationEngineProvider.obtainBestLocationEngineAvailable();
        locationEngine.setPriority(LocationEnginePriority.HIGH_ACCURACY);

        /////////////////////////////////////////
        locationEngine.setInterval(UPDATE_INTERVAL);
        locationEngine.setFastestInterval(FASTEST_INTERVAL);
        locationEngine.setSmallestDisplacement(DISPLACEMENT);
        /////////////////////////////////////////

        locationEngine.activate();

        Location lastLocation = locationEngine.getLastLocation();
        if (lastLocation != null) {
            Common.MyLocation = lastLocation;
        } else {
            locationEngine.addLocationEngineListener(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        permissionsManager.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onExplanationNeeded(List<String> permissionsToExplain) {
        //Toast.makeText(this, R.string.user_location_permission_explanation, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onPermissionResult(boolean granted) {
        if (granted) {
            enableLocationPlugin();
        } else {
            //Toast.makeText(this, R.string.user_location_permission_not_granted, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    @SuppressWarnings({"MissingPermission"})
    @Override
    protected void onStart() {
        super.onStart();

        if (locationEngine != null) {
            locationEngine.requestLocationUpdates();
        }
        mapView.onStart();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (locationEngine != null) {
            locationEngine.removeLocationUpdates();
        }
        mapView.onStop();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationEngine != null) {
            locationEngine.deactivate();
        }
        Common.OnSeguimiento = null;
        mapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }

    @Override
    @SuppressWarnings("MissingPermission")
    public void onConnected() {
        locationEngine.requestLocationUpdates();

    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Common.MyLocation = location;

            if (Common.OnSeguimiento != null) {
                getRoute(originPosition, destinationPosition);
                setRouteCameraPosition();
                try {
                    getLocation();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            } else {

                if (location_switch.isChecked()) {
                    displayLocation();
                    try {
                        getLocation();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

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
                enableLocationPlugin();
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
        } else {
            circleImageView.setBorderColor(getResources().getColor(R.color.colorOFF));
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