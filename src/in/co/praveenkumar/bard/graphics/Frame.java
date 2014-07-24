package in.co.praveenkumar.bard.graphics;

import java.nio.ByteBuffer;

public class Frame {
	static final int WIDTH = 1024;
	static final int HEIGHT = 768;
	static final int BPP = 2; // Bytes per pixel
	public static final int FRAME_LENGTH = WIDTH * HEIGHT * BPP;
	
	public static final int ALT_FRAME_LENGTH = WIDTH * 320 * BPP;
	
	//Temp variables
	public static int bytesReceived = 0;
	public static int frameCount = 0;

	// Frame data will be hold in this
	public static ByteBuffer frameBuffer = ByteBuffer.allocate(FRAME_LENGTH);

	public static int add(byte[] page) {
		frameBuffer.put(page);

		// Reset position if end is reached
		if (frameBuffer.position() >= FRAME_LENGTH)
			frameBuffer.position(0);

		return frameBuffer.position();
	}

	public static ByteBuffer current() {
		return frameBuffer;
	}
}
