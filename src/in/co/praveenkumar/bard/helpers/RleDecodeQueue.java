package in.co.praveenkumar.bard.helpers;

import java.util.ArrayList;

public class RleDecodeQueue {
	static int pendingDecodes = 0;
	static ArrayList<byte[]> queue = new ArrayList<byte[]>();

	/**
	 * Adds a byte[] to the rle decode queue
	 * @param data
	 * 		byte[] rle data.
	 */
	public static void add(byte[] data) {
		queue.add(data);
		pendingDecodes++;
	}

	/**
	 * Get the byte[] at front of the queue and removes it.
	 * 
	 * @return
	 * byte[] rle encoded data
	 */
	public static byte[] getHead() {
		byte[] data = queue.get(0);
		queue.remove(0);
		pendingDecodes--;
		return data;
	}

	/**
	 * The number pending decodes.
	 * @return
	 * int pendingDecodes
	 */
	public static int pending() {
		return pendingDecodes;
	}
}
