package in.co.praveenkumar.bard.activities;

import in.co.praveenkumar.bard.R;
import in.co.praveenkumar.bard.graphics.Frame;
import in.co.praveenkumar.bard.io.USBControl;
import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;

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
		System.out.println("setUpImage called");
		Bitmap bitmap;
		bitmap = Bitmap.createBitmap(1024, 768, Bitmap.Config.RGB_565);

		/*
		 * -TODO- - Some strange thing here. Sometimes copyPixelsFromBuffer is
		 * reading outside the buffer range - A possible race condition with
		 * position being of Frame being set from reader thread.
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
		myHandler.postDelayed(frameUpdater, 40);
	}

	private Runnable frameUpdater = new Runnable() {
		@Override
		public void run() {
			frameUpdate();
		}
	};

}