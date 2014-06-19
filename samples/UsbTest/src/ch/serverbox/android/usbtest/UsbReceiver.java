package ch.serverbox.android.usbtest;

import com.android.future.usb.UsbAccessory;
import com.android.future.usb.UsbManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class UsbReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action)) {
			UsbAccessory accessory = UsbManager.getAccessory(intent);
			Log.d("USB", "Attached!");
			Toast.makeText(context, "Attached", Toast.LENGTH_LONG).show();
			if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED,
					false)) {
				// openAccessory(accessory);
				Log.d("TestUSB", "Open Accessory");
				Toast.makeText(context, "Open Accessory", Toast.LENGTH_LONG)
						.show();
			} else {
				Log.d("USB", "permission denied for accessory " + accessory);
			}
		} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
			UsbAccessory accessory = UsbManager.getAccessory(intent);
			Toast.makeText(context, "Detached", Toast.LENGTH_LONG).show();
		}
	}
}
