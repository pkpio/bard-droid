package in.co.praveenkumar.bard.activities;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import in.co.praveenkumar.bard.R;
import in.co.praveenkumar.bard.io.ADKReader;
import in.co.praveenkumar.bard.io.ADKWriter;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// Register receiver for actions
		IntentFilter i = new IntentFilter();
		i.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		i.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		i.addAction("ch.serverbox.android.usbtest.USBPERMISSION");
		registerReceiver(mUsbReceiver, i);

		if (getIntent().getAction().equals(
				"android.hardware.usb.action.USB_ACCESSORY_ATTACHED"))
			openAccessory(getIntent());
		else
			requestPermission();

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
			finish();
		} catch (NullPointerException e) {
			finish();
		}
		mFout = new FileOutputStream(fd);
		mFin = new FileInputStream(fd);
		mADKReader = new ADKReader(mFin);
		mADKWriter = new ADKWriter(mFout);

		// New thread to monitor incoming data
		Thread readLogger = new Thread(mADKReader);
		readLogger.start();
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
		}
	}

	private void requestPermission() {
		UsbAccessory[] accessories = UsbManager.getInstance(this)
				.getAccessoryList();
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

}
