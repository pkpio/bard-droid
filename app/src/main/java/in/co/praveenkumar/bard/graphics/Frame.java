package in.co.praveenkumar.bard.graphics;

import java.nio.ByteBuffer;

public class Frame {
	public static final int FRAME_LENGTH = FrameSettings.WIDTH
			* FrameSettings.HEIGHT * FrameSettings.BPP;

	// Temp variables
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
