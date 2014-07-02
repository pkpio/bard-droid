package in.co.praveenkumar.bard.io;

import in.co.praveenkumar.bard.activities.MainActivity.UIUpdater;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.future.usb.UsbAccessory;

public class ADKReader {
	final String DEBUG_TAG = "BARD.IO.ADKReader";
	FileInputStream mFin = null;
	UIUpdater uu = null;
	Context context = null;
	UsbAccessory mAccessory = null;
	FileDescriptor fd = null;

	public ADKReader(FileInputStream mFin, UIUpdater uu, Context context,
			UsbAccessory mAccessory) {
		this.mFin = mFin;
		this.uu = uu;
		this.context = context;
		this.mAccessory = mAccessory;
	}

	public void start() {
		new dataListener().execute(0);
	}

	public void setFinputstream(FileInputStream mFin) {
		this.mFin = mFin;
	}

	private class dataListener extends AsyncTask<Integer, Integer, Long> {
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
				byte[] buffer = new byte[16384];
				try {
					// fd = UsbManager.getInstance(context)
					// .openAccessory(mAccessory).getFileDescriptor();
					// mFin = new FileInputStream(fd);
					Log.d(DEBUG_TAG, "Trying to buffer read");
					ret = mFin.read(buffer);
					Log.d(DEBUG_TAG, "Buffer read");
				} catch (IOException e) {
					Log.d(DEBUG_TAG, "Caught a Reader exception");
					e.printStackTrace();
					break;
				} catch (Exception e) {
					Log.d(DEBUG_TAG,
							"Unknow exception while getting inputstream");
					e.printStackTrace();
				}
				read = buffer.toString();
				publishProgress(0);

				// i = 0;
				// read = "";
				// while (i < ret) {
				// int len = ret - i;
				// if (len >= 1) {
				// //int value = (int) buffer[i];
				// read = read + (int) buffer[i] + "   ";
				// }
				// i += 1; // number of bytes sent
				//
				// if (i == ret) {
				// Log.d(DEBUG_TAG, "Bytes received:" + i);
				// publishProgress(len);
				// // try {
				// // mFin.close();
				// // } catch (IOException e) {
				// // Log.d(DEBUG_TAG, "Inputstream close failed");
				// // e.printStackTrace();
				// // } catch (Exception e) {
				// // Log.d(DEBUG_TAG,
				// // "Unknow exception while closing inputstream");
				// // e.printStackTrace();
				// // }
				// }
				// }

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
