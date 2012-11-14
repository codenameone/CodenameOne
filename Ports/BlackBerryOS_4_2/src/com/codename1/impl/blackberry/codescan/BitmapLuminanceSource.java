/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.impl.blackberry.codescan;

import com.google.zxing.LuminanceSource;
import net.rim.device.api.system.Bitmap;

/**
 *
 * @author nirmal
 */
public class BitmapLuminanceSource extends LuminanceSource {
	private final Bitmap _bitmap;
	private byte[] _matrix;

	/**
	 * Construct luminance source for specified Bitmap 
	 * @param bitmap
	 */
	public BitmapLuminanceSource(Bitmap bitmap) {
		super(bitmap.getWidth(), bitmap.getHeight());
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		_bitmap = bitmap;

		int area = width * height;
		_matrix = new byte[area];
		int[] rgb = new int[area];

		_bitmap.getARGB(rgb, 0, width, 0, 0, width, height);

		for (int y = 0; y < height; y++) {
			int offset = y * width;
			for (int x = 0; x < width; x++) {
				int pixel = rgb[offset + x];
				int luminance = (306 * ((pixel >> 16) & 0xFF) + 601
						* ((pixel >> 8) & 0xFF) + 117 * (pixel & 0xFF)) >> 10;
				_matrix[offset + x] = (byte) luminance;
			}
		}

		rgb = null;

	}

	/**
	 * Get the byte for specified row, starts from 0.
	 */
	public byte[] getRow(int y, byte[] row) {
		if (y < 0 || y >= getHeight()) {
			throw new IllegalArgumentException(
					"Requested row is outside the image: " + y);
		}

		int width = getWidth();
		if (row == null || row.length < width) {
			row = new byte[width];
		}

		int offset = y * width;
		System.arraycopy(this._matrix, offset, row, 0, width);

		return row;
	}

	/**
	 * Get the byte matrix in 1-dimensional array of byte
	 */
	public byte[] getMatrix() {
		return _matrix;
	}

}
