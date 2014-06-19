package ch.serverbox.android.usbtest;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class UsbTest extends Activity {
	private final String DEBUG_TAG = "BARD";
	private UsbAccessory mAccessory = null;
	private Button mBtSend = null;
	private FileOutputStream mFout = null;
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
		Toast.makeText(this, "OnCreate", Toast.LENGTH_LONG).show();

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

	public void queueWrite(final String data) {
		Toast.makeText(this, "queue write clicked", Toast.LENGTH_LONG).show();
		if (mAccessory == null) {
			Toast.makeText(this, "Accessory NULL", Toast.LENGTH_LONG).show();
			return;
		}
		String test = null;
		new Thread(new Runnable() {
			String test2;
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
					test2 = e.toString();
				}
			};
		}).start();
		Toast.makeText(context, test, Toast.LENGTH_LONG).show();
	}

	private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
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
							l("added accessory");
							break;
						}
					}
				}
			}
		}
	};

	private void l(String l) {
		Log.d(DEBUG_TAG, l);
	}
}