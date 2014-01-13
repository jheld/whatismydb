package com.whatismydb.mydb;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;

public class MyDbMain extends Activity {

	// Pointers to UI elements
	Switch sw_rec;
	Switch sw_min;
	Switch sw_hr;
	Switch sw_day;
	Switch sw_db;
	ProgressBar pb_dB;
	
	// Constants for AudioRecord
	private static final int SAMPLE_RATE = 8000;
	private static final int NUM_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	
	// Pointer to AudioRecord object
	private AudioRecord recorder = null;
	private Thread recThread = null;
	
	// Record state
	private boolean recording;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_db_main);
        
        // Get the UI elements from the view
        sw_rec = (Switch) findViewById(R.id.sw_rec);
        sw_min = (Switch) findViewById(R.id.sw_min);
        sw_hr = (Switch) findViewById(R.id.sw_hr);
        sw_day = (Switch) findViewById(R.id.sw_day);
        sw_db = (Switch) findViewById(R.id.sw_updateDb);
        pb_dB = (ProgressBar) findViewById(R.id.pb_db);
        
        // Automatically set the "min" switch to "on" for now
        sw_min.setChecked(true);
        
    }

    // Set the buffer size and number of bytes per element
    int bufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, NUM_CHANNELS, ENCODING_FORMAT);
    int bytesPerElement = 2;
    
    // ON-CLICK LISTENER METHODS
    
    // Record switch
    public void onClickRec(View v) {
    	
    	// Get the state of the switch
        boolean on = ((Switch) v).isChecked();
        
        Log.e("onClickRec", "switch state checked");
        
        // If on
        if (on) {
        	
        	Log.e("onClickRec", "switch on");
        	
        	// Create AudioRecord object
        	recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
        			SAMPLE_RATE, NUM_CHANNELS, ENCODING_FORMAT,
        			bufSize*bytesPerElement);
        	
        	Log.e("onClickRec", "recorder created");
        	
        	// Start recording + start timer
        	recorder.startRecording();
        	
        	// Get the recording state from the recorder
        	recording = true;
        	
        	// Grab chunk of audio when timer reaches selected interval(s)
        	short sndChunk[] = new short[bufSize];
        	double rms = 0;
        	
        	while(recording) {
        		recorder.read(sndChunk, 0, bufSize);
        		
        		// Calculate the RMS of the audio chunk
        		rms = calculateRMS(sndChunk);
        		
        		// Log the RMS
        		Log.e("rms:", Double.toString(rms));
        		
        	}
        	
        } else { // If off
        	
        	Log.e("onClickRec", "switch state unchecked");
        	
        	// Stop recording
        	recorder.stop();
        	recording = false;
        	
        	// Stop timer
        	
        	
        	// Free memory, if necessary
        	recorder.release();
        	recorder = null;
        	
        }
    	
    }
    
    // Update switch
    public void onClickUpdate(View v) {
    	
    	// Get the state of the switch
        boolean on = ((Switch) v).isChecked();
    	
        // If on
        if (on) {
        	
        } else {
        	
        }
        
    }
    
    // HELPER FUNCTIONS
    public double calculateRMS(short[] sndChunk) {
		
    	double rms = 0;
    	double sum = 0;
    	
    	for(int i = 0; i < sndChunk.length; i++) {
    		sum += Math.pow(sndChunk[i], 2);
    	}
    	
    	rms = (double) Math.sqrt(sum/sndChunk.length);
    	
    	return rms;
    	
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_db_main, menu);
        return true;
    }
    
}
