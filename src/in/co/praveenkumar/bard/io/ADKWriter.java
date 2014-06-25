package in.co.praveenkumar.bard.io;

import java.io.FileOutputStream;
import java.io.IOException;

import android.util.Log;

public class ADKWriter {
	final String DEBUG_TAG = "BARD ADK Writer";
	private FileOutputStream mFout = null;

	public ADKWriter(FileOutputStream mFout) {
		this.mFout = mFout;
	}

	public void write(final String data) {

		// Start a new thread to write
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					Log.d(DEBUG_TAG, "Writing length " + data.length());
					mFout.write(new byte[] { (byte) data.length() });
					Log.d(DEBUG_TAG, "Writing data: " + data);
					mFout.write(data.getBytes());
					Log.d(DEBUG_TAG, "Done writing");
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		}).start();

	}

}
