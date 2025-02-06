package com.example.ohmyhazard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient fusedLocationProviderClient;

    DatabaseReference databaseUsers;
    private EditText location, name;
    private Spinner hazard;
    private Button reportBtn;

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);

        ImageButton drawerButton = findViewById(R.id.drawer_button);
        drawerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    drawerLayout.openDrawer(GravityCompat.END);
                }
            }
        });

        // Ensure NavigationView listens for item selections
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.home) {
                    startActivity(new Intent(ReportActivity.this, MainActivity.class));
                } else if (itemId == R.id.profile) {
                    startActivity(new Intent(ReportActivity.this, ProfileActivity.class));
                } else if (itemId == R.id.news) {
                    startActivity(new Intent(ReportActivity.this, NewsActivity.class));
                } else if (itemId == R.id.report) {
                    Toast.makeText(ReportActivity.this, getString(R.string.report), Toast.LENGTH_SHORT).show();
                }
                drawerLayout.closeDrawer(GravityCompat.END);  // Close the drawer from the right
                return true;
            }
        });

        // Initialize Firebase
        FirebaseApp.initializeApp(this);
        databaseUsers = FirebaseDatabase.getInstance().getReference();

        // Initialize UI elements
        location = findViewById(R.id.locationcode);
        name = findViewById(R.id.textFullName);
        hazard = findViewById(R.id.spinnerHazardType);
        reportBtn = findViewById(R.id.buttonSubmit);

        location.setVisibility(View.GONE);

        reportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadData();
            }
        });

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        fusedLocationProviderClient = (FusedLocationProviderClient) LocationServices.getFusedLocationProviderClient(this);

        Dexter.withContext(getApplicationContext()).withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        getCurrentLocation();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {
                        // Handle permission denied
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                        permissionToken.continuePermissionRequest();
                    }
                }).check();

        // Create a new FloatingActionButton for reset
        FloatingActionButton resetButton = new FloatingActionButton(this);
        resetButton.setImageResource(R.drawable.reset); // Set your icon here
        resetButton.setSize(FloatingActionButton.SIZE_NORMAL);
        resetButton.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.main))); // Set background tint from colors.xml
        resetButton.setRippleColor(getResources().getColor(android.R.color.white));
        resetButton.setElevation(12);
        resetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetMapToCurrentLocation();
            }
        });

        // Set layout parameters for the new button
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.BOTTOM | Gravity.END;
        layoutParams.bottomMargin = (int) getResources().getDimension(R.dimen.fab_margin);
        layoutParams.rightMargin = (int) getResources().getDimension(R.dimen.fab_margin);
        ((FrameLayout) findViewById(R.id.map)).addView(resetButton, layoutParams);
    }

    public void getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(@NonNull GoogleMap googleMap) {
                        onMapReadyCallback(location, googleMap);
                    }
                });
            }
        });
    }

    private void uploadData() {

        String userName = name.getText().toString().trim();
        String selectedHazard = hazard.getSelectedItem().toString();
        String userLocation = location.getText().toString().trim();

        if (userName.isEmpty() || userLocation.isEmpty() ) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        String id = databaseUsers.push().getKey();

        // Get the current date and time
        String time = getCurrentTime();
        String date = getCurrentDate();

        User user = new User(id, userName, selectedHazard, userLocation, time, date);
        databaseUsers.child("users").child(id).setValue(user)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(ReportActivity.this, "Report Submitted", Toast.LENGTH_SHORT).show();
                            clearForm();
                        }
                    }
                });
    }

    private void clearForm() {
        name.setText("");
        location.setText("");
        hazard.setSelection(0);
        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                googleMap.clear();                // Clear all markers from the map
            }
        });
    }

    private String getCurrentTime() {
        // Get current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return sdf.format(new Date());
    }
    private String getCurrentDate() {
        // Get current date and time
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault());
        return sdf.format(new Date());
    }

    public void resetMapToCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Task<Location> task = fusedLocationProviderClient.getLastLocation();
        task.addOnSuccessListener(new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                supportMapFragment.getMapAsync(new OnMapReadyCallback() {
                    @Override
                    public void onMapReady(@NonNull GoogleMap googleMap) {
                        onMapReadyCallback(location, googleMap);
                    }
                });
            }
        });
    }

    private void onMapReadyCallback(Location location, GoogleMap googleMap) {
        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Show a marker at the clicked position
                MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Pinned Location");
                googleMap.clear(); // Clear existing markers
                googleMap.addMarker(markerOptions);

                // Update EditText with location details
                updateLocationDetails(latLng);
            }
        });

        if (location != null) {
            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Current Location !");
            googleMap.clear(); // Clear existing markers
            googleMap.addMarker(markerOptions);
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));

            // Update EditText with current location details
            updateLocationDetails(latLng);
        } else {
            Toast.makeText(ReportActivity.this, "Please turn on your location permissions", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateLocationDetails(LatLng latLng) {

        EditText linkEditText = findViewById(R.id.locationcode);

        // Set the text in the EditText views with location details
        linkEditText.setText(latLng.latitude + ", " + latLng.longitude);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            // Handle toolbar back button click
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
