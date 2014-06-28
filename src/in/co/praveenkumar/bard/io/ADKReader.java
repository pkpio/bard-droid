package in.co.praveenkumar.bard.io;

import in.co.praveenkumar.bard.activities.MainActivity.UIUpdater;

import java.io.FileInputStream;
import java.io.IOException;

import android.os.AsyncTask;
import android.util.Log;

public class ADKReader {
	final String DEBUG_TAG = "BARD.IO.ADKReader";
	FileInputStream mFin = null;
	UIUpdater uu = null;

	public ADKReader(FileInputStream mFin, UIUpdater uu) {
		this.mFin = mFin;
		this.uu = uu;
	}

	public void start() {
		new dataListener().execute(0);
	}

	public void setFinputstream(FileInputStream mFin) {
		this.mFin = mFin;
	}

	private class dataListener extends AsyncTask<Integer, Integer, Long> {
		byte[] buffer = new byte[16384];
		String read = "";

		@Override
		protected void onProgressUpdate(Integer... progress) {
			uu.setRead(read);
		}

		@Override
		protected Long doInBackground(Integer... params) {
			int ret = 0;
			int i;

			Log.d(DEBUG_TAG, "ADKReader doInbackground called");

			while (true) { // read data
				try {
					ret = mFin.read(buffer);
				} catch (IOException e) {
					Log.d(DEBUG_TAG, "Caught a Reader exception");
					e.printStackTrace();
					break;
				}

				i = 0;
				read = "";
				while (i < ret) {
					int len = ret - i;
					if (len >= 1) {
						//int value = (int) buffer[i];
						read = read + buffer[i] + "\t";
					}
					i += 1; // number of bytes sent

					if (i == ret) {
						Log.d(DEBUG_TAG, "Bytes received:" + i);
						publishProgress(len);
					}
				}
			}

			return null;
		}

		@Override
		protected void onPostExecute(Long result) {
			// We reached here means our read loop exited.
			// Most common reason is BAD File Descriptor.
			// So, open accessory again with updated FD.
			Log.d(DEBUG_TAG, "ADKReader Post execute called");
			uu.reInitAccessory();
		}

	}

}
