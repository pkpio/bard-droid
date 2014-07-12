package in.co.praveenkumar.bard.activities;

import in.co.praveenkumar.bard.R;
import in.co.praveenkumar.bard.io.ADKReader;
import in.co.praveenkumar.bard.io.ADKWriter;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class MainActivity extends Activity {
	final String DEBUG_TAG = "BARD";
	final String IDENT_MANUFACTURER = "BeagleBone";
	final String USB_PERMISSION = "in.co.praveenkumar.bard.activities.MainActivity.USBPERMISSION";
	UsbAccessory mAccessory = null;
	FileOutputStream mFout = null;
	FileInputStream mFin = null;
	ADKReader mADKReader;
	ADKWriter mADKWriter;
	PendingIntent mPermissionIntent = null;
	TextView accessoryStatus;
	EditText sendData;
	TextView receiveData;
	Button sendButton;
	ImageView sampleImage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Initialize widgets
		accessoryStatus = (TextView) findViewById(R.id.main_accessory_status);
		sendData = (EditText) findViewById(R.id.main_send_value);
		receiveData = (TextView) findViewById(R.id.main_read_value);
		sendButton = (Button) findViewById(R.id.main_send_button);
		sampleImage = (ImageView) findViewById(R.id.sample_image);

		// Set image
		setupImage();

		// Register receiver for actions
		IntentFilter i = new IntentFilter();
		i.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		i.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		i.addAction(USB_PERMISSION);
		registerReceiver(mUsbReceiver, i);

		if (getIntent().getAction().equals(
				"android.hardware.usb.action.USB_ACCESSORY_ATTACHED"))
			openAccessory(getIntent());
		else
			requestPermission();

	}

	public void setupImage() {
		System.out.println("setUpImage called");
		Bitmap bitmap;
		bitmap = BitmapFactory.decodeFile((new File(android.os.Environment
				.getExternalStorageDirectory(), "bard.raw")).toString());
		// bitmap = decodeImage(new File(
		// android.os.Environment.getExternalStorageDirectory(),
		// "bard.raw"));
		System.out.println("reached 1");
		// Bitmap bitmap = Bitmap.createBitmap(1024, 768,
		// Bitmap.Config.RGB_565);
		// ByteBuffer buffer = ByteBuffer.wrap(readFile(new File(
		// android.os.Environment.getExternalStorageDirectory(),
		// "bard.raw")));
		// // buffer.flip();
		// bitmap.copyPixelsFromBuffer(buffer);
		// buffer.rewind();
		if (bitmap == null)
			System.out.println("Null bitmap");
		sampleImage.setImageBitmap(bitmap);
	}

	private static Bitmap decodeImage(File f) {
		try {
			// Decode image size
			BitmapFactory.Options o = new BitmapFactory.Options();
			o.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(new FileInputStream(f), null, o);

			// The new size we want to scale to
			final int REQUIRED_SIZE = 70;

			System.out.println("reached 2");

			// Find the correct scale value. It should be the power of 2.
			int scale = 1;
			while (o.outWidth / scale / 2 >= REQUIRED_SIZE
					&& o.outHeight / scale / 2 >= REQUIRED_SIZE)
				scale *= 2;

			// Decode with inSampleSize
			BitmapFactory.Options o2 = new BitmapFactory.Options();
			o2.inSampleSize = scale;
			return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
		} catch (FileNotFoundException e) {
			System.out.println("File not found");
		}

		System.out.println("Returning null");
		return null;
	}

	public static byte[] readFile(File file) throws IOException {
		// Open file
		RandomAccessFile f = new RandomAccessFile(file, "r");
		try {
			// Get and check length
			long longlength = f.length();
			int length = (int) longlength;
			if (length != longlength)
				throw new IOException("File size >= 2 GB");
			// Read file and return data
			byte[] data = new byte[length];
			f.readFully(data);
			return data;
		} finally {
			f.close();
		}
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}

	private void openAccessory(Intent i) {
		Log.d(DEBUG_TAG, "Accessory attached");
		UsbAccessory accessory = UsbManager.getAccessory(i);
		mAccessory = accessory;
		initAccessory(mAccessory);
	}

	private void initAccessory(UsbAccessory accessory) {
		FileDescriptor fd = null;
		try {
			fd = UsbManager.getInstance(this).openAccessory(accessory)
					.getFileDescriptor();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			finish();
		} catch (NullPointerException e) {
			e.printStackTrace();
			finish();
		}
		mFout = new FileOutputStream(fd);
		mFin = new FileInputStream(fd);
		mADKReader = new ADKReader(mFin, new UIUpdater(), this, accessory);
		mADKWriter = new ADKWriter(mFout);
		widgetsAvailable(true);

		// Start to monitor incoming data
		mADKReader.start();
	}

	private void closeAccessory(Intent i) {
		UsbAccessory accessory = UsbManager.getAccessory(i);
		if (accessory != null && accessory.equals(mAccessory)) {
			if (mFout != null)
				try {
					mFout.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			mAccessory = null;
			widgetsAvailable(false);
		}
	}

	private void requestPermission() {
		Log.d(DEBUG_TAG, "Requesting permission");

		UsbAccessory[] accessories = UsbManager.getInstance(this)
				.getAccessoryList();

		if (accessories != null) {
			for (UsbAccessory a : accessories) {
				Log.d(DEBUG_TAG, "accessory: " + a.getManufacturer());
				if (a.getManufacturer().equals(IDENT_MANUFACTURER)) {
					mPermissionIntent = PendingIntent.getBroadcast(this, 0,
							new Intent(USB_PERMISSION), 0);
					UsbManager.getInstance(this).requestPermission(a,
							mPermissionIntent);
					Log.d(DEBUG_TAG, "permission requested");
					break;
				}
			}
		} else {
			Log.d(DEBUG_TAG, "No accessories");
			widgetsAvailable(false);
		}
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d(DEBUG_TAG, "Broadcast received");
			String action = intent.getAction();

			// Attached
			if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
						false))
					openAccessory(intent);
				else
					Log.d(DEBUG_TAG, "permission denied for accessory");
			}

			// Detached
			else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				closeAccessory(intent);
			}

			// Permission answered
			else if (USB_PERMISSION.equals(action)) {
				Log.d(DEBUG_TAG, "Permission answered");

				// Permission granted
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
						false)) {
					UsbAccessory[] accessories = UsbManager.getInstance(
							getApplicationContext()).getAccessoryList();
					for (UsbAccessory a : accessories) {
						Log.d(DEBUG_TAG, "accessory: " + a.getManufacturer());
						if (a.getManufacturer().equals("BeagleBone")) {
							mAccessory = a;
							initAccessory(mAccessory);
						}
					}
				}

				// permission not granted
				else {
					// TO-DO
				}

			}

		}
	};

	private void widgetsAvailable(Boolean status) {
		if (status) {
			sendButton.setEnabled(true);
			accessoryStatus.setText("attached!");
		} else {
			sendButton.setEnabled(false);
			accessoryStatus.setText("detached!");
		}

	}

	public class UIUpdater {
		public void setRead(String value) {
			if (receiveData != null) {
				receiveData.setText(value);
			}
		}

		public void reInitAccessory() {
			initAccessory(mAccessory);
		}
	}

}
