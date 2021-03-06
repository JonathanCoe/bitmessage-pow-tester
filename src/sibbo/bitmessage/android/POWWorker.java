package sibbo.bitmessage.android;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import android.util.Log;

/**
 * A worker class to parallelize POW calculation.
 * 
 * @author Sebastian Schmidt, modified by Jonathan Coe
 * @version 1.0
 */
public class POWWorker implements Runnable 
{
	private static final String TAG = "POW_WORKER";
	private static final Logger LOG = Logger.getLogger(POWWorker.class.getName());

	/**
	 * The time period in milliseconds to check if the pow calculation should be
	 * aborted.
	 */
	private static final int ROUND_TIME = 100;

	/** The collision quality that should be achieved. */
	private long target;

	/** The POW nonce. */
	private volatile long nonce;

	/** The initial hash value. */
	private byte[] initialHash;

	/** True if the calculation is running. */
	private volatile boolean running;

	/** A stop request can be made by setting this to true. */
	private volatile boolean stop;

	/** The listener to inform if we found the result. */
	private POWListener listener;

	/** The system load that should be created by this worker. */
	private float targetLoad;

	/** The increment that should be used for finding the next nonce. */
	private long increment;
	
	/** The maximum time allowed for the worker to do its calculations. */
	private long maxTime; 
	
	/** The time at which the worker starts working. */
	private long startTime;
	
	/** Indicates whether or not a valid nonce has been found. */
	private boolean POWSuccessful;
	
	/** The number of hashes calculated so far. */
	private int hashesCalculated = 0;
	
	private MessageDigest sha512;
	

	/**
	 * Creates a new POWWorker.
	 * 
	 * @param target
	 *            The target collision quality.
	 * @param startNonce
	 *            The nonce to start with.
	 * @param increment
	 *            The step size. A POW worker calculates with: startNonce,
	 *            startNonce + increment, startNonce + 2 * increment...
	 * @param initialHash
	 *            The hash of the message.
	 * @param listener
	 *            The listener to inform if a result was found.
	 * @param targetLoad
	 *            The system load that should be created by this worker.
	 */
	public POWWorker(long target, long startNonce, long increment, byte[] initialHash, POWListener listener,
			float targetLoad, long maxTime) 
	{
		if (listener == null) 
		{
			throw new NullPointerException("listener must not be null.");
		}

		this.target = target;
		this.nonce = startNonce;
		this.initialHash = initialHash;
		this.listener = listener;
		this.targetLoad = targetLoad;
		this.increment = increment;
		this.maxTime = maxTime;
		
		try 
		{
			sha512 = MessageDigest.getInstance("SHA-512");
		} 
		catch (NoSuchAlgorithmException e) 
		{
			throw new RuntimeException("SHA-512 not supported!", e);
		}
	}

	/**
	 * Returns true if the worker is actually calculating the POW.
	 * 
	 * @return True if the worker is actually calculating the POW.
	 */
	public boolean isRunning() 
	{
		return running;
	}

	/**
	 * Request the worker to stop.
	 */
	public void stop() 
	{
		stop = true;
	}

	/**
	 * Returns the current nonce. Note that it can be wrong if isRunning()
	 * returns true or no success was reported.
	 * 
	 * @return The current nonce.
	 */
	public long getNonce()
	{
		return nonce;
	}
	
	public boolean getSuccessResult()
	{
		return POWSuccessful;
	}
	
	public int getHashesCalculated()
	{
		return hashesCalculated;
	}

	/**
	 * Calculates the POW.
	 */
	@Override
	public void run() 
	{
		running = true;
		
		startTime = System.currentTimeMillis();

		int iterations = 100 * ROUND_TIME;
		long sleepTime = (long) (ROUND_TIME * (1 - targetLoad));
		long result = Long.MAX_VALUE;
		long nonce = this.nonce;

		float topLoad = targetLoad * 1.1f;
		float bottomLoad = targetLoad * 0.9f;
		
		while (!stop) 
		{
			long ls = System.nanoTime();
			byte[] hash;

			for (int i = 0; i < iterations; i++) 
			{
				sha512.reset();
				sha512.update(Util.getBytes(nonce));
				hash = sha512.digest(initialHash);
				sha512.reset();
				hash = sha512.digest(hash);
				
				hashesCalculated = hashesCalculated + 2;
								
				result = Util.getLong(hash);

				if (result <= target && result >= 0) 
				{
					Log.i(TAG, "Found a valid nonce!");
					stop();
					this.nonce = nonce;
					POWSuccessful = true;
					listener.powFinished(this);
					break;
				}
				
				if (startTime + (maxTime * 1000) < System.currentTimeMillis())
				{
					Log.i(TAG, "Failed to find a valid nonce in the time allowed");
					stop();
					this.nonce = nonce;
					listener.powFinished(this);
					break;
				}

				nonce += increment;
			}

			long lh = System.nanoTime();

			if (sleepTime > 0) 
			{
				try 
				{
					Thread.sleep(sleepTime);
				} 
				catch (InterruptedException e) 
				{
					LOG.log(Level.SEVERE, "Sleeping interrupted.", e);
					System.exit(1);
				}
			}

			long lf = System.nanoTime();

			float load = ((float) (lh - ls) / (float) (lf - ls));
			//System.out.println("Load: " + load);

			if (load > topLoad) 
			{
				iterations -= iterations >> 8;
				System.out.println("iterations: " + iterations);
			} 
			else if (load < bottomLoad) 
			{
				iterations += iterations >> 8;
				System.out.println("iterations: " + iterations);
			}
		}

		running = false;
	}
}