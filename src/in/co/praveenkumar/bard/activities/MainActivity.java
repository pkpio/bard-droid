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

	ImageView remoteScreen;

	// Handler, Threads
	private Handler UIHandler = new Handler();
	private USBControlServer usbConnection;

	// Activity Lifecycle
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		remoteScreen = (ImageView) findViewById(R.id.remote_screen);

		setupUSB();

		// Start Frame updater thread
		frameUpdate();

	}

	public void updateImage() {
		/*
		 * A copy because we want the position to be set to 0 and the original
		 * position in Frame could be at a different value
		 */
		// ByteBuffer buffer = Frame.frameBuffer;

		System.out.println("setUpImage called");
		Bitmap bitmap;
		// buffer.position(0);
		bitmap = Bitmap.createBitmap(1024, 768, Bitmap.Config.RGB_565);

		/*
		 * -TODO- Some strange thing here. Sometimes copyPixelsFromBuffer is
		 * reading outside the buffer range
		 */
		try {
			Frame.frameBuffer.position(0);
			bitmap.copyPixelsFromBuffer(Frame.frameBuffer);
			remoteScreen.setImageBitmap(bitmap);
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("Exception with copyPixelsFromBuffer");
		}

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
			// System.out.println("Received onReceive");
			// int pageIndex = (int) (msg[0] & 0x0000000ff)
			// + (int) (msg[1] << 8 & 0x0000ff00);
			//
			// System.out.println("Page index : " + pageIndex);
			//
			// // Update frame data
			// int framePos = pageIndex * 4096;
			// if ((framePos - (msg.length - 2)) <= Frame.FRAME_LENGTH) {
			// Frame.frameBuffer.position(framePos);
			// Frame.frameBuffer.put(msg, 2, msg.length - 2);
			// }

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

	private void frameUpdate() {
		updateImage();

		// Wait before doing next frame update
		Handler myHandler = new Handler();
		myHandler.postDelayed(frameUpdater, 250);
	}

	private Runnable frameUpdater = new Runnable() {
		@Override
		public void run() {
			frameUpdate();
		}
	};

}