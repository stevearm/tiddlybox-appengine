package com.horsefire.tiddly.appengine;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

public class IoUtils {

	public static String getString(byte[] data) {
		return new String(data);
	}

	public static String getString(InputStream in) throws IOException {
		return new String(getBytes(in));
	}

	public static byte[] getBytes(InputStream in) throws IOException {
		return IOUtils.toByteArray(in);
	}
}
