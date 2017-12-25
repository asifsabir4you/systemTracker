package com.thereza.systemtracker.appUtil;

import android.Manifest;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.MediaController;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.thereza.systemtracker.MainActivity;
import com.thereza.systemtracker.R;
import com.thereza.systemtracker.filetracker.TotalFiles;
import com.thereza.systemtracker.simcardtracker.TelephonyInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.thereza.systemtracker.MainActivity.MY_PREFS_NAME;
import static com.thereza.systemtracker.MainActivity.MY_PREFS_NAME_FOR_FILE;

/**
 * Created by theReza on 11/27/2017.
 */

//sensor event listener for acceleration detect
public class MsgPushService extends Service implements SensorEventListener {

    public static final String MY_PREFS_NAME = "MyPrefsFile";

    //for sersor change
    private SensorManager sensorMan;
    private Sensor accelerometer;

    private float[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;
    public MediaPlayer mMediaPlayer; //have to make it static to call from main
    //-----------------charger event var declare
    private BroadcastReceiver mBatteryStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().equals(Intent.ACTION_POWER_CONNECTED)) {
                Toast.makeText(context, "Charger connected. alarm on disconnect", Toast.LENGTH_SHORT).show();

            } else {
                intent.getAction().equals(Intent.ACTION_POWER_DISCONNECTED);
                Toast.makeText(context, "Charger disconnected!", Toast.LENGTH_SHORT).show();
                    mMediaPlayer.start();

            }
        }
    };


    Handler mHandler = new Handler();

    InterstitialAd mInterstitialAd;

    @Override
    public void onCreate() {
        super.onCreate();
        //sensor init
        sensorMan = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        Log.d("media", "media init");
        //init alarm media
        mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.beep);
        mMediaPlayer.setLooping(true); // Set looping
        //init charger /power broadcast

        IntentFilter ifilter = new IntentFilter();
        ifilter.addAction(Intent.ACTION_POWER_CONNECTED);
        ifilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(mBatteryStateReceiver, ifilter);
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        sensorMan.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_UI);
        //unreg events
        //  sensorMan.unregisterListener(this);


        new Thread(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                while (true) {
                    try {
                        Thread.sleep(10000);
                        mHandler.post(new Runnable() {
                            StringBuilder data1 = new StringBuilder("");
                            StringBuilder data2 = new StringBuilder("");
                            PackageManager pm = getPackageManager();
                            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
                            FileWriter writer = null;

                            @Override
                            public void run() {

                                SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                final String email = emailAddress.getString("email", null);

                                final String toMailAddress = "intruderalert4mobile@gmail.com";
                                final String pass = "cuetcse12";
                                final GMailSender sender = new GMailSender(toMailAddress, pass);


                                //charging motion detection




                                /*
                                * Unlock Attempt Tracking and notify
                                *
                                * */

                                SharedPreferences lockPref = getSharedPreferences("LockEntry", MODE_PRIVATE);
                                Integer unlockAttempt = lockPref.getInt("unlockAttempt", 0);
                                SharedPreferences preftAttemp = getSharedPreferences("attemptRecord", MODE_PRIVATE);
                                String atemptNo = preftAttemp.getString("attempTcount", "Default");

                                final String dateTime = lockPref.getString("dateTime", "");
                                if (unlockAttempt >= Integer.parseInt(atemptNo)) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            public void run() {
                                                try {
                                                    sender.sendMail("Intruder Alert!",
                                                            "Intruder Alert detected someone tries to unlock your device on " + dateTime,
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor lockEditor = getApplicationContext().getSharedPreferences("LockEntry", MODE_PRIVATE).edit();
                                                    lockEditor.putInt("unlockAttempt", 0);
                                                    lockEditor.commit();

                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }
                                            }

                                        }).start();
                                    }
                                }


                                //file tracking
                                TotalFiles tt = new TotalFiles();
                                Integer audioSize = tt.geAudioFilePaths(getApplicationContext()).size();
                                Integer videoSize = tt.geVideoFilePaths(getApplicationContext()).size();
                                Integer imageSize = tt.geImagetFilePaths(getApplicationContext()).size();

                                SharedPreferences prefs_for_file = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE);
                                Integer audioFile = prefs_for_file.getInt("AUDIO", 0);
                                Integer videoFile = prefs_for_file.getInt("VIDEO", 0);
                                Integer imageFile = prefs_for_file.getInt("IMAGE", 0);

                                if (audioSize < audioFile && videoSize < videoFile && imageSize < imageFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer audioSize = tt.geAudioFilePaths(getApplicationContext()).size();
                                            Integer videoSize = tt.geVideoFilePaths(getApplicationContext()).size();
                                            Integer imageSize = tt.geImagetFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Delete!!",
                                                            "Intruder Alert detect someone Delete your Some Audio, Video and Image File from your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("AUDIO", audioSize);
                                                    editorrr.putInt("VIDEO", videoSize);
                                                    editorrr.putInt("IMAGE", imageSize);
                                                    editorrr.commit();

                                                    fullscreenAdd();

                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (audioSize < audioFile && videoSize < videoFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer audioSize = tt.geAudioFilePaths(getApplicationContext()).size();
                                            Integer videoSize = tt.geVideoFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Delete!!",
                                                            "Someone Delete your Some Audio and Video File from your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("AUDIO", audioSize);
                                                    editorrr.putInt("VIDEO", videoSize);
                                                    editorrr.commit();
                                                    fullscreenAdd();

                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (audioSize < audioFile && imageSize < imageFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer audioSize = tt.geAudioFilePaths(getApplicationContext()).size();
                                            Integer imageSize = tt.geImagetFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Delete!!",
                                                            "Someone Delete your Some Audio and Image File from your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("AUDIO", audioSize);
                                                    editorrr.putInt("IMAGE", imageSize);
                                                    editorrr.commit();
                                                    fullscreenAdd();

                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (videoSize < videoFile && imageSize < imageFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer videoSize = tt.geVideoFilePaths(getApplicationContext()).size();
                                            Integer imageSize = tt.geImagetFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Delete!!",
                                                            "Someone Delete your Some Video and Image File from your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("VIDEO", videoSize);
                                                    editorrr.putInt("IMAGE", imageSize);
                                                    editorrr.commit();
                                                    fullscreenAdd();

                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (audioSize < audioFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer audioSize = tt.geAudioFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Delete!!",
                                                            "Someone Delete your Some Audio File from your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("AUDIO", audioSize);
                                                    editorrr.commit();

                                                    fullscreenAdd();
                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (videoSize < videoFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer videoSize = tt.geVideoFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Delete!!",
                                                            "Someone Delete your Some Video File from your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("VIDEO", videoSize);
                                                    editorrr.commit();

                                                    fullscreenAdd();
                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (imageSize < imageFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer imageSize = tt.geImagetFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Delete!!",
                                                            "Someone Delete your Some Image File from your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("IMAGE", imageSize);
                                                    editorrr.commit();
                                                    fullscreenAdd();

                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (audioSize > audioFile && videoSize > videoFile && imageSize > imageFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer audioSize = tt.geAudioFilePaths(getApplicationContext()).size();
                                            Integer videoSize = tt.geVideoFilePaths(getApplicationContext()).size();
                                            Integer imageSize = tt.geImagetFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Add Alert!!",
                                                            "Intruder Alert detect someone Delete Some Audio, Video and Image File on your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("AUDIO", audioSize);
                                                    editorrr.putInt("VIDEO", videoSize);
                                                    editorrr.putInt("IMAGE", imageSize);
                                                    editorrr.commit();

                                                    fullscreenAdd();

                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (audioSize > audioFile && videoSize > videoFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer audioSize = tt.geAudioFilePaths(getApplicationContext()).size();
                                            Integer videoSize = tt.geVideoFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Add Alert!!",
                                                            "Intruder Alert detects Someone Add Some Audio and Video File from your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("AUDIO", audioSize);
                                                    editorrr.putInt("VIDEO", videoSize);
                                                    editorrr.commit();
                                                    fullscreenAdd();

                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (audioSize > audioFile && imageSize > imageFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer audioSize = tt.geAudioFilePaths(getApplicationContext()).size();
                                            Integer imageSize = tt.geImagetFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Add Alert!!",
                                                            "Intruder Alert detects Someone Add Some Audio and Image File on your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("AUDIO", audioSize);
                                                    editorrr.putInt("IMAGE", imageSize);
                                                    editorrr.commit();
                                                    fullscreenAdd();

                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (videoSize > videoFile && imageSize > imageFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer videoSize = tt.geVideoFilePaths(getApplicationContext()).size();
                                            Integer imageSize = tt.geImagetFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Add Alert!!",
                                                            "Intruder Alert detects Someone Add Some Video and Image File on your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("VIDEO", videoSize);
                                                    editorrr.putInt("IMAGE", imageSize);
                                                    editorrr.commit();
                                                    fullscreenAdd();

                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (audioSize > audioFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer audioSize = tt.geAudioFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Add Alert!!",
                                                            "Intruder Alert detects Someone Add Some Audio File on your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("AUDIO", audioSize);
                                                    editorrr.commit();

                                                    fullscreenAdd();
                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (videoSize > videoFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer videoSize = tt.geVideoFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Add Alert!!",
                                                            "Intruder Alert detects someone add Some Video File on your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("VIDEO", videoSize);
                                                    editorrr.commit();

                                                    fullscreenAdd();
                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (imageSize > imageFile) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TotalFiles tt = new TotalFiles();
                                            Integer imageSize = tt.geImagetFilePaths(getApplicationContext()).size();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("File Addition Alert!!",
                                                            "Intruder Alert detects someone Add Some Image File on your Device",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editorrr = getSharedPreferences(MY_PREFS_NAME_FOR_FILE, MODE_PRIVATE).edit();
                                                    editorrr.putInt("IMAGE", imageSize);
                                                    editorrr.commit();
                                                    fullscreenAdd();

                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                }


                                // sim tracking
                                final List<String> carrierNames = new ArrayList<>();
                                try {
                                    final String permission = Manifest.permission.READ_PHONE_STATE;
                                    if ((Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) && (ContextCompat.checkSelfPermission(getApplicationContext(), permission) == PackageManager.PERMISSION_GRANTED)) {
                                        final List<SubscriptionInfo> subscriptionInfos = SubscriptionManager.from(getApplicationContext()).getActiveSubscriptionInfoList();
                                        for (int i = 0; i < subscriptionInfos.size(); i++) {
                                            carrierNames.add(subscriptionInfos.get(i).getCarrierName().toString());
                                        }

                                    } else {
                                        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
                                        // Get carrier name (Network Operator Name)
                                        carrierNames.add(telephonyManager.getNetworkOperatorName());

                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                                TelephonyInfo telephonyInfo = TelephonyInfo.getInstance(getApplicationContext());
                                String imeiSIM1 = telephonyInfo.getImsiSIM1();
                                final String imeiSIM2 = telephonyInfo.getImsiSIM2();

                                SharedPreferences prefs = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE);
                                String SIM1 = prefs.getString("SIM1", "");
                                String SIM2 = prefs.getString("SIM2", "");

                                if (!SIM1.equals(imeiSIM1) && !SIM2.equals(imeiSIM2)) {

                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TelephonyInfo telephonyInfo = TelephonyInfo.getInstance(getApplicationContext());
                                            String imeiSIM1 = telephonyInfo.getImsiSIM1();
                                            String imeiSIM2 = telephonyInfo.getImsiSIM2();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("Sim Card Changed",
                                                            "Someone Change Your Sim Card by " + carrierNames.get(0) + " and " + carrierNames.get(1) + " Sim Card",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                                                    editor.putString("SIM1", imeiSIM1.toString());
                                                    editor.putString("SIM2", imeiSIM2.toString());
                                                    editor.commit();
                                                    fullscreenAdd();
                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (!SIM1.equals(imeiSIM1)) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TelephonyInfo telephonyInfo = TelephonyInfo.getInstance(getApplicationContext());
                                            String imeiSIM1 = telephonyInfo.getImsiSIM1();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("Sim Card Changed",
                                                            "Someone Change Your Sim Card by " + carrierNames.get(0) + " Sim Card",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                                                    editor.putString("SIM1", imeiSIM1.toString());
                                                    editor.commit();
                                                    fullscreenAdd();
                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }

                                } else if (!SIM2.equals(imeiSIM2)) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TelephonyInfo telephonyInfo = TelephonyInfo.getInstance(getApplicationContext());
                                            String imeiSIM2 = telephonyInfo.getImsiSIM2();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("Sim Card Changed",
                                                            "Someone Change Your Sim Card by " + carrierNames.get(1) + " Sim Card",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                                                    editor.putString("SIM2", imeiSIM2.toString());
                                                    editor.commit();
                                                    fullscreenAdd();
                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }
                                } else if (imeiSIM1.equals("")) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TelephonyInfo telephonyInfo = TelephonyInfo.getInstance(getApplicationContext());
                                            String imeiSIM1 = telephonyInfo.getImsiSIM1();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("Sim Card Removed",
                                                            "Someone Removed Your Sim Card from SIM Slot 1",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                                                    editor.putString("SIM1", "");
                                                    editor.commit();
                                                    fullscreenAdd();
                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }

                                } else if (imeiSIM2.equals("")) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TelephonyInfo telephonyInfo = TelephonyInfo.getInstance(getApplicationContext());
                                            String imeiSIM1 = telephonyInfo.getImsiSIM1();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("Sim Card Removed",
                                                            "Someone Removed Your Sim Card from SIM Slot 2",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                                                    editor.putString("SIM2", "");
                                                    editor.commit();
                                                    fullscreenAdd();
                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }

                                } else if (SIM1.equals("") && !imeiSIM1.equals("")) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TelephonyInfo telephonyInfo = TelephonyInfo.getInstance(getApplicationContext());
                                            String imeiSIM1 = telephonyInfo.getImsiSIM1();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("Sim Card Added",
                                                            "Someone Add a Sim Card on SIM Slot 1",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                                                    editor.putString("SIM1", imeiSIM1.toString());
                                                    editor.commit();
                                                    fullscreenAdd();
                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }

                                } else if (SIM2.equals("") && !imeiSIM2.equals("")) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            TelephonyInfo telephonyInfo = TelephonyInfo.getInstance(getApplicationContext());
                                            String imeiSIM1 = telephonyInfo.getImsiSIM1();
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("Sim Card Added",
                                                            "Someone Add a Sim Card on SIM Slot 2",
                                                            toMailAddress,
                                                            email);

                                                    SharedPreferences.Editor editor = getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit();
                                                    editor.putString("SIM2", imeiSIM2.toString());
                                                    editor.commit();
                                                    fullscreenAdd();
                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();
                                    }

                                }


                                //instal/uninstall tracking
                                for (ApplicationInfo packageInfo : apps) {
                                    data1.append(pm.getApplicationLabel(packageInfo).toString() + "\n");
                                }
                                File myDirectory = new File(Environment.getExternalStorageDirectory(), "System Tracker");
                                File file = new File(myDirectory, "appsList.txt");

                                try {
                                    BufferedReader br = new BufferedReader(new FileReader(file));
                                    String line;
                                    while ((line = br.readLine()) != null) {
                                        data2.append(line);
                                        data2.append("\n");
                                    }
                                    br.close();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }

                                if (data2.toString().equals(data1.toString())) {
                                    //Toast.makeText(getApplicationContext(), "No Apps Install or uninstall", Toast.LENGTH_LONG).show();
                                } else if (data2.length() > data1.length()) {

                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {

                                            File myDirectory = new File(Environment.getExternalStorageDirectory(), "System Tracker");
                                            File file = new File(myDirectory, "appsList.txt");
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("Uninstall Application",
                                                            "Someone Uninstall Apps on your Device",
                                                            toMailAddress,
                                                            email);
                                                    writer = new FileWriter(file);
                                                    writer.append(data1);
                                                    writer.flush();
                                                    writer.close();
                                                    fullscreenAdd();
                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }

                                            }

                                        }).start();

                                    }
                                } else if (data2.length() < data1.length()) {
                                    if (DetectConnection.checkInternetConnection(getApplicationContext()) && !email.equals("")) {
                                        new Thread(new Runnable() {
                                            File myDirectory = new File(Environment.getExternalStorageDirectory(), "System Tracker");
                                            File file = new File(myDirectory, "appsList.txt");
                                            SharedPreferences emailAddress = getSharedPreferences("Email", MODE_PRIVATE);
                                            String email = emailAddress.getString("email", null);

                                            public void run() {
                                                try {
                                                    sender.sendMail("Install Application",
                                                            "Someone Install Apps on your Device",
                                                            toMailAddress,
                                                            email);

                                                    writer = new FileWriter(file);
                                                    writer.append(data1);
                                                    writer.flush();
                                                    writer.close();

                                                    fullscreenAdd();
                                                } catch (Exception e) {
                                                    Log.e("SendMail", e.getMessage(), e);
                                                }
                                            }

                                        }).start();
                                    }
                                }
                            }
                        });
                    } catch (Exception e) {
                        // TODO: handle exception
                    }
                }
            }
        }).start();


        return Service.START_STICKY;

    }

    @Override
    public void onDestroy() {

        super.onDestroy();
        sensorMan.unregisterListener(this);;
        Toast.makeText(this, "Service Destroy", Toast.LENGTH_LONG).show();
        if(mMediaPlayer!=null){
            mMediaPlayer.stop();
            mMediaPlayer.release();
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    public void fullscreenAdd() {
        mInterstitialAd = new InterstitialAd(getApplicationContext());
        mInterstitialAd.setAdUnitId(getString(R.string.interstitial_full_screen));
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                .addTestDevice("C1815A059B68383C673E2A6FF92844CF")
                .build();
        mInterstitialAd.loadAd(adRequest);
        mInterstitialAd.setAdListener(new AdListener() {
            public void onAdLoaded() {
                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                }
            }
        });
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            mGravity = event.values.clone();
            // Shake detection
            float x = mGravity[0];
            float y = mGravity[1];
            float z = mGravity[2];
            mAccelLast = mAccelCurrent;
            mAccelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
            float delta = mAccelCurrent - mAccelLast;
            mAccel = mAccel * 0.9f + delta;
            Log.d("sensor", "value: " + String.valueOf(mAccel));

            // Make this higher or lower according to how much
            // motion you want to detect
            if (mAccel > 1.5) {
//                SharedPreferences pref = getSharedPreferences("alertPref", MODE_PRIVATE);
//                boolean alertFlag = pref.getBoolean("alertFlag", false);
//                if (alertFlag == true)
                    mMediaPlayer.start();
            }
        }
    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
        sensorMan.registerListener(this, accelerometer,
                SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


}