package in.co.praveenkumar.bard.activities;

import in.co.praveenkumar.bard.R;
import in.co.praveenkumar.bard.graphics.Frame;
import in.co.praveenkumar.bard.io.ADKReader;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.nio.ByteBuffer;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class MainActivity extends Activity {
	final String DEBUG_TAG = "BARD";
	final String IDENT_MANUFACTURER = "BeagleBone";
	final String USB_PERMISSION = "in.co.praveenkumar.bard.activities.MainActivity.USBPERMISSION";

	UsbAccessory mAccessory = null;
	public static FileInputStream mFin = null;
	ADKReader mADKReader;

	PendingIntent mPermissionIntent = null;

	TextView accessoryStatus;
	ImageView remoteScreen;

	Frame fp = new Frame(); // Instantiate frame details

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Initialize widgets
		accessoryStatus = (TextView) findViewById(R.id.main_accessory_status);
		remoteScreen = (ImageView) findViewById(R.id.remote_screen);

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

		// Start Frame updater thread
		frameUpdate();
	}

	// Temporary listener
	public void sndBtnClick(View v) {
		updateImage(Frame.frameBuffer);
	}

	public void updateImage(ByteBuffer buffer) {
		System.out.println("setUpImage called");
		Bitmap bitmap;
		buffer.position(0);
		bitmap = Bitmap.createBitmap(1024, 768, Bitmap.Config.RGB_565);

		/*
		 * -TODO- Some strange thing here. Sometimes copyPixelsFromBuffer is
		 * reading outside the buffer range
		 */
		try {
			bitmap.copyPixelsFromBuffer(buffer);
			buffer.rewind();
			remoteScreen.setImageBitmap(bitmap);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exception with copyPixelsFromBuffer");
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
		mFin = new FileInputStream(fd);
		mADKReader = new ADKReader(mFin, new UIUpdater(), this, accessory);

		// Start to monitor incoming data
		mADKReader.start();
	}

	private void closeAccessory(Intent i) {
		UsbAccessory accessory = UsbManager.getAccessory(i);
		if (accessory != null && accessory.equals(mAccessory)) {
			mAccessory = null;
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

	public class UIUpdater {

		public void reInitAccessory() {
			resetInputStream();
		}

		public void restartReaderThread() {
			initAccessory(mAccessory);
		}

		public void updateFrame() {
			updateImage(Frame.frameBuffer);
		}

	}

	private void frameUpdate() {
		updateImage(Frame.frameBuffer);

		// Wait before doing next frame update
		Handler myHandler = new Handler();
		myHandler.postDelayed(frameUpdater, 500);
	}

	private Runnable frameUpdater = new Runnable() {
		@Override
		public void run() {
			frameUpdate();
		}
	};

	public void resetInputStream() {
		FileDescriptor fd = null;
		try {
			fd = UsbManager.getInstance(this).openAccessory(mAccessory)
					.getFileDescriptor();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			finish();
		} catch (NullPointerException e) {
			e.printStackTrace();
			finish();
		}
		mFin = new FileInputStream(fd);
	}

}