package com.bespalov.locationapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import java.text.DateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int CHECK_SETTINGS_CODE = 123;
    private static final int REQUEST_LOCATION_PERMISSION = 321;

    private Button startLocationUpdateButton, stopLocationUpdateButton;
    private TextView locationTextView, locationUpdateTimeTextView;

    private FusedLocationProviderClient fusedLocationClient;
    private SettingsClient settingsClient;
    private LocationRequest locationRequest;
    private LocationSettingsRequest locationSettingsRequest;
    private LocationCallback locationCallback;
    private Location currentLocation;

    private boolean locationUpdateActive;
    private String loacationUpdateTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        startLocationUpdateButton = findViewById(R.id.startLocationUpdateButton);
        stopLocationUpdateButton = findViewById(R.id.stopLocationUpdateButton);
        locationTextView = findViewById(R.id.locationTextView);
        locationUpdateTimeTextView = findViewById(R.id.locationUpdateTimeTextView);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        settingsClient = LocationServices.getSettingsClient(this);

        buildLocationRequest();
        buildLocationCallBack();
        buildLocationSettingRequest();

        startLocationUpdateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startLocatinUpdate();
            }
        });


    }

    private void startLocatinUpdate() {
        locationUpdateActive = true;
        startLocationUpdateButton.setEnabled(false);
        stopLocationUpdateButton.setEnabled(true);
        settingsClient.checkLocationSettings(locationSettingsRequest).addOnSuccessListener(this,
                new OnSuccessListener<LocationSettingsResponse>() {
                    @Override
                    public void onSuccess(LocationSettingsResponse locationSettingsResponse) {
                        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                                ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) !=
                                        PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
                    }
                }).addOnFailureListener(this, new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {
                    case LocationSettingsStatusCodes
                            .RESOLUTION_REQUIRED:
                        try {
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(MainActivity.this, CHECK_SETTINGS_CODE);
                        } catch (IntentSender.SendIntentException sendIntentException) {
                            sendIntentException.printStackTrace();
                        }
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        String message = "Adjust location settings on your device";
                        Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
                        locationUpdateActive = false;
                        startLocationUpdateButton.setEnabled(true);
                        stopLocationUpdateButton.setEnabled(false);
                }

            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        if (locationUpdateActive && checkLocationPermission()) {
            startLocatinUpdate();
        } else if (!checkLocationPermission()) {
            requestLocationPermission();
        }
    }

    private void requestLocationPermission() {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        if (shouldProvideRationale) {
            showSnackBar("Location permission is nedded for app functionality", "OK", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    REQUEST_LOCATION_PERMISSION);
                        }
                    }
            );
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        }

    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length <= 0) {
                Log.d("onRequestPermission", "Request was cancelled");
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocatinUpdate();
            }
        }else {
            showSnackBar("Turn on location on settings", "Settings", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_SETTINGS);
                    Uri uri =  Uri.fromParts("package", MainActivity.this.getPackageName(),null);
                    intent.setData(uri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
            });
        }
    }



    private void showSnackBar(final String mainText, final String action, View.OnClickListener listener) {
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE).setAction(action, listener).show();
    }

    private boolean checkLocationPermission() {
        int permisionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
        return permisionState == PackageManager.PERMISSION_GRANTED;

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case CHECK_SETTINGS_CODE:
                switch (resultCode) {
                    case Activity
                            .RESULT_OK:
                        Log.d("AminActivity", "User has agreed to change locations settings");
                        startLocatinUpdate();

                    case Activity.RESULT_CANCELED:
                        Log.d("AminActivity", "User has NOT agreed to change locations settings");
                        locationUpdateActive = false;
                        startLocationUpdateButton.setEnabled(true);
                        stopLocationUpdateButton.setEnabled(false);
                        updateLocationUi();
                        break;
                }
                break;
        }


    }

    private void buildLocationSettingRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);

    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                currentLocation = locationResult.getLastLocation();
                updateLocationUi();

            }
        };
    }

    private void updateLocationUi() {
        locationTextView.setText("" + currentLocation.getLatitude() + "/" + currentLocation.getLongitude());
        locationUpdateTimeTextView.setText(DateFormat.getTimeInstance().format(new Date()));
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(3000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }
}