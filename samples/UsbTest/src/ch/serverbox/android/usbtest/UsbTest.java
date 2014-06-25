package ch.serverbox.android.usbtest;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

public class UsbTest extends Activity {
	private final String DEBUG_TAG = "BARD";
	private UsbAccessory mAccessory = null;
	private Button mBtSend = null;
	private FileOutputStream mFout = null;
	private FileInputStream mFin = null;
	private PendingIntent mPermissionIntent = null;
	public Context context;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		Log.d(DEBUG_TAG, "OnCreate");
		context = this;
		// ((NfcManager)getSystemService(NFC_SERVICE)).getDefaultAdapter().enableForegroundDispatch(this,
		// intent, filters, techLists)
		IntentFilter i = new IntentFilter();
		i.addAction(UsbManager.ACTION_USB_ACCESSORY_ATTACHED);
		i.addAction(UsbManager.ACTION_USB_ACCESSORY_DETACHED);
		i.addAction("ch.serverbox.android.usbtest.USBPERMISSION");
		registerReceiver(mUsbReceiver, i);

		if (getIntent().getAction().equals(
				"android.hardware.usb.action.USB_ACCESSORY_ATTACHED")) {
			Log.d(DEBUG_TAG, "Accessory attached.");
			UsbAccessory accessory = UsbManager.getAccessory(getIntent());
			mAccessory = accessory;
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
			startRunner();
		} else {
			UsbAccessory[] accessories = UsbManager.getInstance(this)
					.getAccessoryList();
			for (UsbAccessory a : accessories) {
				l("accessory: " + a.getManufacturer());
				if (a.getManufacturer().equals("BeagleBone")) {
					mPermissionIntent = PendingIntent
							.getBroadcast(
									this,
									0,
									new Intent(
											"ch.serverbox.android.usbtest.USBPERMISSION"),
									0);
					UsbManager.getInstance(this).requestPermission(a,
							mPermissionIntent);
					Log.d(DEBUG_TAG, "permission requested");
					break;
				}
			}
		}

		mBtSend = (Button) (findViewById(R.id.btSebd));
		mBtSend.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				String s = ((EditText) findViewById(R.id.editText1)).getText()
						.toString();
				queueWrite(s);
			}
		});
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mUsbReceiver);
		super.onDestroy();
	}

	public void startRunner() {
		Log.d(DEBUG_TAG, "Runnable request sent");
		Thread t = new Thread(new MyRunnableReader());
		t.start();
	}

	public void queueWrite(final String data) {
		Toast.makeText(this, "queue write clicked", Toast.LENGTH_LONG).show();
		if (mAccessory == null) {
			Toast.makeText(this, "Accessory NULL", Toast.LENGTH_LONG).show();
			return;
		}
		new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					Log.d(DEBUG_TAG, "Writing length " + data.length());
					mFout.write(new byte[] { (byte) data.length() });
					Log.d(DEBUG_TAG, "Writing data: " + data);
					mFout.write(data.getBytes());
					Log.d(DEBUG_TAG, "Done writing");
				} catch (IOException e) {
					e.printStackTrace();
				}
			};
		}).start();
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			Log.d(DEBUG_TAG, "Broadcast received");
			
			if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				Log.d(DEBUG_TAG, "Attached!");
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
						false)) {
					// openAccessory(accessory);
					mAccessory = accessory;
					FileDescriptor fd = null;
					try {
						fd = UsbManager.getInstance(getApplicationContext())
								.openAccessory(accessory).getFileDescriptor();
					} catch (IllegalArgumentException e) {
						finish();
					} catch (NullPointerException e) {
						finish();
					}
					mFout = new FileOutputStream(fd);
					mFin = new FileInputStream(fd);
					mBtSend.setEnabled(true);
				} else {
					Log.d("USB", "permission denied for accessory " + accessory);
				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = UsbManager.getAccessory(intent);
				if (accessory != null && accessory.equals(mAccessory)) {
					if (mFout != null)
						try {
							mFout.close();
						} catch (IOException e) {
							e.printStackTrace();
						}
					mAccessory = null;
					mBtSend.setEnabled(false);
				}
			} else if ("ch.serverbox.android.usbtest.USBPERMISSION"
					.equals(action)) {
				l("permission answered");
				if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
						false)) {
					UsbAccessory[] accessories = UsbManager.getInstance(
							getApplicationContext()).getAccessoryList();
					for (UsbAccessory a : accessories) {
						l("accessory: " + a.getManufacturer());
						if (a.getManufacturer().equals("BeagleBone")) {
							mAccessory = a;
							FileDescriptor fd = null;
							try {
								fd = UsbManager
										.getInstance(getApplicationContext())
										.openAccessory(a).getFileDescriptor();
							} catch (IllegalArgumentException e) {
								finish();
							} catch (NullPointerException e) {
								finish();
							}
							mFout = new FileOutputStream(fd);
							mFin = new FileInputStream(fd);
							l("added accessory");
							break;
						}
					}
				}
			}
		}
	};

	public class MyRunnableReader implements Runnable {
		// For receiving data from Accessory
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
						// &squot;f&squot; is the flag, use for your own logic
						// value is the value from the arduino
						Log.d(DEBUG_TAG, "Value is: " + value);
					}
					i += 1; // number of bytes sent from arduino
					Log.d(DEBUG_TAG, "Bytes received:" + i);
				}

			}
		}
	}

	private void l(String l) {
		Log.d(DEBUG_TAG, l);
	}
}