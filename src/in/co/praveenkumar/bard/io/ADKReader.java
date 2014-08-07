package in.co.praveenkumar.bard.io;

import in.co.praveenkumar.bard.activities.MainActivity;
import in.co.praveenkumar.bard.activities.MainActivity.UIUpdater;
import in.co.praveenkumar.bard.graphics.Frame;

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

		@Override
		protected void onProgressUpdate(Integer... progress) {
			uu.updateFrame();
		}

		@Override
		protected Long doInBackground(Integer... params) {

			Log.d(DEBUG_TAG, "ADKReader doInbackground called");

			while (true) { // read data
				byte[] buffer = new byte[4098];
				try {
					if (mFin != null) {
						System.out.println("mFin is not NULL");
						Frame.bytesReceived += mFin.read(buffer);
					} else
						System.out.println("mFin in NULL");

					// Get the index of the page
					int pageIndex = (int) (buffer[0] & 0x0000000ff)
							+ (int) (buffer[1] << 8 & 0x0000ff00);

					System.out.println("Page index: " + pageIndex);

					int framePos = pageIndex * 4096;
					if ((framePos - (buffer.length - 2)) <= Frame.FRAME_LENGTH) {
						Frame.frameBuffer.position(framePos);
						Frame.frameBuffer.put(buffer, 2, buffer.length - 2);
					}

				} catch (IOException e) {
					Log.d(DEBUG_TAG, "Caught a Reader exception");
					e.printStackTrace();
					uu.reInitAccessory();
					mFin = MainActivity.mFin;
					// break;
				} catch (Exception e) {
					Log.d(DEBUG_TAG,
							"Unknow exception while getting inputstream");
					e.printStackTrace();
					uu.reInitAccessory();
					mFin = MainActivity.mFin;
					// break;
				}
			}

			// try {
			// mFin.close();
			// } catch (IOException e) {
			// // TODO Auto-generated catch block
			// e.printStackTrace();
			// }

		}

		@Override
		protected void onPostExecute(Long result) {
			// We reached here means our read loop exited.
			// Most common reason is BAD File Descriptor.
			// So, open accessory again with updated FD.
			Log.d(DEBUG_TAG, "ADKReader Post execute called");
			uu.restartReaderThread();
		}

	}

}
