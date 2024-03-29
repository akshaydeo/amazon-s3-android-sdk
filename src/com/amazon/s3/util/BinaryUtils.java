package com.amazon.s3.util;

/*
 * Copyright 2010-2013 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Portions copyright 2006-2009 James Murty. Please see LICENSE.txt
 * for applicable license terms and NOTICE.txt for applicable notices.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Locale;

import org.apache.commons.codec.binary.Base64;

import android.util.Log;

/**
 * Utilities for encoding and decoding binary data to and from different forms.
 */
public class BinaryUtils {

	/** Default encoding when extracting binary data from a String */
	private static final String DEFAULT_ENCODING = "UTF-8";

	private static final String TAG = "###BinaryUtils###";

	/**
	 * Converts byte data to a Hex-encoded string.
	 * 
	 * @param data
	 *            data to hex encode.
	 * 
	 * @return hex-encoded string.
	 */
	public static String toHex(byte[] data) {
		StringBuilder sb = new StringBuilder(data.length * 2);
		for (int i = 0; i < data.length; i++) {
			String hex = Integer.toHexString(data[i]);
			if (hex.length() == 1) {
				// Append leading zero.
				sb.append("0");
			} else if (hex.length() == 8) {
				// Remove ff prefix from negative numbers.
				hex = hex.substring(6);
			}
			sb.append(hex);
		}
		return sb.toString().toLowerCase(Locale.getDefault());
	}

	/**
	 * Converts a Hex-encoded data string to the original byte data.
	 * 
	 * @param hexData
	 *            hex-encoded data to decode.
	 * @return decoded data from the hex string.
	 */
	public static byte[] fromHex(String hexData) {
		byte[] result = new byte[(hexData.length() + 1) / 2];
		String hexNumber = null;
		int stringOffset = 0;
		int byteOffset = 0;
		while (stringOffset < hexData.length()) {
			hexNumber = hexData.substring(stringOffset, stringOffset + 2);
			stringOffset += 2;
			result[byteOffset++] = (byte) Integer.parseInt(hexNumber, 16);
		}
		return result;
	}

	/**
	 * Converts byte data to a Base64-encoded string.
	 * 
	 * @param data
	 *            data to Base64 encode.
	 * @return encoded Base64 string.
	 */
	public static String toBase64(byte[] data) {
		byte[] b64 = Base64.encodeBase64(data);
		return new String(b64);
	}

	/**
	 * Converts a Base64-encoded string to the original byte data.
	 * 
	 * @param b64Data
	 *            a Base64-encoded string to decode.
	 * 
	 * @return bytes decoded from a Base64 string.
	 */
	public static byte[] fromBase64(String b64Data) {
		byte[] decoded;
		try {
			decoded = Base64.decodeBase64(b64Data.getBytes(DEFAULT_ENCODING));
		} catch (UnsupportedEncodingException uee) {
			// Shouldn't happen if the string is truly Base64 encoded.
			Log.w(TAG,
					"Tried to Base64-decode a String with the wrong encoding: ",
					uee);
			decoded = Base64.decodeBase64(b64Data.getBytes());
		}
		return decoded;
	}

	/**
	 * Wraps a ByteBuffer in an InputStream.
	 * 
	 * @param byteBuffer
	 *            The ByteBuffer to wrap.
	 * 
	 * @return An InputStream wrapping the ByteBuffer content.
	 */
	public static InputStream toStream(ByteBuffer byteBuffer) {
		byte[] bytes = new byte[byteBuffer.remaining()];
		byteBuffer.get(bytes);
		return new ByteArrayInputStream(bytes);
	}

}
