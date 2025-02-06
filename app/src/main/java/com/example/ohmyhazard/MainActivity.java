package com.example.ohmyhazard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
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
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    SupportMapFragment supportMapFragment;
    FusedLocationProviderClient fusedLocationProviderClient;

    private FloatingActionButton resetLocationButton;

    private Map<String, Marker> markersMap = new HashMap<>();

    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle drawerToggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

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
                    Toast.makeText(MainActivity.this, getString(R.string.home), Toast.LENGTH_SHORT).show();
                } else if (itemId == R.id.profile) {
                    startActivity(new Intent(MainActivity.this, ProfileActivity.class));
                } else if (itemId == R.id.news) {
                    startActivity(new Intent(MainActivity.this, NewsActivity.class));
                } else if (itemId == R.id.report) {
                    startActivity(new Intent(MainActivity.this, ReportActivity.class));
                }
                drawerLayout.closeDrawer(GravityCompat.END);  // Close the drawer from the right
                return true;
            }
        });

        Button newsBtn = findViewById(R.id.newsfeed);
        Button reportBtn = findViewById(R.id.reporthazard);

        newsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                newsPage();
            }
        });

        reportBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                reportPage();
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
        layoutParams.bottomMargin = (int) getResources().getDimension(R.dimen.fab_margin) + 10;
        layoutParams.rightMargin = (int) getResources().getDimension(R.dimen.fab_margin);
        ((FrameLayout) findViewById(R.id.map)).addView(resetButton, layoutParams);

        supportMapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(@NonNull GoogleMap googleMap) {
                // Fetch and display markers on the map
                onMapReadyCallback(googleMap);
            }
        });
    }

    private void onMapReadyCallback(GoogleMap googleMap) {
        DatabaseReference reportsRef = FirebaseDatabase.getInstance().getReference().child("users");

        reportsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                googleMap.clear(); // Clear existing markers

                for (DataSnapshot reportSnapshot : dataSnapshot.getChildren()) {
                    Double latitude = reportSnapshot.child("latitude").getValue(Double.class);
                    Double longitude = reportSnapshot.child("longitude").getValue(Double.class);
                    String title = reportSnapshot.child("title").getValue(String.class);

                    // Check if latitude and longitude are not null
                    if (latitude != null && longitude != null) {
                        LatLng reportLatLng = new LatLng(latitude, longitude);

                        // Show a marker at the report's location with custom icon
                        MarkerOptions markerOptions = new MarkerOptions()
                                .position(reportLatLng)
                                .title(title != null ? title : "Unknown Hazard") // Avoid null titles
                                .icon(getMarkerIcon(title != null ? title : "Others"));

                        Marker marker = googleMap.addMarker(markerOptions);
                        markersMap.put(reportSnapshot.getKey(), marker); // Store marker with unique ID
                    } else {
                        // Log or handle reports with missing coordinates
                        Log.e("MapError", "Missing latitude/longitude for report: " + reportSnapshot.getKey());
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("FirebaseError", "Database error: " + databaseError.getMessage());
            }
        });
    }

    private BitmapDescriptor getMarkerIcon(String title) {
        // Define your mapping of titles to icon resources here
        switch (title) {
            case "Crash":
                return BitmapDescriptorFactory.fromResource(R.drawable.crash);
            case "Haze":
                return BitmapDescriptorFactory.fromResource(R.drawable.haze);
            case "Landslide":
                return BitmapDescriptorFactory.fromResource(R.drawable.landslide);
            case "Flood":
                return BitmapDescriptorFactory.fromResource(R.drawable.flood);
            case "Traffic":
                return BitmapDescriptorFactory.fromResource(R.drawable.traffic);
            case "Blocked Lane":
                return BitmapDescriptorFactory.fromResource(R.drawable.blockedlane);
            // Add cases for other titles as needed
            default:
                return BitmapDescriptorFactory.fromResource(R.drawable.other);
        }
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
                        if (location != null) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Current Location !");
                            googleMap.clear(); // Clear existing markers
                            googleMap.addMarker(markerOptions);
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        } else {
                            Toast.makeText(MainActivity.this, "Please turn on your location permissions", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
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
                        if (location != null) {
                            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                            MarkerOptions markerOptions = new MarkerOptions().position(latLng).title("Current Location !");
                            //googleMap.clear(); // Clear existing markers
                            //googleMap.addMarker(markerOptions);
                            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                        } else {
                            Toast.makeText(MainActivity.this, "Please turn on your location permissions", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.share_menu, menu); // Inflate menu from XML
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.menuShare) {
            shareApp();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void shareApp() {
        String shareText = "Check out this app for real-time hazard reports! Github: https://github.com/farisbasar/OhMyHazard";
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        startActivity(Intent.createChooser(shareIntent, "Share via"));
    }


    private void newsPage() {
        Intent intent = new Intent(MainActivity.this, NewsActivity.class);
        startActivity(intent);
    }

    private void reportPage() {
        Intent intent = new Intent(MainActivity.this, ReportActivity.class);
        startActivity(intent);
    }
}