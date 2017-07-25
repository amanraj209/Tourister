package com.example.android.tourister;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
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
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class FavouritePlacesActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = MainActivity.class.getSimpleName();

    private SQLiteDatabase sqLiteDatabase;
    private ArrayList<String> placesName, placesAddress, placesAttributions, placesLatitudes, placesLongitudes, placesPhone, placesPriceLevel, placesRating, placesWebsite;
    private ArrayAdapter arrayAdapter;
    private ListView favouritesList;

    private GoogleApiClient googleApiClient;
    private LocationRequest locationRequest;
    private Location lastKnownLocation;

    private static final int REQUEST_CHECK_SETTINGS = 1;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private boolean locationPermissionGranted;
    private boolean requestLocation;

    private LatLng origin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favourite_places);

        favouritesList = (ListView) findViewById(R.id.favouritesList);

        placesName = new ArrayList<>();
        placesAddress = new ArrayList<>();
        placesAttributions = new ArrayList<>();
        placesLatitudes = new ArrayList<>();
        placesLongitudes = new ArrayList<>();
        placesPhone = new ArrayList<>();
        placesPriceLevel = new ArrayList<>();
        placesRating = new ArrayList<>();
        placesWebsite = new ArrayList<>();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        googleApiClient.connect();

        sqLiteDatabase = this.openOrCreateDatabase("Favourites", MODE_PRIVATE, null);

        Cursor cursor = sqLiteDatabase.rawQuery("SELECT * FROM favourites", null);

        int nameIndex = cursor.getColumnIndex("name");
        int addressIndex = cursor.getColumnIndex("address");
        int attributionsIndex = cursor.getColumnIndex("attributions");
        int latitudeIndex = cursor.getColumnIndex("latitude");
        int longitudeIndex = cursor.getColumnIndex("longitude");
        final int phoneIndex = cursor.getColumnIndex("phone");
        int priceLevelIndex = cursor.getColumnIndex("priceLevel");
        int ratingIndex = cursor.getColumnIndex("rating");
        int websiteIndex = cursor.getColumnIndex("website");

        cursor.moveToFirst();
        placesName.clear();

        if (cursor.getCount() == 0) {
            placesName.add("No favourite places to show");
            arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, placesName);
            favouritesList.setAdapter(arrayAdapter);
        }
        else {
            int i = 0;
            while (i < cursor.getCount()) {
                placesName.add(cursor.getString(nameIndex));
                placesAddress.add(cursor.getString(addressIndex));
                placesAttributions.add(cursor.getString(attributionsIndex));
                placesLatitudes.add(cursor.getString(latitudeIndex));
                placesLongitudes.add(cursor.getString(longitudeIndex));
                placesPhone.add(cursor.getString(phoneIndex));
                placesPriceLevel.add(cursor.getString(priceLevelIndex));
                placesRating.add(cursor.getString(ratingIndex));
                placesWebsite.add(cursor.getString(websiteIndex));
                i++;
                cursor.moveToNext();
            }
            arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, placesName);
            favouritesList.setAdapter(arrayAdapter);

            favouritesList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    if (locationPermissionGranted && requestLocation) {
                        Intent infoIntent = new Intent(FavouritePlacesActivity.this, DirectionToPlaceActivity.class);

                        LatLng destination = new LatLng(Double.valueOf(placesLatitudes.get(position)), Double.valueOf(placesLongitudes.get(position)));
                        Bundle endPointsLatLng = new Bundle();
                        endPointsLatLng.putParcelable("origin", origin);
                        endPointsLatLng.putParcelable("destination", destination);
                        infoIntent.putExtra("endPointsLatLng", endPointsLatLng);

                        Bundle placeExtraInfo = new Bundle();
                        placeExtraInfo.putString("name", placesName.get(position));
                        placeExtraInfo.putString("address", placesAddress.get(position));
                        placeExtraInfo.putString("attributions", placesAttributions.get(position));
                        placeExtraInfo.putString("phone", placesPhone.get(position));
                        placeExtraInfo.putString("priceLevel", placesPriceLevel.get(position));
                        placeExtraInfo.putString("rating", placesRating.get(position));
                        placeExtraInfo.putString("website", placesWebsite.get(position));
                        infoIntent.putExtra("placeExtraInfo", placeExtraInfo);

                        startActivity(infoIntent);
                    }
                    else if (!locationPermissionGranted) {
                        if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                                android.Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                            locationPermissionGranted = true;
                        } else {
                            ActivityCompat.requestPermissions(getParent(),
                                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                        }
                    }
                    else if (!requestLocation) {
                        createLocationRequest();
                        lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                        origin = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                    }
                }
            });

            favouritesList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    new AlertDialog.Builder(FavouritePlacesActivity.this)
                            .setMessage("Do you want to remove this place to you Favourite Places List?")
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    sqLiteDatabase.delete("favourites", "name = \"" + placesName.get(position) + "\"", null);
                                    placesName.remove(position);
                                    if (placesName.isEmpty()) {
                                        placesName.add("No favourite places to show");
                                    }
                                    arrayAdapter.notifyDataSetChanged();
                                    Toast.makeText(FavouritePlacesActivity.this, "Place removed from Favourite Places List", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("No", null)
                            .show();
                    return true;
                }
            });
        }
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
                            status.startResolutionForResult(FavouritePlacesActivity.this, REQUEST_CHECK_SETTINGS);
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
                if (ActivityCompat.checkSelfPermission(this.getApplicationContext(),
                        android.Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    locationPermissionGranted = true;
                } else {
                    ActivityCompat.requestPermissions(this,
                            new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                }
                lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
                if (lastKnownLocation != null) {
                    origin = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
                }
            }
            else requestLocation = false;
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "Play services connection failed");
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
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
        lastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
        if (lastKnownLocation != null) {
            origin = new LatLng(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Play services connection suspended");
    }
}
