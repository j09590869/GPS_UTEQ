package com.example.gpsuteq;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback {
    GoogleMap Mapa;
    Marker marker = null;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    DatabaseReference coordenadasRef;
    FirebaseDatabase database = FirebaseDatabase.getInstance();
    DatabaseReference LatitudinRef, LongitudinRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Conectamos el fragment a Google Maps
        SupportMapFragment mapFragment = (SupportMapFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        TextView txtLatitud = findViewById(R.id.txtLatitud);
        TextView txtLongitud = findViewById(R.id.txtLongitud);

        LatitudinRef = database.getReference("coordenadas/latitud");
        LongitudinRef = database.getReference("coordenadas/longitud");
        coordenadasRef = database.getReference("coordenadas");

        LatitudinRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.exists() && snapshot.getValue() != null) {
                        Double latitud = snapshot.getValue(Double.class);
                        if (latitud != null) {
                            txtLatitud.setText(String.format("%.5f", latitud));
                        } else {
                            txtLatitud.setText("Latitud no disponible");
                        }
                    } else {
                        txtLatitud.setText("Latitud no disponible");
                    }
                } catch (Exception e) {
                    txtLatitud.setText("Error: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error al leer latitud: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        LongitudinRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                try {
                    if (snapshot.exists() && snapshot.getValue() != null) {
                        Double longitud = snapshot.getValue(Double.class);
                        if (longitud != null) {
                            txtLongitud.setText(String.format("%.5f", longitud));
                        } else {
                            txtLongitud.setText("Longitud no disponible");
                        }
                    } else {
                        txtLongitud.setText("Longitud no disponible");
                    }
                } catch (Exception e) {
                    txtLongitud.setText("Error: " + e.getMessage());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error al leer longitud: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        setupLocationUpdates();

        coordenadasRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Double lat = snapshot.child("latitud").getValue(Double.class);
                Double lng = snapshot.child("longitud").getValue(Double.class);

                if (lat != null && lng != null) {
                    actualizarMarcadorMapa(lat, lng);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "Error al actualizar marcador: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            return;
        }

        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(5000); // Cada 5 segundos

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    Location location = locationResult.getLastLocation();
                    grabarNuevaPosicionGPS(location);
                }
            }
        };

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
    }

    private void grabarNuevaPosicionGPS(Location location) {
        TextView txtLatitud = findViewById(R.id.txtLatitud);
        TextView txtLongitud = findViewById(R.id.txtLongitud);
        txtLatitud.setText(String.format("%.5f", location.getLatitude()));
        txtLongitud.setText(String.format("%.5f", location.getLongitude()));

        coordenadasRef.child("latitud").setValue(location.getLatitude());
        coordenadasRef.child("longitud").setValue(location.getLongitude());
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        Mapa = googleMap;
        Mapa.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        Mapa.getUiSettings().setZoomControlsEnabled(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupLocationUpdates();
        } else {
            Toast.makeText(this, "Acceso NO permitido", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    private void actualizarMarcadorMapa(double lat, double lng) {
        LatLng latLng = new LatLng(lat, lng);

        if (marker == null)
            marker = Mapa.addMarker(new MarkerOptions().position(latLng).title("Tu Pos"));
        else
            marker.setPosition(latLng);

        Mapa.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
    }
}