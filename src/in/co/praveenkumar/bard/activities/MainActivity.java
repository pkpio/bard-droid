package in.co.praveenkumar.bard.activities;

import in.co.praveenkumar.bard.R;
import in.co.praveenkumar.bard.graphics.Frame;
import in.co.praveenkumar.bard.io.ADKReader;
import in.co.praveenkumar.bard.io.ADKWriter;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
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
	Frame fp = new Frame(); // Instantiate frame details

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Initialize widgets
		accessoryStatus = (TextView) findViewById(R.id.main_accessory_status);
		// sendData = (EditText) findViewById(R.id.main_send_value);
		receiveData = (TextView) findViewById(R.id.main_read_value);
		sendButton = (Button) findViewById(R.id.main_send_button);
		sampleImage = (ImageView) findViewById(R.id.sample_image);

		// Set image
		ByteBuffer buffer = ByteBuffer.wrap(getImgBytes(new File(
				android.os.Environment.getExternalStorageDirectory(),
				"bard.raw")));
		setupImage(buffer);

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

	// Temporary listener
	public void sndBtnClick(View v) {
		setupImage(Frame.frameBuffer);
	}

	public void setupImage(ByteBuffer buffer) {
		System.out.println("setUpImage called");
		Bitmap bitmap;
		bitmap = Bitmap.createBitmap(1024, 668, Bitmap.Config.RGB_565);
		bitmap.copyPixelsFromBuffer(buffer);
		buffer.rewind();
		sampleImage.setImageBitmap(bitmap);
	}

	public static byte[] getImgBytes(File file) {
		// Open file
		RandomAccessFile f;
		byte[] data = null;

		try {
			f = new RandomAccessFile(file, "r");
			data = new byte[(int) f.length()];
			f.readFully(data);
			f.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return data;

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

		public void updateFrame() {
			setupImage(Frame.frameBuffer);
		}
	}

}
