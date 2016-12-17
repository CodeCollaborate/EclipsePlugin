package cceclipseplugin.util;

import java.util.Locale;

public class OSUtil {
	
	private static String osType = null;
	
	private static String getOSType() {
		if (osType == null) {
			osType = System.getProperty("os.name", "generic").toLowerCase(Locale.ENGLISH);
		}
		return osType;
	}
	
	public static boolean isWindows() {
		return getOSType().indexOf("win") >= 0;
	}
	
	public static boolean isLinux() {
		return getOSType().indexOf("nux") >= 0;
	}
	
	public static boolean isMac() {
		String type = getOSType();
		return type.indexOf("mac") >= 0 || type.indexOf("darwin") >= 0;
	}
}
