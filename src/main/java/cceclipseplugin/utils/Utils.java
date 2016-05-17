package cceclipseplugin.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class Utils {

	/**
	 * Encodes the given string into UTF-8 URL encoding
	 * @param str string to be encoded
	 * @return encoded version of str
	 */
	public static String urlEncode(String str) {
		try {
			return URLEncoder.encode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Should never happen; UTF-8 is pretty standard
			throw new IllegalArgumentException("Invalid encoding", e);
		}
	}

	/**
	 * Decodes the given string from UTF-8 URL encoding
	 * @param str string to be decoded
	 * @return decoded version of str
	 */
	public static String urlDecode(String str) {
		try {
			return URLDecoder.decode(str, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Should never happen; UTF-8 is pretty standard
			throw new IllegalArgumentException("Invalid encoding", e);
		}
	}

}
