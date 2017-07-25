package com.example.android.tourister;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.places.PlaceLikelihood;
import com.google.android.gms.location.places.PlaceLikelihoodBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, OnMapReadyCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final float DEFAULT_ZOOM = 13;

    private SQLiteDatabase sqLiteDatabase;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location lastKnownLocation;
    private GoogleMap googleMap;
    private CameraPosition cameraPosition;

    private static final int REQUEST_CHECK_SETTINGS = 1;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private boolean locationPermissionGranted;
    private boolean requestLocation;

    private ProgressBar mainProgressBar;

    private ListView placesList;
    private ArrayList<String> likelyPlaceNames = new ArrayList<>();
    private ArrayList<String> likelyPlaceAddresses = new ArrayList<>();
    private ArrayList<String> likelyPlaceAttributions = new ArrayList<>();
    private ArrayList<LatLng> likelyPlaceLatLngs = new ArrayList<>();
    private ArrayList<String> likelyPlacePhone = new ArrayList<>();
    private ArrayList<String> likelyPlacePriceLevel = new ArrayList<>();
    private ArrayList<String> likelyPlaceRating = new ArrayList<>();
    private ArrayList<String> likelyPlaceWebsite = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        placesList = (ListView) findViewById(R.id.placesList);
        mainProgressBar = (ProgressBar) findViewById(R.id.mainProgressBar);

        sqLiteDatabase = this.openOrCreateDatabase("Favourites", MODE_PRIVATE, null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS favourites (" +
                "id INTEGER PRIMARY KEY, " +
                "name VARCHAR, " +
                "address VARCHAR, " +
                "attributions VARCHAR, " +
                "latitude VARCHAR, " +
                "longitude VARCHAR, " +
                "phone VARCHAR, " +
                "priceLevel VARCHAR, " +
                "rating VARCHAR, " +
                "website VARCHAR)");

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        googleApiClient.connect();
    }

    @Override
    protected void onStart() {
        googleApiClient.connect();
        super.onStart();
    }

    @Override
    protected void onStop() {
        googleApiClient.disconnect();
        super.onStop();
    }

    protected void createLocationRequest() {
        locationRequest = new LocationRequest()
                .setInterval(10000)
                .setFastestInterval(5000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest);

        PendingResult<LocationSettingsResult> pendingResult = LocationServices.SettingsApi
                .checkLocationSettings(googleApiClient, builder.build());

        pendingResult.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(@NonNull LocationSettingsResult locationSettingsResult) {
                final Status status = locationSettingsResult.getStatus();
                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        requestLocation = true;
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            status.startResolutionForResult(MainActivity.this, REQUEST_CHECK_SETTINGS);
                        } catch (IntentSender.SendIntentException e) {
                            Log.e("error", e.getMessage());
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == RESULT_OK) {
                requestLocation = true;
            }
            else requestLocation = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.option_get_place) {
            getPlaces();
        }
        else if (item.getItemId() == R.id.option_favourites_list) {
            startActivity(new Intent(MainActivity.this, FavouritePlacesActivity.class));
        }
        return true;
    }

    protected void getPlaces() {
        createLocationRequest();
        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }

        mainProgressBar.setVisibility(View.VISIBLE);
        placesList.setVisibility(View.GONE);

        lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);

        if (lastKnownLocation != null) {
            if (googleMap != null) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(lastKnownLocation.getLatitude(),
                                    lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
            }
            if (locationPermissionGranted && requestLocation) {
                PendingResult<PlaceLikelihoodBuffer> result = Places.PlaceDetectionApi
                        .getCurrentPlace(googleApiClient, null);
                result.setResultCallback(new ResultCallback<PlaceLikelihoodBuffer>() {
                    @Override
                    public void onResult(@NonNull PlaceLikelihoodBuffer likelyPlaces) {
                        likelyPlaceNames = new ArrayList<>();
                        likelyPlaceAddresses = new ArrayList<>();
                        likelyPlaceAttributions = new ArrayList<>();
                        likelyPlaceLatLngs = new ArrayList<>();
                        likelyPlacePhone = new ArrayList<>();
                        likelyPlacePriceLevel = new ArrayList<>();
                        likelyPlaceRating = new ArrayList<>();
                        likelyPlaceWebsite = new ArrayList<>();

                        for (PlaceLikelihood placeLikelihood : likelyPlaces) {
                            likelyPlaceNames.add((String) placeLikelihood.getPlace().getName());
                            likelyPlaceAddresses.add((String) placeLikelihood.getPlace().getAddress());
                            likelyPlaceAttributions.add((String) placeLikelihood.getPlace().getAttributions());
                            likelyPlaceLatLngs.add(placeLikelihood.getPlace().getLatLng());
                            likelyPlacePhone.add((String) placeLikelihood.getPlace().getPhoneNumber());
                            likelyPlacePriceLevel.add(String.valueOf(placeLikelihood.getPlace().getPriceLevel()));
                            likelyPlaceRating.add(String.valueOf(placeLikelihood.getPlace().getRating()));
                            likelyPlaceWebsite.add(String.valueOf((placeLikelihood.getPlace().getWebsiteUri() == null) ? "-" : placeLikelihood.getPlace().getWebsiteUri()));
                        }

                        likelyPlaces.release();
                        populatePlacesList();
                    }
                });
            }
            else if (!locationPermissionGranted) {
                if (ActivityCompat.checkSelfPermission(this.getApplicationContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                }
            }
        }
    }

    private void populatePlacesList() {
        mainProgressBar.setVisibility(View.GONE);
        placesList.setVisibility(View.VISIBLE);

        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, likelyPlaceNames);
        placesList.setAdapter(arrayAdapter);

        placesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, DirectionToPlaceActivity.class);
                LatLng origin = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                LatLng destination = new LatLng(likelyPlaceLatLngs.get(position).latitude, likelyPlaceLatLngs.get(position).longitude);
                Bundle endPointsLatLng = new Bundle();
                endPointsLatLng.putParcelable("origin", origin);
                endPointsLatLng.putParcelable("destination", destination);
                intent.putExtra("endPointsLatLng", endPointsLatLng);

                Bundle placeExtraInfo = new Bundle();
                placeExtraInfo.putString("name", likelyPlaceNames.get(position));
                placeExtraInfo.putString("address", likelyPlaceAddresses.get(position));
                placeExtraInfo.putString("attributions", likelyPlaceAttributions.get(position));
                placeExtraInfo.putString("phone", likelyPlacePhone.get(position));
                placeExtraInfo.putString("priceLevel", likelyPlacePriceLevel.get(position));
                placeExtraInfo.putString("rating", likelyPlaceRating.get(position));
                placeExtraInfo.putString("website", likelyPlaceWebsite.get(position));
                intent.putExtra("placeExtraInfo", placeExtraInfo);

                startActivity(intent);
            }
        });

        placesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM favourites WHERE name = \"" + likelyPlaceNames.get(position) + "\"", null);
                cursor.moveToFirst();
                if (cursor.getCount() == 0) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("Do you want to add this place to you Favourite Places List?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String sql = "INSERT INTO favourites (name, address, attributions, latitude, longitude, phone, priceLevel, rating, website) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

                                    SQLiteStatement statement = sqLiteDatabase.compileStatement(sql);

                                    statement.bindString(1, likelyPlaceNames.get(position));
                                    statement.bindString(2, likelyPlaceAddresses.get(position) == null ? "-" : likelyPlaceAddresses.get(position));
                                    statement.bindString(3, likelyPlaceAttributions.get(position) == null ? "-" : likelyPlaceAttributions.get(position));
                                    statement.bindString(4, String.valueOf(likelyPlaceLatLngs.get(position).latitude));
                                    statement.bindString(5, String.valueOf(likelyPlaceLatLngs.get(position).longitude));
                                    statement.bindString(6, likelyPlacePhone.get(position) == null ? "-" : likelyPlacePhone.get(position));
                                    statement.bindString(7, likelyPlacePriceLevel.get(position) == null ? "-" : likelyPlacePriceLevel.get(position));
                                    statement.bindString(8, likelyPlaceRating.get(position) == null ? "-" : likelyPlaceRating.get(position));
                                    statement.bindString(9, likelyPlaceWebsite.get(position) == null ? "-" : likelyPlaceWebsite.get(position));

                                    statement.execute();

                                    Toast.makeText(MainActivity.this, "Place added to Favourite Places List", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                }
                else {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("Do you want to remove this place to you Favourite Places List?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sqLiteDatabase.delete("favourites", "name = \"" + likelyPlaceNames.get(position) + "\"", null);
                                    Toast.makeText(MainActivity.this, "Place removed from Favourite Places List", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                }
                return true;
            }
        });
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        if (locationPermissionGranted) {
            createLocationRequest();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        locationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                }
            }
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Play services connection suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Play services connection failed");
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;

        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
        if (locationPermissionGranted) {
            googleMap.setMyLocationEnabled(true);
            googleMap.getUiSettings().setMyLocationButtonEnabled(true);
        } else {
            googleMap.setMyLocationEnabled(false);
            googleMap.getUiSettings().setMyLocationButtonEnabled(false);
            lastKnownLocation = null;
        }

        if (locationPermissionGranted) {
            lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        }

        cameraPosition = googleMap.getCameraPosition();
        if (cameraPosition != null) {
            googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else if (lastKnownLocation != null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(lastKnownLocation.getLatitude(),
                            lastKnownLocation.getLongitude()), DEFAULT_ZOOM));
        }
    }
}