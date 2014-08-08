package in.co.praveenkumar.bard.activities;

import in.co.praveenkumar.bard.R;
import in.co.praveenkumar.bard.graphics.Frame;
import in.co.praveenkumar.bard.io.ADKReader;
import in.co.praveenkumar.bard.io.USBControl;

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

	// Handler, Threads
	private Handler UIHandler = new Handler();
	private USBControlServer usbConnection;

	// Activity Lifecycle
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		setupUSB();

	}

	private void setupUSB() {

		System.out.println("Starting USB...");
		usbConnection = new USBControlServer(UIHandler);
		System.out.println("Done\n");
	}

	public class USBControlServer extends USBControl {

		public USBControlServer(Handler ui) {
			super(getApplicationContext(), ui);
		}

		@Override
		public void onReceive(byte[] msg) {

			int i = 0;
			System.out.println("Received onReceive");
			int pageIndex = (int) (msg[0] & 0x0000000ff)
					+ (int) (msg[1] << 8 & 0x0000ff00);
			
			// Update frame data
			int framePos = pageIndex * 4096;
			if ((framePos - (msg.length - 2)) <= Frame.FRAME_LENGTH) {
				Frame.frameBuffer.position(framePos);
				Frame.frameBuffer.put(msg, 2, msg.length - 2);
			}

		}

		@Override
		public void onNotify(String msg) {
			// console(msg);
		}

		@Override
		public void onConnected() {
			// usb.enable();
		}

		@Override
		public void onDisconnected() {
			// usb.pause();
			finish();
		}

	}

}