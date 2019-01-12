package com.example.rafael_perez.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager; //first, we need a sensor manager.
    private Sensor magneticSensor=null, accelerometerSensor=null; //then, we declare the sensors.
    private boolean enabled=false;
    private TextView txt_compass_value;
    private ImageView img_compass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        txt_compass_value = findViewById(R.id.compass_value);
        img_compass = findViewById(R.id.compass);
        initSensors();
    }

    private void initSensors(){ //Initialize the sensors.
        sensorManager =(SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            magneticSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        //if sensors are null, means that the phone do not have the hardware sensors necessary for make the app to work.
        if (magneticSensor!=null && accelerometerSensor!=null) enabled = true;
        else txt_compass_value.setText(getString(R.string.sensors_unavailable));
    }

    public void startSensors(){
        if (enabled && sensorManager != null) {
            sensorManager.registerListener(this, accelerometerSensor, 40000);
            sensorManager.registerListener(this, magneticSensor, 40000);
            //I use 40000 microseconds as frequency, which is equivalent to 25 fps.
        }
    }

    public void stopSensors(){
        if (enabled && sensorManager != null) {
            sensorManager.unregisterListener(this, accelerometerSensor);
            sensorManager.unregisterListener(this, magneticSensor);
        }
    }

    private float[] magnetometerReading;
    private float[] accelerometerReading;

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        try{
            float alpha = 0.2f;
            if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER){ //Read accelerometer value.
                accelerometerReading = lowPassFilter(sensorEvent.values.clone(), accelerometerReading, alpha);
            }

            if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) { //Read magnetometer value.
                magnetometerReading = lowPassFilter(sensorEvent.values.clone(), magnetometerReading, alpha);
            }

            if (magnetometerReading!=null && accelerometerReading!=null) { //here we simply make some math conversions.
                float[] gravity = new float[9];
                float[] magnetic = new float[9];
                SensorManager.getRotationMatrix(gravity, magnetic, accelerometerReading, magnetometerReading);
                float[] orientationAngles = new float[3];
                float[] outGravity = new float[9];
                SensorManager.remapCoordinateSystem(gravity, SensorManager.AXIS_X,SensorManager.AXIS_Z, outGravity);
                SensorManager.getOrientation(outGravity, orientationAngles);
                int azimut = (int) Math.round(Math.toDegrees(-orientationAngles[0])); //this is the output value in degrees.
                //we add a "-" operator because we want to our compass image rotate at the opposite direction.
                int degrees = -azimut;
                if (degrees<0) degrees = degrees + 360; //we need to make this conversion because the previous math returns negative values.
                txt_compass_value.setText("Bearing: " + degrees + "°");
                img_compass.setRotation(azimut); //asign the rotatio value to our compass image.
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private float[] lowPassFilter(float[] input, float[] output, float alpha) {
        if ( output == null ){
            return input;
        }
        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    protected void onPause() {
        super.onPause();
        stopSensors(); //we must stop the sensors when the app is on pause state.
    }

    @Override
    protected void onResume() {
        super.onResume();
        startSensors(); //when the app resumes we need to restart the sensors again.
    }
}
