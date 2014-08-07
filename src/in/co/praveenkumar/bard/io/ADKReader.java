package in.co.praveenkumar.bard.io;

import in.co.praveenkumar.bard.activities.MainActivity.UIUpdater;
import in.co.praveenkumar.bard.graphics.Frame;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.android.future.usb.UsbAccessory;

public class ADKReader {
	final String DEBUG_TAG = "BARD.IO.ADKReader";

	static FileInputStream mFin = null;
	static UIUpdater uu = null;
	static Context context = null;
	static UsbAccessory mAccessory = null;
	static FileDescriptor fd = null;

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
		// String read = "";

		int length = 0;
		int frame = 0;
		int callCount = 0;

		@Override
		protected void onProgressUpdate(Integer... progress) {
			uu.updateFrame();
		}

		@Override
		protected Long doInBackground(Integer... params) {

			FileOutputStream f = null;

			Log.d(DEBUG_TAG, "ADKReader doInbackground called");

			while (true) { // read data
				byte[] buffer = new byte[4098];
				try {
					if (mFin != null) {
						System.out.println("mFin available: "
								+ mFin.available());
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
					break;
				} catch (Exception e) {
					Log.d(DEBUG_TAG,
							"Unknow exception while getting inputstream");
					e.printStackTrace();
					break;
				}
			}

			try {
				mFin.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return null;
		}

		@Override
		protected void onPostExecute(Long result) {
			// We reached here means our read loop exited.
			// Most common reason is BAD File Descriptor.
			// So, open accessory again with updated FD.
			Log.d(DEBUG_TAG, "ADKReader Post execute called");
			System.out.println(Frame.frameBuffer.capacity());
			uu.reInitAccessory();
		}

	}

}
