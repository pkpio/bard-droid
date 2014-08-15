package in.co.praveenkumar.bard.helpers;

import java.util.ArrayList;

public class RleDecodeQueue {
	static int pendingDecodes = 0;
	static ArrayList<byte[]> queue = new ArrayList<byte[]>();

	/**
	 * Adds a byte[] to the rle decode queue
	 * 
	 * @param data
	 *            byte[] rle data.
	 */
	public static void add(byte[] data) {
		queue.add(data);
		System.out.println(queue.get(0));
		//pendingDecodes++;
		System.out.println("Pending decodes : " + queue.size());
	}

	/**
	 * Get the byte[] at front of the queue and removes it.
	 * 
	 * @return byte[] rle encoded data
	 */
	public static byte[] getHead() {
		//pendingDecodes--;
		System.out.println("Yo Pending decodes : " + queue.size());
		return queue.remove(0);
	}

	/**
	 * The number pending decodes.
	 * 
	 * @return int pendingDecodes
	 */
	public static int pending() {
		return queue.size();
	}
}
