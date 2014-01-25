package com.whatismydb.mydb;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

public class MyDbMain extends Activity {

	// Pointers to UI elements
	Switch sw_rec;
	Switch sw_min;
	Switch sw_hr;
	Switch sw_day;
	Switch sw_db;
	TextView tv_dB;
	
	// Constants for AudioRecord
	private static final int SAMPLE_RATE = 8000;
	private static final int NUM_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
	private static final int ENCODING_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
	
	// Pointer to AudioRecord object
	private AudioRecord recorder = null;
	
	// Record state
	private boolean recording;
	
	// Update rate
	private int updateRate;
	private static final int UPDATE_MINUTE = 1;
	private static final int UPDATE_HOUR = 60;
	private static final int UPDATE_DAY = 1440;
	
	// Create an empty scheduler
    ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    
    // URL for posting data
    String postUrl = "http://192.168.15.228:8080/whatismydb/poster/";
	
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
        tv_dB = (TextView) findViewById(R.id.tv_decibels);
        
        // Disable the updatedb switch for now
        sw_db.setEnabled(false);
        
    }

    // Set the buffer size and number of bytes per element
    int bufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, NUM_CHANNELS, ENCODING_FORMAT);
    int bytesPerElement = 2;
    
    // ON-CLICK LISTENER METHODS
    
    // Record switch
    public void onClickRec(View v) {
    	
    	// Get the state of the switch
        boolean on = ((Switch) v).isChecked();
        
        // If switch is on
        if (on) {
        	
        	// Create AudioRecord object
        	recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
        			SAMPLE_RATE, NUM_CHANNELS, ENCODING_FORMAT,
        			bufSize*bytesPerElement);
        	
        	// Start RecordAudioTask
        	new RecordAudioTask().execute();
        	
        } else { // If switch is off
        	
        	// Set recording boolean to false
        	recording = false;
        	
        	// Stop recording
        	recorder.stop();
        	
        	// Free memory, if necessary
        	recorder.release();
        	recorder = null;
        	
        	// Set other switches to false
        	
        }
    	
    }
    
    // Update switch
    public void onClickUpdate(View v) {
    	
    	// Get the state of the switch
        boolean on = ((Switch) v).isChecked();
        
        // If switch is on
        if (on) {
        	
        	// Figure out which switch was changed
        	switch(v.getId()){
        		
        	case R.id.sw_min:
        		updateRate = UPDATE_MINUTE;
        		break;
        	case R.id.sw_hr:
        		updateRate = UPDATE_HOUR;
        		break;
        	case R.id.sw_day:
        		updateRate = UPDATE_DAY;
        		break;
        	default:
        		break;
        	
        	}
        	
        	// Enable the update database switch
        	if (sw_db.isEnabled() == false) {
        		sw_db.setEnabled(true);
        	}
        	
        } else { // If switch is off

        	// Uncheck and disable the update database switch
        	sw_db.setChecked(false);
        	sw_db.setEnabled(false);
        	
        }
        
    }
    
    // Update switch
    public void onClickUpdateDb(View v) {
    	
    	// Get the state of the switch
        boolean on = ((Switch) v).isChecked();
        
        // If on
        if (on) {
        	
        	// Schedule a regularly occurring task at the currently specified update_rate
        	scheduler.scheduleAtFixedRate (new Runnable() {
        		public void run() {
        			Long tsLong = System.currentTimeMillis()/1000;
        			String ts = tsLong.toString();
        			String value = (String) tv_dB.getText();
        			String theLog = ts + ": " + value;
        			Log.e("scheduler: ", theLog);
        			
        			// Make a JSON objects w/ the data
        			JSONObject json = dataToJson(ts, value);
        			
        			// POST to the database
        			postToDb(json);
        			
        		}
        	}, 0, updateRate, TimeUnit.MINUTES);
        	
        } else {
        	
        	// Shut down all the scheduled tasks
        	scheduler.shutdownNow();
        	
        }
        
    }
    
    // HELPER FUNCTIONS

    // Gets the RMS
    public double calculateRMS(short[] sndChunk) {
		
    	double rms = 0;
    	double sum = 0;
    	
    	// Sum the values in the buffer
    	for(int i = 0; i < sndChunk.length; i++) {
    		sum += Math.pow(sndChunk[i], 2);
    	}
    	
    	// Get the mean and take the square root to get rms
    	rms = (double) Math.sqrt(sum/sndChunk.length);
    	
    	return rms;
    	
    }
    
    // Gets dB from RMS
    public double calculateDb(double rms) {
		
    	double db = 0;
    	double ref = 32767.0; // reference value used for dB calculation

    	// dB calculation
    	db = 20 * Math.log10(rms/ref);
    	
    	return db;
    	
    }
    
    // Create a JSON Object from the supplied data
    public JSONObject dataToJson(String timestamp, String value) {
		
    	// Make a new JSON objects
    	JSONObject obj = new JSONObject();
    	
    	// Put the data in it
    	try {
    		obj.put("timestamp", timestamp);
    		obj.put("value", value);
    	} catch (JSONException e) {
    		e.printStackTrace();
    	}
    	
    	return obj;
    }
    
    // Posts data to the database
    public void postToDb(JSONObject jsonObject) {
    	
    	try {
    		
    		// Create a default HTTP client
    		HttpClient client = new DefaultHttpClient();
    		
    		// Create HTTP post object
    		HttpPost poster = new HttpPost(postUrl);
    		
    		// Get a string from the JSON Object
    		String jsonString = jsonObject.toString();
    		Log.e("json string: ", jsonString);
    		
    		// Set the HTTP entity
    		StringEntity entity = new StringEntity(jsonString);
    		poster.setEntity(entity);
    		
    		// Set the header
    		poster.setHeader("Accept", "application/json");
            poster.setHeader("Content-type","application/json");
    		
    		// Execute the post
    		HttpResponse response = client.execute((HttpUriRequest)poster);
    		
    		// Get entity from the response
    		HttpEntity entityHttp = response.getEntity();
    		
    		// Check on the entity (should be JSON string data)
    		if (entity != null) {
                Log.e("result: ", EntityUtils.toString(entityHttp));
            }
    		
    	} catch(Exception e) {
    		Log.e("post error: ", "Unable to post to database");
    		e.printStackTrace();
    	}
    	
	}
    
    // CLASSES
    private class RecordAudioTask extends AsyncTask<Void, Double, Void> {

		@Override
		protected Void doInBackground(Void... arg0) {
			
			// Start recording + start timer
        	recorder.startRecording();
        	
        	// Get the recording state from the recorder
        	recording = true;
        	
        	// Grab chunk of audio when timer reaches selected interval(s)
        	short sndChunk[] = new short[bufSize];
        	double rms = 0;
        	double db = 0;
        	
        	while(recording) {
        		
        		// Read a chunk of audio data
        		recorder.read(sndChunk, 0, bufSize);
        		
        		// Calculate the RMS of the audio chunk
        		rms = calculateRMS(sndChunk);
        		
        		// Calculate dB
        		db = calculateDb(rms);
        		
        		// Update the UI with the rms value
        		publishProgress(db);
        		
        	}
			
			return null;
		}
    	
		protected void onProgressUpdate(Double... db) {
			
			// Get the dB value and set it to the text field
			double db_val = db[0].doubleValue();
			String output = String.format("%.2f dB", db_val);
			tv_dB.setText(output);
			
	     }
		
		protected void onPostExecute() {
			
		}
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_db_main, menu);
        return true;
    }
    
}
