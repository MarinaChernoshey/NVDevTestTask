package com.example.marina.testnvdev;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.ybq.android.spinkit.style.Circle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class MapsActivity extends FragmentActivity
        implements
        OnMapReadyCallback,
        GoogleMap.OnMyLocationButtonClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int DEFAULT_ZOOM = 13;
    private static final LatLng DEFAULT_LOCATION = new LatLng(-33.8523341, 151.2106085);

    private LatLng location = DEFAULT_LOCATION;

    private boolean mPermissionDenied = false;

    private GoogleMap map;
    private GoogleApiClient mGoogleApiClient;

    private Location mLastKnownLocation;
    private boolean mLocationPermissionGranted;

    private CameraPosition cameraPosition;

    private FloatingActionButton fab;
    private BottomSheetBehavior bottomSheetBehavior;

    private static LinearLayout linearLayoutElementsForShare;
    private static ImageView imageView;
    private static ProgressBar progressBar;
    private static ImageButton button;
    private static TextView textViewDisconnect;

    private Region region;

    private LocationManager locationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        setViews();

        setGoogleApiClient();
    }

    private void setViews() {
        linearLayoutElementsForShare = (LinearLayout) findViewById(R.id.elements_for_share);

        Circle circle = new Circle();
        circle.setColor(R.color.colorAccent);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        imageView = (ImageView) findViewById(R.id.image_places);
        button = (ImageButton) findViewById(R.id.button_share);
        textViewDisconnect = (TextView) findViewById(R.id.text_view_disconnect);

        progressBar.setIndeterminateDrawable(circle);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCollage();
            }
        });

        setFloatingActionButton();

        setBottomSheet();
    }

    private void setGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */,
                        this /* OnConnectionFailedListener */)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .build();
        mGoogleApiClient.connect();
    }

    private void setBottomSheet() {
        LinearLayout llBottomSheet = (LinearLayout) findViewById(R.id.bottom_sheet);

        bottomSheetBehavior = BottomSheetBehavior.from(llBottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    private void setFloatingActionButton() {
        fab = (FloatingActionButton) findViewById(R.id.fab);


        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                bottomSheetBehavior.setPeekHeight((int) (48 * getResources().getDisplayMetrics().density));
                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);

                linearLayoutElementsForShare.setVisibility(View.INVISIBLE);

                if (!isOnline()) {
                    progressBar.setVisibility(View.INVISIBLE);
                    textViewDisconnect.setVisibility(View.VISIBLE);
                } else {
                    region = new Region(location.latitude, location.longitude, MapsActivity.this);

                    progressBar.setVisibility(View.VISIBLE);
                    textViewDisconnect.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.setOnMyLocationButtonClickListener(this);

        requestToUseLocation();
        setCameraPosition();
    }

    private void requestToUseLocation() {
        if (map == null) {
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;

        } else {
            PermissionHelper.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        }

        if (mLocationPermissionGranted) {
            mLastKnownLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

            map.setMyLocationEnabled(true);
            map.getUiSettings().setMyLocationButtonEnabled(true);

        } else {
            map.setMyLocationEnabled(false);
            map.getUiSettings().setMyLocationButtonEnabled(false);
            mLastKnownLocation = null;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        mLocationPermissionGranted = false;

        if (PermissionHelper.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                startActivity(new Intent(
                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                mPermissionDenied = false;
                mLocationPermissionGranted = true;
            }
        } else {
            mPermissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();

        if (!mPermissionDenied) {
            requestToUseLocation();
        }
    }

    private void setCameraPosition() {
        if (cameraPosition != null) {
            map.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        } else if (mLastKnownLocation != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(
                    new LatLng(mLastKnownLocation.getLatitude(),
                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));

            location = new LatLng(mLastKnownLocation.getLatitude(), mLastKnownLocation.getLongitude());

        } else {
            Toast.makeText(getApplicationContext(), getString(R.string.message_this_location_is_null),
                    Toast.LENGTH_SHORT).show();

            map.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, DEFAULT_ZOOM));
            map.getUiSettings().setMyLocationButtonEnabled(false);
        }
    }

    private void sendCollage() {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType(getString(R.string.share_intent_type_image_jpeg));
        shareIntent.putExtra(Intent.EXTRA_STREAM, region.getUri());
        startActivity(Intent.createChooser(shareIntent,
                String.valueOf(getString(R.string.message_send_image))));
    }

    public static void getRegionSearchEqualsTrue(Bitmap collage) {
        progressBar.setVisibility(View.INVISIBLE);
        linearLayoutElementsForShare.setVisibility(View.VISIBLE);

        imageView.setImageBitmap(collage);
    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }
}


