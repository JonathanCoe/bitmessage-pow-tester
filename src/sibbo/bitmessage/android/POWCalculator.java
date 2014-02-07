package sibbo.bitmessage.android;

import java.math.BigInteger;

import android.util.Log;

/**
 * Does the POW calculation, uses multiple threads.
 * 
 * @author Sebastian Schmidt, modified by Jonathan Coe
 * @version 1.0
 */
public class POWCalculator implements POWListener 
{
	private static final String TAG = "POW_CALCULATOR";

	/** The amount of threads to use per CPU. */
	private static final int THREADS_PER_CPU = 1;

	/** The target collision quality. */
	private long target;

	/** The hash of the message. */
	private byte[] initialHash;

	/** The target system load created by the calculation. (Per CPU) */
	private float targetLoad;

	/** The worker that found a valid nonce. */
	private POWWorker finishedWorker;
	
	private boolean POWSuccessful;
	
	private static int averageProofOfWorkNonceTrialsPerByte = 320;
	
	private static int payloadLengthExtraBytes = 14000;
	
	int hashesCalculated = 0;

	/**
	 * Creates a new POWCalculator.
	 * 
	 * @param target
	 *            The target collision quality.
	 * @param initialHash
	 *            The hash of the message.
	 * @param targetLoad
	 *            The target system load created by the calculation. (Per CPU)
	 */
	public POWCalculator()
	{
		
	}
	
	public void setTarget(long newTarget)
	{
		target = newTarget;
	}
	
	public void setInitialHash(byte[] newInitialHash)
	{
		initialHash = newInitialHash;
	}
	
	public void setTargetLoad(long newTargetLoad)
	{
		targetLoad = newTargetLoad;
	}

	/**
	 * Calculate the POW.<br />
	 * <b>WARNING: This can take a long time.</b>
	 * 
	 * @return A byte[] containing a nonce that fulfills the collision quality condition.
	 */
	public synchronized byte[] execute(int maxTime) 
	{
		POWWorker[] workers = new POWWorker[Runtime.getRuntime().availableProcessors() * THREADS_PER_CPU];

		for (int i = 0; i < workers.length; i++) 
		{
			workers[i] = new POWWorker(target, i, workers.length, initialHash, this, targetLoad / THREADS_PER_CPU, maxTime);
			new Thread(workers[i], "POW Worker No. " + i).start();
		}

		try 
		{
			wait();
		}
		catch (InterruptedException e) 
		{
			Log.i(TAG, "Waiting interrupted!");
		}

		for (POWWorker w : workers) 
		{
			w.stop();
			
			hashesCalculated = hashesCalculated + w.getHashesCalculated();
		}
		
		if (finishedWorker.getSuccessResult() == true)
		{
			POWSuccessful = true;
		}

		return Util.getBytes(finishedWorker.getNonce());
	}

	@Override
	public synchronized void powFinished(POWWorker powWorker) 
	{
		if (finishedWorker == null) 
		{
			finishedWorker = powWorker;
		}

		notifyAll();
	}

	/**
	 * Returns the POW target for a message with the given length.
	 * 
	 * @param length
	 *            The message length.
	 * @return The POW target for a message with the given length.
	 */
	public static long getPOWTarget(int length) 
	{
		BigInteger powTarget = BigInteger.valueOf(2);
		powTarget = powTarget.pow(64);
		Log.i(TAG, "Using a payloadLengthExtraBytes value of " + payloadLengthExtraBytes);
		Log.i(TAG, "Using an averageProofOfWorkNonceTrialsPerByte value of " + averageProofOfWorkNonceTrialsPerByte);
		powTarget = powTarget.divide(BigInteger.valueOf((length + payloadLengthExtraBytes + 8) * averageProofOfWorkNonceTrialsPerByte));
		
		// Note that we are dividing through at least 8, so that the value is
		// smaller than 2^61 and fits perfectly into a long.
		return powTarget.longValue();
	}
	
	public boolean getPOWSuccessfulResult()
	{
		return POWSuccessful;
	}
	
	public void setDifficulty(int difficultyFactor)
	{
		averageProofOfWorkNonceTrialsPerByte = 320 * difficultyFactor;
		payloadLengthExtraBytes = 14000 * difficultyFactor;
	}
	
	public int getHashesCalculated()
	{
		return hashesCalculated;
	}
}