package com.example.android.tourister;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.example.android.tourister.adapters.PlaceAdapter;
import com.example.android.tourister.models.PlaceItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class DirectionToPlaceActivity extends AppCompatActivity implements OnMapReadyCallback, RoutingListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private GoogleMap googleMap;
    private LatLng origin, destination;
    private List<Polyline> polylines;
    private ListView placeInfoList;
    private List<PlaceItem> placeInfo;
    private PlaceAdapter placeAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_direction_to_place);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.directionsMap);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap map) {
        googleMap = map;
        Intent intent = getIntent();
        Bundle endPointsLatLng = intent.getBundleExtra("endPointsLatLng");
        origin = endPointsLatLng.getParcelable("origin");
        destination = endPointsLatLng.getParcelable("destination");

        Bundle placeExtraInfo = intent.getBundleExtra("placeExtraInfo");

        polylines = new ArrayList<>();
        placeInfoList = (ListView) findViewById(R.id.placeInfoList);

        placeInfo = new ArrayList<>();
        placeInfo.add(new PlaceItem("Name", placeExtraInfo.getString("name")));
        placeInfo.add(new PlaceItem("Address", placeExtraInfo.getString("address")));
        placeInfo.add(new PlaceItem("Latitude", String.valueOf(destination.latitude)));
        placeInfo.add(new PlaceItem("Longitude", String.valueOf(destination.longitude)));
        placeInfo.add(new PlaceItem("Contact No.", placeExtraInfo.getString("phone")));
        placeInfo.add(new PlaceItem("Attributions", placeExtraInfo.getString("attributions", "-")));
        placeInfo.add(new PlaceItem("Price Level", placeExtraInfo.getString("priceLevel", "-")));
        placeInfo.add(new PlaceItem("Rating", placeExtraInfo.getString("rating", "-")));
        placeInfo.add(new PlaceItem("Website", placeExtraInfo.getString("website")));

        placeAdapter = new PlaceAdapter(this, R.layout.place_item_layout, placeInfo);
        placeInfoList.setAdapter(placeAdapter);

        placeInfoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PlaceItem placeItem = placeInfo.get(position);
                if (placeItem.getTitle().equals("Website")) {
                    if (!placeItem.getValue().equals("-")) {
                        Intent i = new Intent(DirectionToPlaceActivity.this, WebviewActivity.class);
                        i.putExtra("webURL", placeItem.getValue());
                        startActivity(i);
                    }
                }
            }
        });

        googleMap.clear();

        Routing routing = new Routing.Builder()
                .travelMode(AbstractRouting.TravelMode.DRIVING)
                .withListener(this)
                .alternativeRoutes(true)
                .waypoints(origin, destination)
                .build();
        routing.execute();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.share_location_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.share) {
            String location = "http://maps.google.com/maps?q=" + destination.latitude + "," + destination.longitude + "&iwloc=A";
            String mimeType = "text/plain";
            String title = "Share Destination Location";

            ShareCompat.IntentBuilder.from(this)
                    .setChooserTitle(title)
                    .setType(mimeType)
                    .setText(location)
                    .startChooser();
        }
        return true;
    }

    @Override
    public void onRoutingFailure(RouteException e) {
        Toast.makeText(this, "Something went wrong, Try again", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRoutingStart() {
        Log.i(TAG, "Routing process started.");
    }

    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 18));
        polylines = new ArrayList<>();

        for (int i = 0; i < route.size(); i++) {
            PolylineOptions polylineOptions = new PolylineOptions();
            polylineOptions.color(R.color.primary_dark);
            polylineOptions.width(10 + i * 3);
            polylineOptions.addAll(route.get(i).getPoints());
            Polyline polyline = googleMap.addPolyline(polylineOptions);
            polylines.add(polyline);

            placeInfo.add(new PlaceItem("Route " + (i + 1) + " (Driving)", "Distance : " + route.get(i).getDistanceText() + "\nDuration : " + route.get(i).getDurationText()));
        }
        placeAdapter.notifyDataSetChanged();

        googleMap.addMarker(new MarkerOptions().position(origin));
        googleMap.addMarker(new MarkerOptions().position(destination));
    }

    @Override
    public void onRoutingCancelled() {
        Log.i(TAG, "Routing was cancelled.");
    }
}
