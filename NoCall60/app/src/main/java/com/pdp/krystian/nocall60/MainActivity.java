package com.pdp.krystian.nocall60;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Dialog;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

public class MainActivity extends AppCompatActivity {

    private int LOCATION_PERMISSION_CODE = 1;
    private int CALENDAR_PERMISSION_CODE = 1;
    private int INTERNET_PERMISSION_CODE = 1;

    private FusedLocationProviderClient client;
    private Cursor cursor;

    // wiadomośc od serwisu pracy w tle
    private BroadcastReceiver broadcastReceiver;

    // zmienne przechowujace kordynaty
    private TextView textViewLatitude;
    private TextView textViewLongitude;

    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    //latitude.append("\n" +intent.getExtras().get("latitude"));
                    textViewLatitude.append((String)intent.getExtras().get("latitude"));
                    textViewLongitude.append((String)intent.getExtras().get("longitude"));

                }
            };
        }
        registerReceiver(broadcastReceiver,new IntentFilter("location_update"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
    }

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        client = LocationServices.getFusedLocationProviderClient(this);

        // przycisk, służący do pobrania aktualnej lokalizacji
        Button buttonLocation = findViewById(R.id.buttonGetLocation);
        buttonLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                client.getLastLocation().addOnSuccessListener(MainActivity.this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {

                        if(location!= null){
                            //TextView textViewLocation = findViewById(R.id.textViewLocation);
                            //textViewLocation.setText(location.toString());
                            textViewLatitude = findViewById(R.id.textViewLatitude);
                            textViewLongitude = findViewById(R.id.textViewLongitude);
                            double LocationLatitude = location.getLatitude();
                            double LocationLongitude = location.getLongitude();
                            textViewLatitude.setText(String.valueOf(LocationLatitude));
                            textViewLongitude.setText(String.valueOf(LocationLongitude));
                        }
                    }
                });
            }
        });


        // pobranie danych o wydarzeniu
        Button buttonEventLocation = findViewById(R.id.buttonEventLocation);
        buttonEventLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }


                cursor =  getContentResolver().query(CalendarContract.Events.CONTENT_URI, null, null, null, null);
                while (cursor.moveToNext()){
                    if(cursor!= null){

                            int id_1 = cursor.getColumnIndex(CalendarContract.Events._ID);
                            int id_2 = cursor.getColumnIndex(CalendarContract.Events.TITLE);
                            int id_3 = cursor.getColumnIndex(CalendarContract.Events.ORIGINAL_INSTANCE_TIME);
                            int id_4 = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION);

                            String titleValue = cursor.getString(id_2);
                            String locationValue = cursor.getString(id_4);

                            Toast.makeText(MainActivity.this, titleValue + ", " + locationValue, Toast.LENGTH_SHORT).show();

                    }
                }
            }
        });



        // prośba o dostęp do lokalizacji
        Button buttonRequest = findViewById(R.id.button);
        buttonRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // jeżeli wyrażono zgodę to wyświetla o tym informację
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                        ContextCompat.checkSelfPermission(MainActivity.this,
                                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(MainActivity.this, "Już zezwolono na dostęp do lokalizacji", Toast.LENGTH_SHORT).show();
                }
                // jeżeli nie wyrażono zgody to wywołuje funkcję pytającą o tę zgodę
                else {
                    requestLocationPermission();
                }
            }
        });

        // prośba o dostęp do kalendarza
        Button buttonRequestCalendar = findViewById(R.id.buttonCalendar);
        buttonRequestCalendar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if( ContextCompat.checkSelfPermission( MainActivity.this,
                        Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(MainActivity.this, "Już zezwolono na dostęp do kalendarza!", Toast.LENGTH_SHORT).show();
                }
                else{
                    requestCalendarPermission();
                }
            }
        });

        // włączenie pracy w tle
        Button buttonBackground = findViewById(R.id.buttonBackground);
        buttonBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Intent myIntent = new Intent(MainActivity.this, MyBackgroundService.class);
                Intent myIntent = new Intent(getApplicationContext(), MyBackgroundService.class);
                startService(myIntent);
            }
        });

        // funkcja wywołująca metodę pytającą o dostęp do lokalizacji i kalendarza
        CheckPermissions();
    }

    // metoda wywołująca metody pytające o pozwolenie
    public void CheckPermissions(){
        requestCalendarPermission();
        requestLocationPermission();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    // funkcja pytająca o pozwolenie dostępu do kalendarza
    private void requestCalendarPermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CALENDAR)){
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because i must know wher you are")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.READ_CALENDAR}, CALENDAR_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        }else{
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_CALENDAR}, CALENDAR_PERMISSION_CODE);
        }
    }
    // funkcja pytająca o pozwolenie dostępu do lokalizacji
    private void requestLocationPermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION) &&
            ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_COARSE_LOCATION)){
            new AlertDialog.Builder(this)
                    .setTitle("Permission needed")
                    .setMessage("This permission is needed because i must know wher you are")
                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_CODE);
                        }
                    })
                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .create().show();
        }else{
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT);
            }
            else{
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT);
            }
        }
    }
}