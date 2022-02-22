package com.kdg.toast.plugin;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class PedometerService extends Service implements SensorEventListener {

    public SharedPreferences sharedPreferences;
    String TAG = "PEDOMETER";
    SensorManager sensorManager;
    boolean running;
    Date currentDate;
    Date initialDate;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final NotificationChannel notificationChannel = new NotificationChannel(
                    "PedometerLib",
                    "Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            final NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    private void startNotification(){
        Log.i(TAG, "onstartNotification: STARTED1");
        String input = "Counting your steps...";

        Notification notification = new NotificationCompat.Builder(this, "PedometerLib")
                .setContentTitle("Today: 8501 Steps")
                .setContentText("Yesterday: 503 steps")
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setShowWhen(false).setOnlyAlertOnce(true)
                .build();
        startForeground(112, notification);

        //     .setContentIntent(pendingIntent)
    }

    private void updateNotification(int todaySteps,int yesterdaysSteps){
        Log.i(TAG, "onstartNotification: STARTED1");
        String input = "Counting your steps...";
        Notification notification = new NotificationCompat.Builder(this, "PedometerLib")
                .setContentTitle("Today: "+ todaySteps +" Steps")
                .setContentText("Yesterday: "+ yesterdaysSteps +" steps")
                .setSmallIcon(R.mipmap.ic_launcher_foreground)
                .setShowWhen(false).setOnlyAlertOnce(true)
                .build();
        startForeground(112, notification);
    }


    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate: CREATED"+Bridge.steps);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        loadData();
        saveSummarySteps(Bridge.summarySteps+Bridge.steps);
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i(TAG, "onTaskRemoved: REMOVED"+Bridge.steps);
        initSensorManager();

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        SimpleDateFormat spf = new SimpleDateFormat("yyyy-MM-dd");
        String todayDate = spf.format(Calendar.getInstance().getTime());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -1);
        String yesterdayDate =spf.format(cal.getTime());
        int todaySteps = getStepCountData("Select * from StepCount WHERE recordedOn = date('"+todayDate+"')");
        int yesterdaySteps =  getStepCountData("Select * from StepCount WHERE recordedOn = date('"+yesterdayDate+"')");
        updateNotification(todaySteps,yesterdaySteps);
        super.onCreate();
        Bridge.initialSteps=0;
        initSensorManager();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        initialDate = Calendar.getInstance().getTime();
        editor.putString(Bridge.INIT_DATE, currentDate.toString());
        editor.apply();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy: DESTROYED");
        disposeSensorManager();
        loadData();
        saveSummarySteps(Bridge.summarySteps+Bridge.steps);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Log.i(TAG, "onSensorChanged!!!!!!: "+sensorEvent.values[0]);

        if (Bridge.initialSteps==0){
            Log.i(TAG, "onSensorChanged: AWAKE");
            Bridge.initialSteps=(int) sensorEvent.values[0];
            Bridge.currentStep =(int) sensorEvent.values[0];
        }
        if (running){
            Bridge.steps=(int)sensorEvent.values[0]-Bridge.initialSteps;
            Bridge.currentStep = Bridge.steps-Bridge.steps_last;
            Log.i(TAG, "onSensorChanged: current steps: "+Bridge.steps);
            SimpleDateFormat spf = new SimpleDateFormat("yyyy-MM-dd");
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR,0);
            cal.set(Calendar.MINUTE,0);
            cal.set(Calendar.AM_PM,Calendar.PM);
            String todayDate = spf.format(cal.getTime());
            cal.add(Calendar.DATE, -1);
            String yesterdayDate =spf.format(cal.getTime());
            Log.i(TAG, "Got stepcount data for notificaiton" + todayDate + " " +yesterdayDate);
            //int todaySteps = this.getStepCountData("Select * from StepCount WHERE recordedOn BETWEEN datetime('2021-12-12') AND datetime('2021-12-14')");
            int todaySteps = getStepCountData("Select * from StepCount WHERE recordedOn = date('"+todayDate+"')");
            int yesterdaySteps =  getStepCountData("Select * from StepCount WHERE recordedOn = date('"+yesterdayDate+"')");
            //Log.i(TAG, "Got stepcount data for notificaiton" + todaySteps + " " +yesterdaySteps);
            updateNotification(todaySteps,yesterdaySteps);
            Log.i(TAG, "onSensorChanged: steps in this iteration: "+Bridge.currentStep);
            if(Bridge.currentStep>0)
                storeDataInSqliteDB(Bridge.currentStep);
            Bridge.steps_last = Bridge.steps;
            saveData(Bridge.steps);
        }
    }

    public int getStepCountData(String query){
        Log.i("PEDOMETER", "called stepcount data with query: "+query);
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase stepCountDB = dbHelper.getWritableDatabase();
        Cursor c = null;
        int steps = 0;
        try {
            c = stepCountDB.rawQuery(query, null);
            steps = 0;
            if (c.moveToFirst()) {
                do {
                    String column1 = c.getString(0);
                    String column2 = c.getString(1);
                    Log.i("PEDOMETER", "called stepcount data " + Integer.parseInt(column1) + " " + column2);
                    steps += Integer.parseInt(column1);
                } while (c.moveToNext());
            }
        }
        catch (SQLiteException e){
            stepCountDB.execSQL("CREATE TABLE IF NOT EXISTS StepCount(steps INTEGER,recordedOn text);");
            getStepCountData(query);
        }

        Log.i("PEDOMETER", "called stepcount data "+steps+ "steps");
        return steps;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {    }

    public void initSensorManager(){
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        running = true;
        Sensor countSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (countSensor!=null){
            sensorManager.registerListener(this,countSensor,SensorManager.SENSOR_DELAY_UI);
        }
        else {
            Toast.makeText(Bridge.myActivity,"Sensor Not Found (", Toast.LENGTH_LONG).show();
        }
    }
    public void disposeSensorManager(){
        running=false;
        sensorManager.unregisterListener(this);
    }

    public void saveData(int currentSteps) {

        SharedPreferences.Editor editor = sharedPreferences.edit();
        currentDate = Calendar.getInstance().getTime();
        editor.putString(Bridge.DATE, currentDate.toString());
        Log.i(TAG, "saveData: saved! "+currentSteps);
        int storedSteps =sharedPreferences.getInt(Bridge.STEPS,0);
        sharedPreferences.getInt(Bridge.STEPS,0);
        int stepsTaken = currentSteps-storedSteps;

        editor.putInt(Bridge.STEPS, currentSteps);
        editor.apply();
    }

    public void storeDataInSqliteDB(int stepsTaken){
        Log.i("PEDOMETER", "Storing Data in sqlite DB: "+stepsTaken+' ');
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        SQLiteDatabase stepCountDB = dbHelper.getReadableDatabase();
        stepCountDB.execSQL("CREATE TABLE IF NOT EXISTS StepCount(steps INTEGER,recordedOn text);");
        SimpleDateFormat dateFormatter = new SimpleDateFormat("YYYY-MM-DD");
        String currentDateStr = dateFormatter.format(currentDate);
        stepCountDB.execSQL("INSERT INTO StepCount VALUES("+stepsTaken+",date('now'))");
    }

    public void getStepsForDate(Date date){

    }

    public void saveSummarySteps(int stepsToSave) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        currentDate = Calendar.getInstance().getTime();
        editor.putString(Bridge.DATE, currentDate.toString());
        Log.i(TAG, "saveSummarySteps: saved! "+stepsToSave);
        editor.putInt("summarySteps", stepsToSave);
        editor.apply();
    }
    public void loadData() {
        Bridge.steps = sharedPreferences.getInt(Bridge.STEPS, 0);
        Bridge.summarySteps = sharedPreferences.getInt("summarySteps",0);
        Log.i(TAG, "loadData: steps"+Bridge.steps);
        Log.i(TAG, "loadData: summarySteps "+Bridge.summarySteps);
    }
}