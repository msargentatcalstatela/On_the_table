package com.myesis.onthetable;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ToggleButton;

import com.myesis.classifierandsensorservice.Constants;
import com.myesis.classifierandsensorservice.DataSet;
import com.myesis.classifierandsensorservice.LogisticPredictor;
import com.myesis.classifierandsensorservice.Predictor;
import com.myesis.classifierandsensorservice.SensorService;


public class MainActivity extends Activity {
    private boolean mBound;
    private SensorService mBoundService;
    BroadcastReceiver receiver;
    Predictor predictor;
    FrameLayout table;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Bind service if already running
        if (SensorService.isStarted) {
            bindService(new Intent(MainActivity.this,
                    SensorService.class), mConnection, BIND_AUTO_CREATE);
            mBound = true;

        }

        IntentFilter mStatusIntentFilter = new IntentFilter(
                Constants.BROADCAST_SENSOR_DATA);
        receiver = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, mStatusIntentFilter);

        ToggleButton start = (ToggleButton) findViewById(R.id.monitor);

        if (SensorService.isStarted) start.setChecked(true);

        start.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {

                    Intent intent = new Intent(buttonView.getContext(),
                            SensorService.class);

                    startService(intent);
                    mBound = bindService(new Intent(MainActivity.this,
                            SensorService.class), mConnection, BIND_AUTO_CREATE);
                    mBoundService.setIsStarted(true);
                    mBound = true;

                } else {
                    stopService(new Intent(MainActivity.this,
                            SensorService.class));

                    if (mBound) {
                        mBoundService.setIsStarted(false);
                        mBoundService.disableSensor();
                        unbindService(mConnection);
                        stopService(new Intent(MainActivity.this,
                                SensorService.class));

                        mBound = false;
                        table.setBackgroundColor(getResources().getColor(R.color.darkgreen));
                    }
                }
            }
        });

        predictor = new LogisticPredictor(this, "test1.txt");


        table = (FrameLayout)findViewById(R.id.tframe);

    }
    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mBoundService = ((SensorService.LocalBinder) service).getService();

            mBoundService.setHostingActivityRunning(true);
        }

        public void onServiceDisconnected(ComponentName className) {
            mBoundService.setHostingActivityRunning(false);
            mBoundService = null;
        }
    };
    // Broadcast receiver for receiving status updates from the IntentService
    private class ResponseReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            DataSet data = (DataSet) intent.getSerializableExtra(Constants.DATA);

            boolean inCategory = predictor.predict(data);

            if (inCategory){
                table.setBackgroundColor(getResources().getColor(R.color.green));
            }else{
                table.setBackgroundColor(getResources().getColor(R.color.darkgreen));
            }
        }
    }
}
