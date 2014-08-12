/*
 * We won't know the length of the output data ahead of decoding so,
 * we need a byte data structure that can expand as the data comes in. 
 */

package in.co.praveenkumar.bard.helpers;

import java.io.ByteArrayOutputStream;

public class RleDecoder {
	byte[] outData;

	/**
	 * <p>
	 * Decodes RLE encoded data into plain form. Depending on the the length and
	 * contents of the input data, this might take some time to evaluate.
	 * </p>
	 * <br/>
	 * <p>
	 * It is assumed that the encoding scheme followed takes 2 bytes as basic
	 * block and uses special char 'rr' when there is a repetition of data. More
	 * details about the encoding scheme can be found at,
	 * https://github.com/praveendath92/udlfb
	 * </p>
	 * <br/>
	 * 
	 * @param rleData
	 *            RLE encoded in byte[].
	 * @param offset
	 * 			  Data offset in rleData from where RLE data starts
	 * @param length
	 * 			  Length of RLE data in rleData 
	 * @return decode plain data in byte[]
	 */
	public byte[] decode(byte[] rleData, int offset, int length) {
		int pos = offset;
		int count = 0;
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		while (pos < length) {

			// Check for special chars for repeated data
			if (rleData[pos] == 'r' && rleData[pos + 1] == 'r') {
				// Get the repetition count from next byte of special chars
				pos = pos + 2;
				count = rleData[pos]&0x000000ff;

				// Get pixel data from next two bytes
				pos++;
				while (count != 0){
					out.write(rleData, pos, 2);
					count--;
				}

			}

			// No repetition, just write out the input data.
			else
				out.write(rleData, pos, 2);

			pos = pos + 2;
		}

		outData = out.toByteArray();
		return outData;
	}

}
