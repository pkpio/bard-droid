package in.co.praveenkumar.bard.graphics;

public class FrameSettings {

	/**
	 * Width of the frame
	 */
	public static final int WIDTH = 1024;

	/**
	 * Height of the frame
	 */
	public static final int HEIGHT = 768;

	/**
	 * Bytes per pixel
	 */
	public static final int BPP = 2;

	/**
	 * Frames per second.
	 */
	public static final int FPS = 18;

	/**
	 * Bytes per compression. 
	 * <br/>
	 * This is the number of actual bytes that are
	 * encoded into each line of rle compression. This is equal to the number of
	 * bytes in one page on framebuffer. i.e., 4096.
	 */
	public static final int BPC = WIDTH * BPP * 2;

}
