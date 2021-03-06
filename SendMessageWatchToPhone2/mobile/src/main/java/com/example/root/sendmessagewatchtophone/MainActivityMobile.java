package com.example.root.sendmessagewatchtophone;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.io.File;
import java.io.FileWriter;
import java.util.Date;


public class MainActivityMobile extends Activity implements MessageApi.MessageListener, GoogleApiClient.ConnectionCallbacks {
    private static final String WEAR_MESSAGE_PATH = "/message";
    private static final String TAG = "sensorValues";
    private GoogleApiClient mApiClient;
    private ArrayAdapter<String> mAdapter;
    private ListView mListView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_activity_mobile);
        mListView = (ListView) findViewById(R.id.list);
        mAdapter = new ArrayAdapter<String>( this, R.layout.list_item );
        mListView.setAdapter( mAdapter );
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initGoogleApiClient();
    }


    private void initGoogleApiClient() {
        mApiClient = new GoogleApiClient.Builder( this )
                .addApi( Wearable.API )
                .addConnectionCallbacks( this )
                .build();
        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();
    }


    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG,"resumed");

        if( mApiClient != null && !( mApiClient.isConnected() || mApiClient.isConnecting() ) )
            mApiClient.connect();
    }
    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    public void onMessageReceived( final MessageEvent messageEvent ) {
        runOnUiThread( new Runnable() {
            @Override
            public void run() {
                Log.i(TAG,"message received");

                if( messageEvent.getPath().equalsIgnoreCase( WEAR_MESSAGE_PATH ) ) {
                    mAdapter.add(new String(messageEvent.getData()));
                    mAdapter.notifyDataSetChanged();
                    saveData(messageEvent);
                }
            }
        });
    }

    private void saveData(MessageEvent messageEvent) {
        Log.i(TAG,"saving data");
        File prepath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String str = new String(messageEvent.getData());
        String filename = "/"+new Date().getTime()+"_excercises.json";
        String path = prepath + filename;

        try {
            File f = new File(prepath, filename);
           FileWriter writer =new FileWriter(f);
            writer.append(str);
            writer.flush();
            writer.close();
            Log.i(TAG, "saved to " + path);
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "not saved " + e);

        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener( mApiClient, this );
    }
    @Override
    protected void onStop() {
        if ( mApiClient != null ) {
            Wearable.MessageApi.removeListener( mApiClient, this );
            if ( mApiClient.isConnected() ) {
                mApiClient.disconnect();
            }
        }
        super.onStop();
    }
    @Override
    protected void onDestroy() {
        if( mApiClient != null )
            mApiClient.unregisterConnectionCallbacks( this );
        super.onDestroy();
    }
    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG,"connection suspended mobile");
    }
}
