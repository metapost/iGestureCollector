package com.example.haipengwang.igesturecollector;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Environment;
import android.support.v4.os.EnvironmentCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    float down_x = 0;
    float down_y = 0;
    float up_x, up_y;

    private SensorManager mSensorManager = null;

    private long t; // timestamp
    private float x, y, z; // x, y, z axis of sensor

    private boolean bAcc = false;
    private boolean bLAcc = false;
    private boolean bGyro = false;
    private boolean bMag = false; // magnetometer

    private BufferedWriter mWriterAcc, mWriterLAcc, mWriterGyro, mWriterMag;
    public File mAccFile = null; // File class in java is just the representation of path of filename, so it doesn't need to close and open.
    private File mLAccFile = null;
    private File mGyroFile = null;
    private File mMagFile = null;

    private String mAccFilename, mLAccFilename, mGyroFilename, mMagFilename;

    private TextView msamplingrate;
    private Button mBtnTouch;

    private static final float NS2S = 1.0f / 1000000000.0f; // nano second to second

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ((TextView)findViewById(R.id.tv_speed)).setVisibility(View.GONE);
        ((RadioGroup) findViewById(R.id.radiogroup_speed)).setVisibility(View.GONE);

        ((TextView)findViewById(R.id.tv_gesture)).setVisibility(View.GONE);
        ((RadioGroup)findViewById(R.id.radiogroup_type)).setVisibility(View.GONE);

        ((TextView)findViewById(R.id.tv_seq)).setVisibility(View.GONE);
        ((TextView)findViewById(R.id.tv_seq_symbols)).setVisibility(View.GONE);


        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        msamplingrate = (TextView)findViewById(R.id.samplingrate);

        mBtnTouch = (Button)findViewById(R.id.btn_collectdata);
        mBtnTouch.setOnTouchListener(new MyBtnOnTouchListener());

        bAcc = ((CheckBox)findViewById(R.id.checkbox_acc)).isChecked();
        bLAcc = ((CheckBox)findViewById(R.id.checkbox_lacc)).isChecked();
        bGyro = ((CheckBox)findViewById(R.id.checkbox_gyro)).isChecked();
        bMag = ((CheckBox)findViewById(R.id.checkbox_mag)).isChecked();


        // output range of sensor
        Toast.makeText(this, "Acc Range is " + Float.toString(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).getMaximumRange()), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Acc Resolution is " + Float.toString(mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER).getResolution()), Toast.LENGTH_SHORT).show();

        Toast.makeText(this, "Gyro Range is " + Float.toString(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE).getMaximumRange()), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Gyro Range is " + Float.toString(mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE).getResolution()), Toast.LENGTH_SHORT).show();

        Toast.makeText(this, "Mag Range is " + Float.toString(mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).getMaximumRange()), Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "Mag Range is " + Float.toString(mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD).getResolution()), Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mSensorManager.unregisterListener(this); // disable sensor
        try {
            if (mWriterAcc != null) {
                mWriterAcc.flush();
                mWriterAcc.close();
                mWriterAcc = null;
                mAccFile = null;
            }
            if (mWriterLAcc != null) {
                mWriterLAcc.flush();
                mWriterLAcc.close();
                mWriterLAcc = null;
                mLAccFile = null;
            }
            if (mWriterGyro != null) {
                mWriterGyro.flush();
                mWriterGyro.close();
                mWriterGyro = null;
                mGyroFile = null;
            }
            if (mWriterMag != null) {
                mWriterMag.flush();
                mWriterMag.close();
                mWriterMag = null;
                mMagFile = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(this); // disable sensor
        try {
            if (mWriterAcc != null) {
                mWriterAcc.flush();
                mWriterAcc.close();
                mWriterAcc = null;
                //mAccFile = null;
            }
            if (mWriterLAcc != null) {
                mWriterLAcc.flush();
                mWriterLAcc.close();
                mWriterLAcc = null;
                //mLAccFile = null;
            }
            if (mWriterGyro != null) {
                mWriterGyro.flush();
                mWriterGyro.close();
                mWriterGyro = null;
                mGyroFile = null;
            }
            if (mWriterMag != null) {
                mWriterMag.flush();
                mWriterMag.close();
                mWriterMag = null;
                mMagFile = null;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void registerSensorManagerListeners() {
        // int fs = 10000;
        int fs = 120; // 120Hz best sampling rate from the paper of mike "Tutorial: Implementing a Pedestrian Tracker Using Inertial Sensors"
        msamplingrate.setText("Sampling rate: " + String.valueOf(fs) + "Hz");

        if (bAcc)
            //mSensorManager.registerListener(MainActivity.this, mAcc, 10000); // 10000 millisecond, 100Hz sampling rate
            mSensorManager.registerListener(MainActivity.this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    1000000/fs);
        //SensorManager.SENSOR_DELAY_FASTEST);
        if (bLAcc)
            mSensorManager.registerListener(MainActivity.this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                    1000000/fs);
        if (bGyro)
            mSensorManager.registerListener(MainActivity.this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                    1000000/fs);
        if (bMag)
            mSensorManager.registerListener(MainActivity.this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                    1000000/fs);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                t = event.timestamp;
                //Toast.makeText(this, "yessssss", Toast.LENGTH_SHORT).show();

                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                //TextView tvAcc = (TextView) findViewById(R.id.acc);
                //tvAcc.setText(Float.toString(y));

                try {
                    mWriterAcc.write(
                            Long.toString(t) + " " + // timestamp
                                    Float.toString(x) + " " + // x
                                    Float.toString(y) + " " + // y
                                    Float.toString(z) + " " + // z
                                    "\n");// newline
                } catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                t = event.timestamp;

                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                try {
                    mWriterLAcc.write(
                            Long.toString(t) + " " + // timestamp
                                    Float.toString(x) + " " + // x
                                    Float.toString(y) + " " + // y
                                    Float.toString(z) + " " + // z
                                    "\n");// newline
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case Sensor.TYPE_GYROSCOPE:
                t = event.timestamp;

                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                try {
                    mWriterGyro.write(
                            Long.toString(t) + " " + // timestamp
                                    Float.toString(x) + " " + // x
                                    Float.toString(y) + " " + // y
                                    Float.toString(z) + " " + // z
                                    "\n");// newline
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                t = event.timestamp;

                x = event.values[0];
                y = event.values[1];
                z = event.values[2];

                try {
                    mWriterMag.write(
                            Long.toString(t) + " " + // timestamp
                                    Float.toString(x) + " " + // x
                                    Float.toString(y) + " " + // y
                                    Float.toString(z) + " " + // z
                                    "\n");// newline
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case Sensor.TYPE_STEP_COUNTER:
                //Toast.makeText(MainActivity.this, "step counter is"+ Float.toString(event.values[0]), Toast.LENGTH_SHORT).show();
                break;
        }
    }

    public void onCheckBoxClicked(View view) {
        // Is the button now checked?
        boolean checked = ((CheckBox) view).isChecked();

        switch (view.getId()) {
            case R.id.checkbox_acc:
                if (checked)
                    bAcc = true;
                else
                    bAcc = false;
                break;
            case R.id.checkbox_lacc:
                if (checked)
                    bLAcc = true;
                else
                    bLAcc = false;
                break;
            case R.id.checkbox_gyro:
                if (checked)
                    bGyro = true;
                else
                    bGyro = false;
                break;
            case R.id.checkbox_mag:
                if (checked)
                    bMag = true;
                else
                    bMag = false;
                break;
        }
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }

        return false;
    }

    public void onRadioIndClicked(View v) {
        ((TextView)findViewById(R.id.tv_speed)).setVisibility(View.VISIBLE);
        ((RadioGroup) findViewById(R.id.radiogroup_speed)).setVisibility(View.VISIBLE);

        ((RadioButton)findViewById(R.id.radio_mixed)).setVisibility(View.GONE);

        ((TextView)findViewById(R.id.tv_gesture)).setVisibility(View.VISIBLE);
        ((RadioGroup)findViewById(R.id.radiogroup_type)).setVisibility(View.VISIBLE);

        ((TextView)findViewById(R.id.tv_seq)).setVisibility(View.GONE);
        ((TextView)findViewById(R.id.tv_seq_symbols)).setVisibility(View.GONE);

        if(!((Button)findViewById(R.id.btn_collectdata)).isEnabled())
            ((Button)findViewById(R.id.btn_collectdata)).setEnabled(true);
    }

    public void onRadioSeqClicked(View v) {
        ((TextView)findViewById(R.id.tv_seq)).setVisibility(View.VISIBLE);
        TextView tmp = (TextView)findViewById(R.id.tv_seq_symbols);
        tmp.setVisibility(View.VISIBLE);
        ((RadioButton)findViewById(R.id.radio_mixed)).setVisibility(View.VISIBLE);

        ((TextView)findViewById(R.id.tv_gesture)).setVisibility(View.GONE);
        ((RadioGroup)findViewById(R.id.radiogroup_type)).setVisibility(View.GONE);

        String seq = "";
        int g_min = 0; // 0: O, 1: C, 2: e, 3: V, 4: W, 5: Y, 6: U
        int g_max = 6;

        // length of sequence
        int min = 2; // min 2
        int max = 7; // max 7
        Random r = new Random();
        r.setSeed(System.currentTimeMillis());
        int len = r.nextInt(max - min + 1) + min;

        int random;
        // how long the sequence
        for (int i=1; i<=len; i++) {
            random = r.nextInt((g_max - g_min) + 1) + g_min;
            switch (random) {
                case 0:
                    //seq += "\u21E2" + "O";
                    seq += "O";
                    break;
                case 1:
                    seq += "C";
                    break;
                case 2:
                    seq += "e";
                    break;
                case 3:
                    seq += "V";
                    break;
                case 4:
                    seq += "W";
                    break;
                case 5:
                    seq += "y";
                    break;
                case 6:
                    seq += "U";
                    break;
            }
        }
        tmp.setText(seq);
        if(!((Button)findViewById(R.id.btn_collectdata)).isEnabled())
            ((Button)findViewById(R.id.btn_collectdata)).setEnabled(true);

    }

    class MyBtnOnTouchListener implements View.OnTouchListener {

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // touch the collect data button to start data collection
            // make filename and path

            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                RadioGroup xx = (RadioGroup)findViewById(R.id.radiogroup_speed);
                xx.setVisibility(View.VISIBLE);

                down_x = event.getX();
                down_y = event.getY();

                String sdcard = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
                DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
                String date = df.format(Calendar.getInstance().getTime());

                TextView name = (TextView) findViewById(R.id.edittext_name);
                TextView age = (TextView) findViewById(R.id.edittext_age);

                String gender = null;
                String speed = null;
                String type = null;
                String mode = null;

                switch (((RadioGroup) findViewById(R.id.radiogroup_speed)).getCheckedRadioButtonId()) {
                    case R.id.radio_normal:
                        speed = "Normal";
                        break;
                    case R.id.radio_slow:
                        speed = "Slow";
                        break;
                    case R.id.radio_fast:
                        speed = "Fast";
                        break;
                    case R.id.radio_mixed:
                        speed = "Mixed";
                        break;
                }
                switch (((RadioGroup) findViewById(R.id.radiogroup_gender)).getCheckedRadioButtonId()) {
                    case R.id.radio_male:
                        gender = "M";
                        break;
                    case R.id.radio_female:
                        gender = "F";
                        break;
                }
                switch (((RadioGroup)findViewById(R.id.radiogroup_modality)).getCheckedRadioButtonId()) {
                    case R.id.radio_ind:
                        switch (((RadioGroup) findViewById(R.id.radiogroup_type)).getCheckedRadioButtonId()) {
                            case R.id.radio_O:
                                type = "O";
                                break;
                            case R.id.radio_C:
                                type = "C";
                                break;
                            case R.id.radio_e:
                                type = "e";
                                break;
                            case R.id.radio_V:
                                type = "V";
                                break;
                            case R.id.radio_U:
                                type = "U";
                                break;
                            case R.id.radio_W:
                                type = "W";
                                break;
                            case R.id.radio_y:
                                type = "y";
                                break;
                            case R.id.radio_x:
                                type = "x";
                                break;
                        }
                        break;
                    case R.id.radio_seq:
                        type = (((TextView)findViewById(R.id.tv_seq_symbols)).getText()).toString();
                        break;
                }

                switch (((RadioGroup) findViewById(R.id.radiogroup_mode)).getCheckedRadioButtonId()) {
                    case R.id.radio_sit:
                        mode = "sit";
                        break;
                    case R.id.radio_stand:
                        mode = "stand";
                        break;
                    case R.id.radio_walk:
                        mode = "walk";
                        break;
                }

                String file_ext = "_" +
                        name.getText().toString() + "_" +
                        gender + "_" +
                        age.getText().toString() + "_" +
                        speed + "_" +
                        type + "_" +
                        mode;

                if (bAcc) {
                    mAccFilename = sdcard + "/" + date + file_ext;
                    String ext =  ".acc";
                    if (mAccFile == null)
                        mAccFile = new File(mAccFilename + ext);
                }
                if (bLAcc) {
                    mLAccFilename = sdcard + "/" + date + file_ext;
                    String ext = ".lacc";
                    if (mLAccFile == null) // the File class is just a representation of the filename and path.
                        mLAccFile = new File(mLAccFilename + ext);

                }
                if (bGyro) {
                    mGyroFilename = sdcard + "/" + date + file_ext;
                    String ext = ".gyro";
                    if (mGyroFile == null)
                        mGyroFile = new File(mGyroFilename + ext);
                }

                if (bMag) {
                    mMagFilename = sdcard + "/" + date + file_ext;
                    String ext = ".mag";
                    if (mMagFile == null)
                        mMagFile = new File(mMagFilename + ext);
                }

                // file writer
                if (isExternalStorageWritable()) {
                    try {
                        if (bAcc)
                            //character-based buffered writer
                            mWriterAcc = new BufferedWriter(new FileWriter(mAccFile, true)); // appends to file
                        if (bLAcc)
                            mWriterLAcc = new BufferedWriter(new FileWriter(mLAccFile, true));
                        if (bGyro)
                            mWriterGyro = new BufferedWriter(new FileWriter(mGyroFile, true));
                        if (bMag)
                            mWriterMag = new BufferedWriter(new FileWriter(mMagFile, true));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                // sensors registering
                registerSensorManagerListeners();

                mBtnTouch.setText("Collecting now ...");
                mBtnTouch.setBackgroundColor(0xFF0000FF);
            } else if (event.getAction() == MotionEvent.ACTION_UP) {
                // release touch to stop collection
                mSensorManager.unregisterListener(MainActivity.this);

                up_x = Math.abs(event.getX() - down_x);
                up_y = Math.abs(event.getY() - down_y);

                double dis = Math.sqrt(up_x*up_x + up_y*up_y);
                DisplayMetrics metrics = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metrics);
                int th = metrics.widthPixels/3;

                if (dis <= th) {
                    // up point closer to down point, save file
                    mBtnTouch.setText("Done, saved! do next one.");
                    mBtnTouch.setBackgroundColor(0xFF00FF00);
                    if (bAcc) {
                        try {
                            mWriterAcc.flush();
                            mWriterAcc.close();
                            mWriterAcc = null;
                            mAccFile = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (bLAcc) {
                        try {
                            mWriterLAcc.flush();
                            mWriterLAcc.close();
                            mWriterLAcc = null;
                            mLAccFile = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (bGyro) {
                        try {
                            mWriterGyro.flush();
                            mWriterGyro.close();
                            mWriterGyro = null;
                            mGyroFile = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (bMag) {
                        try {
                            mWriterMag.flush();
                            mWriterMag.close();
                            mWriterMag = null;
                            mMagFile = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (dis > th) {
                    // up point far away down point, delete file
                    mBtnTouch.setText("Deleted! Redo it.");
                    mBtnTouch.setBackgroundColor(0xFFFF0000);
                    if (bAcc) {
                        try {
                            //mWriterAcc.flush();
                            mWriterAcc.close();
                            mWriterAcc = null;
                            mAccFile.delete();
                            mAccFile = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (bLAcc) {
                        try {
                            //mWriterLAcc.flush();
                            mWriterLAcc.close();
                            mWriterLAcc = null;
                            mLAccFile.delete();
                            mLAccFile = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (bGyro) {
                        try {
                            //mWriterGyro.flush();
                            mWriterGyro.close();
                            mWriterGyro = null;
                            mGyroFile.delete();
                            mGyroFile = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (bMag) {
                        try {
                            //mWriterMag.flush();
                            mWriterMag.close();
                            mWriterMag = null;
                            mMagFile.delete();
                            mMagFile = null;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return true;
        }
    }
}