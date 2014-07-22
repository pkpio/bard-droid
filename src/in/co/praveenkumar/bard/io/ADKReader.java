package in.co.praveenkumar.bard.io;

import in.co.praveenkumar.bard.activities.MainActivity.UIUpdater;
import in.co.praveenkumar.bard.graphics.Frame;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
				byte[] buffer = new byte[16384];
				try {
					try {
						// There is an initial off-set of 76 Bytes in 1st frame
						// See how it can be fixed in udlfb
						if (Frame.bytesReceived >= Frame.FRAME_LENGTH) {
							frame++;
							length = 0;
							Frame.bytesReceived = 0;
							Frame.frameCount++;
							Log.d(DEBUG_TAG, "Frame is: " + frame);
							publishProgress(0);
						}

						File file = new File(
								android.os.Environment
										.getExternalStorageDirectory(),
								"bard" + Frame.frameCount + ".raw");
						f = new FileOutputStream(file, true);

					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}

					Log.d(DEBUG_TAG, "Trying to buffer read");
					Frame.bytesReceived += mFin.read(buffer);
					Log.d(DEBUG_TAG, "Buffer read");

					if (f != null) {
						f.write(buffer);
						f.flush();
						f.close();
					}

					Log.d(DEBUG_TAG, "Bytes got: " + Frame.bytesReceived);

					// Write to frameBuffer
					 int pos = Frame.add(buffer);

					// Draw the frame if end is reached
					// if (pos == 0 && callCount != 0)
					// publishProgress(0);

					// Testing
					// System.out.println("Capacity: "
					// + Frame.frameBuffer.position());
					// System.out.println("Position: "
					// + Frame.frameBuffer.capacity());

					// callCount++;
					// Log.d(DEBUG_TAG, "mCurrently at: " + length + " count:"
					// + callCount);
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
				// read = bytesToHex(buffer);// Arrays.toString(buffer);

				// publishProgress(0);
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

	final protected static char[] hexArray = "0123456789ABCDEF".toCharArray();

	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

}
