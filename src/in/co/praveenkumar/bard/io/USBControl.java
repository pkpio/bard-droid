package in.co.praveenkumar.bard.io;

import in.co.praveenkumar.bard.graphics.Frame;
import in.co.praveenkumar.bard.graphics.FrameSettings;
import in.co.praveenkumar.bard.helpers.RleDecoder;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbAccessory;
import android.hardware.usb.UsbManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ParcelFileDescriptor;

/**
 * Configures a USB accessory and its input/output streams.
 * 
 * Call this.send to sent a byte array to the accessory Override onReceive to
 * process incoming bytes from accessory
 */

public abstract class USBControl extends Thread {

	// The permission action
	private static final String ACTION_USB_PERMISSION = "in.co.praveenkumar.bard.activities.MainActivity.USBPERMISSION";

	// An instance of accessory and manager
	private UsbAccessory mAccessory;
	private UsbManager mManager;
	private Context context;
	private Handler UIHandler;
	private Handler controlSender;
	private Thread controlListener;
	boolean connected = false;
	private ParcelFileDescriptor mFileDescriptor;
	private FileInputStream input;
	private FileOutputStream output;

	// Receiver for connect/disconnect events
	BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();

			if (ACTION_USB_PERMISSION.equals(action)) {
				synchronized (this) {
					UsbAccessory accessory = (UsbAccessory) intent
							.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
					if (intent.getBooleanExtra(
							UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
						openAccessory(accessory);
					} else {

					}

				}
			} else if (UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action)) {
				UsbAccessory accessory = (UsbAccessory) intent
						.getParcelableExtra(UsbManager.EXTRA_ACCESSORY);
				if (accessory != null && accessory.equals(mAccessory)) {
					closeAccessory();
				}
			}

		}
	};

	// Configures the usb connection
	public USBControl(Context main, Handler ui) {
		super("USBControlSender");
		UIHandler = ui;
		context = main;

		mManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
		UsbAccessory[] accessoryList = mManager.getAccessoryList();
		PendingIntent mPermissionIntent = PendingIntent.getBroadcast(context,
				0, new Intent(ACTION_USB_PERMISSION), 0);
		IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
		context.registerReceiver(mUsbReceiver, filter);

		UsbAccessory mAccessory = (accessoryList == null ? null
				: accessoryList[0]);
		if (mAccessory != null) {

			while (!mManager.hasPermission(mAccessory)) {
				mManager.requestPermission(mAccessory, mPermissionIntent);
			}
			openAccessory(mAccessory);

		}

	}

	// Send byte array over connection
	public void send(byte[] command) {
		if (controlSender != null) {
			Message msg = controlSender.obtainMessage();
			msg.obj = command;
			controlSender.sendMessage(msg);
		}
	}

	// Receive byte array over connection
	private void receive(final byte[] msg) {

		// pass to ui thread for processing
		UIHandler.post(new Runnable() {
			public void run() {
				onReceive(msg);
			}
		});
	}

	public abstract void onReceive(byte[] msg);

	public abstract void onNotify(String msg);

	public abstract void onConnected();

	public abstract void onDisconnected();

	@Override
	public void run() {
		// Listens for messages from usb accessory
		controlListener = new Thread(new Runnable() {
			boolean running = true;

			public void run() {
				while (running) {
					byte[] msg = new byte[4096];
					try {
						// Handle incoming messages
						while (input != null && input.read(msg) != -1
								&& running) {
							// receive(msg);
							System.out.println("Read USB data");

							/*
							 * Read RLE encoded page length. This is generally
							 * equal to the number of bytes read. We need to
							 * handle decoding based on this value when there is
							 * a mismatch from bytes received.
							 */
							int rled_length = (int) (msg[0] & 0x0000000ff)
									+ (int) (msg[1] << 8 & 0x0000ff00);

							System.out.println("rled_length : " + rled_length);

							// Read pageIndex
							int pageIndex = (int) (msg[2] & 0x0000000ff)
									+ (int) (msg[3] << 8 & 0x0000ff00);

							System.out.println("Page index : " + pageIndex);

							// Decode RLE data
							RleDecoder rled = new RleDecoder();
							rled.decode(msg, 4, msg.length - 4);

							// Update frame data
							int framePos = pageIndex * 4096;
							if ((framePos - (msg.length - 2)) <= Frame.FRAME_LENGTH) {
								Frame.frameBuffer.position(framePos);
								Frame.frameBuffer.put(msg, 2, msg.length - 2);
							}

						}
					} catch (final Exception e) {
						UIHandler.post(new Runnable() {
							public void run() {
								onNotify("USB Receive Failed " + e.toString()
										+ "\n");
								closeAccessory();
							}
						});
						running = false;
					}
				}
			}
		});
		controlListener.setDaemon(true);
		controlListener.setName("USBCommandListener");
		controlListener.start();

		// Sends messages to usb accessory
		Looper.prepare();
		controlSender = new Handler() {
			public void handleMessage(Message msg) {
				try {
					output.write((byte[]) msg.obj);
				} catch (final Exception e) {
					UIHandler.post(new Runnable() {
						public void run() {
							onNotify("USB Send Failed " + e.toString() + "\n");
						}
					});
					controlSender.getLooper().quit();
				}
			}
		};
		Looper.loop();
	}

	// Sets up filestreams
	private void openAccessory(UsbAccessory accessory) {
		mAccessory = accessory;
		mFileDescriptor = mManager.openAccessory(accessory);
		if (mFileDescriptor != null) {
			FileDescriptor fd = mFileDescriptor.getFileDescriptor();
			input = new FileInputStream(fd);
			output = new FileOutputStream(fd);
		}
		this.start();
		onConnected();
	}

	// Cleans up accessory
	public void closeAccessory() {

		// halt i/o
		controlSender.getLooper().quit();
		controlListener.interrupt();

		try {
			if (mFileDescriptor != null) {
				mFileDescriptor.close();
			}
		} catch (IOException e) {
		} finally {
			mFileDescriptor = null;
			mAccessory = null;
		}

		onDisconnected();
	}

	// Removes the usb receiver
	public void destroyReceiver() {
		context.unregisterReceiver(mUsbReceiver);
	}

}
