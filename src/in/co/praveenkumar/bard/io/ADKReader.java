package in.co.praveenkumar.bard.io;

import java.io.FileInputStream;
import java.io.IOException;

import android.util.Log;

public class ADKReader implements Runnable {
	final String DEBUG_TAG = "BARD ADK Reader";
	FileInputStream mFin = null;

	public ADKReader(FileInputStream mFin) {
		this.mFin = mFin;
	}

	@Override
	public void run() {
		int ret = 0;
		byte[] buffer = new byte[16384];
		int i;

		while (true) { // read data
			Log.d(DEBUG_TAG, "Run is running");
			try {
				ret = mFin.read(buffer);
			} catch (IOException e) {
				break;
			}

			i = 0;
			while (i < ret) {
				int len = ret - i;
				if (len >= 1) {
					int value = (int) buffer[i];
					Log.d(DEBUG_TAG, "Value is: " + value);
				}
				i += 1; // number of bytes sent
				Log.d(DEBUG_TAG, "Bytes received:" + i);
			}
		}

	}

}
