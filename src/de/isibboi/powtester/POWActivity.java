package de.isibboi.powtester;

import java.text.DecimalFormat;
import java.util.Random;

import sibbo.bitmessage.android.POWCalculator;
import sibbo.bitmessage.android.Util;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

/**
 * The activity class for the Proof of Work Tester app
 * 
 * @author Sebastian Schmidt, modified by Jonathan Coe
 */
public class POWActivity extends Activity 
{
	private static final String TAG = "POW_ACTIVITY";
	
	private TextView maxTimeAllowedTextView;
	private SeekBar maxTimeAllowedSeekBar;
	private TextView payloadLengthTextView;
	private SeekBar payloadLengthSeekBar;
	private TextView difficultyTextView;
	private SeekBar difficultySeekBar;
	private Button doPOWButton;
	private TextView resultTextView;
	
	private boolean powTestSuccessful;
	private boolean powTestRunning;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_pow);
		
		payloadLengthSeekBar = (SeekBar) findViewById(R.id.payloadLengthSeekBar);
		payloadLengthTextView = (TextView) findViewById(R.id.payloadLengthTextView);
		resultTextView = (TextView) findViewById(R.id.resultTextView);
		payloadLengthSeekBar.setMax(10000);
		payloadLengthTextView.setText("Payload length: " + payloadLengthSeekBar.getProgress() + " bytes");	
		payloadLengthSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() 
		{
			@Override
			public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) 
			{
				payloadLengthTextView.setText("Payload length: " + payloadLengthSeekBar.getProgress() + " bytes");
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) 
			{
				// Ignore
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) 
			{
				// Ignore
			}
		});
		
		maxTimeAllowedSeekBar = (SeekBar) findViewById(R.id.maxTimeAllowedSeekBar);
		maxTimeAllowedTextView = (TextView) findViewById(R.id.maxTimeAllowedTextView);
		maxTimeAllowedSeekBar.setMax(600);
		maxTimeAllowedTextView.setText("Max time allowed: " + payloadLengthSeekBar.getProgress() + " seconds");
		maxTimeAllowedSeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() 
		{
			@Override
			public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) 
			{
				maxTimeAllowedTextView.setText("Max time allowed: " + maxTimeAllowedSeekBar.getProgress() + " seconds");
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) 
			{
				// Ignore
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) 
			{
				// Ignore
			}
		});
		
		difficultySeekBar = (SeekBar) findViewById(R.id.difficultySeekBar);
		difficultyTextView = (TextView) findViewById(R.id.difficultyTextView);
		difficultySeekBar.setMax(9);
		difficultyTextView.setText("Difficulty factor: " + (difficultySeekBar.getProgress() + 1));
		difficultySeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() 
		{
			@Override
			public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) 
			{
				// Because SeekBar cannot have a minimum value other than 0 for some reason, we have to add
				// 1 to the value we get from the user
				difficultyTextView.setText("Difficulty factor: " + (difficultySeekBar.getProgress() + 1));
			}

			@Override
			public void onStartTrackingTouch(SeekBar arg0) 
			{
				// Ignore
			}

			@Override
			public void onStopTrackingTouch(SeekBar arg0) 
			{
				// Ignore
			}
		});

		doPOWButton = (Button) findViewById(R.id.doPOWButton);
		doPOWButton.setOnClickListener(new OnClickListener() 
		{
			DoPOWTask powTask = null;
			
			@Override
			public void onClick(View view) 
			{			
				if (powTestRunning == false)
				{	
					// Run the test and then set to 'running' state
					resultTextView.setTextColor(Color.BLACK);
					resultTextView.setText("Running Proof of Work test...");
					
					powTestSuccessful = false; // Reset this value to avoid false positives
					
					powTask = new DoPOWTask();
					powTask.execute();
					
					powTestRunning = true;
					
					doPOWButton.setText("Cancel Proof of Work Test");
				}
				
				else
				{
					// Cancel and then set to 'ready to run' state
					powTask.cancel(true);
					
					resultTextView.setTextColor(Color.BLACK);
					resultTextView.setText("Proof of Work test cancelled");
					doPOWButton.setText("Run Proof of Work Test");
					
					powTestRunning = false;
				}
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.pow, menu);
		return true;
	}
	
	private class DoPOWTask extends AsyncTask<Void, Void, Object>
	{		
		@Override
		protected Object doInBackground(Void... params) 
		{							
			Log.i(TAG, "DoPOWTask.doInBackground() called");
			
			String result = doPOW();
			
			publishProgress();
			
			return result;	
		}
		
		protected void onPostExecute(Object POWResult)
		{
			Log.i(TAG, "DoPOWTask.onPostExecute() called");
			
			String resultString = (String) POWResult;
			
			resultTextView.setText(resultString);
			
			if (powTestSuccessful == true)
			{
				resultTextView.setTextColor(0xFF009900); // Dark green
			}
			else
			{
				resultTextView.setTextColor(Color.RED);
			}
			
			doPOWButton.setText("Run Proof of Work Test");
			powTestRunning = false;
		}
	}
	
	@SuppressLint("Wakelock")
	private String doPOW()
	{
		Random r = new Random();
		byte[] hash = new byte[64];
		r.nextBytes(hash);
		
		POWCalculator pow = new POWCalculator();
				
		Log.i(TAG, "Using a difficulty factor of " + (difficultySeekBar.getProgress() + 1));
		pow.setDifficulty((difficultySeekBar.getProgress() + 1)); // Have to add 1 as the minimum difficulty in Bitmessage is 1
		pow.setTarget(POWCalculator.getPOWTarget(payloadLengthSeekBar.getProgress()));
		pow.setInitialHash(hash);
		pow.setTargetLoad(1);

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "POW");
		
		wl.acquire();
		long start = System.currentTimeMillis();
		byte[] result = pow.execute(maxTimeAllowedSeekBar.getProgress()); // Do the POW calculations
		long end = System.currentTimeMillis();
		wl.release();

		StringBuilder ResultString = new StringBuilder();
		ResultString.append("Test Results:\n");
		ResultString.append("Cores: " + Runtime.getRuntime().availableProcessors() + "\n");
		ResultString.append("Time: " + ((end - start) / 1000) + "." + (((end - start) / 1000) / 10) + " seconds\n");
		
		if (pow.getPOWSuccessfulResult() == true)
		{
			String resultNonce = String.valueOf(Util.getLong(result));
			double amount = Double.parseDouble(resultNonce);
			DecimalFormat formatter = new DecimalFormat("###,###,###"); // Format the nonce result with comma separators
			
			ResultString.append("Result: Found a valid nonce! - " + formatter.format(amount));
			powTestSuccessful = true;
		}
		else
		{
			ResultString.append("Result: Failed to find a valid nonce within the time allowed");
		}

		return ResultString.toString();
	}
}
