package com.pdp.krystian.nocall60;

import android.app.Service;
import android.content.Intent;
import android.database.Cursor;
import android.os.IBinder;
import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.CalendarContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.widget.Toast;

import com.google.android.gms.nearby.messages.Distance;

import java.util.Calendar;
import java.util.Date;

public class MyBackgroundService extends Service {
    public MyBackgroundService() {
    }

    private LocationListener locationListener;
    private LocationManager locationManager;
    // tworzenie obiektu typu AudioManager
    private AudioManager myAudioManager;
    private Cursor cursor;

    private double longitude;
    private double latitude;
    private double EventcoridnatesLongitude;
    private double EventcoridnatesLatitude;

    private int EventStart;
    private int EventEnd;

    private String EventLocation;
    private String[] Eventcoordinates;

    private Date EventEndValue;
    private Date EventStartValue;
    private Date currentTime;

    private Boolean sameTime;
    private Boolean sameLocation;

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {

        myAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {

                longitude=location.getLongitude();
                latitude=location.getLatitude();
                // wywołanie funkcji pobierajacej dane z kalendarza
                Toast.makeText(MyBackgroundService.this, "Koordynaty:"+ longitude + latitude, Toast.LENGTH_SHORT).show();

                GetDataAboutEventFromCalendar();

                Intent i = new Intent("location_update");
                i.putExtra("longitude",  location.getLatitude());
                i.putExtra("latitude", location.getLatitude());
                sendBroadcast(i);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        };

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        UpdateLocalization();

    }
    public void UpdateLocalization(){
        //noinspection MissingPermission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        // minTime [ms],   minDistance [m]
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, locationListener);


    }

    public void GetDataAboutEventFromCalendar()
    {
        cursor =  getContentResolver().query(CalendarContract.Events.CONTENT_URI, null, null, null, null);
        while (cursor.moveToNext()){
            if(cursor!= null){

                int EventID = cursor.getColumnIndex(CalendarContract.Events._ID);
                int EventTitle = cursor.getColumnIndex(CalendarContract.Events.TITLE);
                int EventTime = cursor.getColumnIndex(CalendarContract.Events.ORIGINAL_INSTANCE_TIME);
                int EventLocationIndex = cursor.getColumnIndex(CalendarContract.Events.EVENT_LOCATION);
                 EventEnd = cursor.getColumnIndex(CalendarContract.Events.DTEND);
                 EventStart = cursor.getColumnIndex(CalendarContract.Events.DTSTART);

                String titleValue = cursor.getString(EventTitle);
                EventLocation= cursor.getString(EventLocationIndex);
                if(EventLocation != null){
                    Eventcoordinates = EventLocation.split(" ");

                    EventcoridnatesLongitude = Double.parseDouble(Eventcoordinates[1]);
                    EventcoridnatesLatitude= Double.parseDouble(Eventcoordinates[0]);
                }
                else{
                    EventcoridnatesLongitude = 0;
                    EventcoridnatesLatitude= 0;

                }


                EventEndValue = new Date(cursor.getLong(EventEnd));
                EventStartValue = new Date(cursor.getLong(EventStart));

                if (CheckData() == true)
                {
                    IsUserOnEvent();
                }

            }
        }
    }
    // funkcja przyrównująca datę wydarzenia z czasem aktualnym i sprawdza czy owe wydarzenie dzieje się teraz
    public boolean CheckData(){
        currentTime = Calendar.getInstance().getTime();

        if (currentTime.getYear() == EventStartValue.getYear() &&
                currentTime.getMonth() == EventStartValue.getMonth() &&
                currentTime.getDay() == EventStartValue.getDay() &&
                (currentTime.getHours() == EventStartValue.getHours() &&
                        currentTime.getMinutes() >= EventStartValue.getMinutes() ||
                        currentTime.getHours() > EventStartValue.getHours()) &&
                (currentTime.getHours() == EventEndValue.getHours() &&
                        currentTime.getMinutes() <= EventEndValue.getMinutes() ||
                        currentTime.getHours() < EventEndValue.getHours())) {
            sameTime = true;
            return true;
        }
        else
        {
            sameTime=false;
            return false;
        }
    }
    // funkcja sprawdzająca na podstawie polozenia GPS i godziny czy uzytkownik znajduje sie obecnie na waznym wydarzeniu,
    // na ktorym powinien miec wyciszony telefon
    public void IsUserOnEvent(){

        sameLocation = false;
        double distance= getDistanceFromLatLonInKm(latitude,longitude,EventcoridnatesLatitude, EventcoridnatesLongitude);
        // margines błędu = 25metry
        if ( distance <= 0.025)
        {
            sameLocation= true;
        }
        else{
            sameLocation=false;
        }

        if(sameTime==true && sameLocation==true){
            if (myAudioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL){
                myAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                Toast.makeText(MyBackgroundService.this, "MUTE!", Toast.LENGTH_SHORT).show();
                //Toast.makeText(MyBackgroundService.this, "Dystans:"+ distance + "km", Toast.LENGTH_SHORT).show();
            }

        }
        else{
            if (myAudioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT){
                myAudioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                Toast.makeText(MyBackgroundService.this, "UNMUTE!", Toast.LENGTH_SHORT).show();
              //  Toast.makeText(MyBackgroundService.this, "Dystans:"+ distance + "km", Toast.LENGTH_SHORT).show();
            }
        }

    }

    // formuła obliczająca odległość między aktualnym położeniem,
    // a miejscem wydarzenia zapisanego w kalanedarzu użytkownika na podstawie koordynatów
    private double getDistanceFromLatLonInKm(double lat1,double lon1, double lat2, double lon2) {
        int R = 6371; // Radius of the earth in km
        double dLat = deg2rad(lat2-lat1);  // deg2rad below
        double dLon = deg2rad(lon2-lon1);
        double a =
                Math.sin(dLat/2) * Math.sin(dLat/2) +
                        Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) *
                                Math.sin(dLon/2) * Math.sin(dLon/2)
                ;
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        double d = R * c; // Distance in km
        return d;
    }

    // zamiana stopni na radiany
    private double  deg2rad(double deg) {
        return deg * (Math.PI/180);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        if(locationManager != null){
            //noinspection MissingPermission
            locationManager.removeUpdates(locationListener);
        }
    }
}
