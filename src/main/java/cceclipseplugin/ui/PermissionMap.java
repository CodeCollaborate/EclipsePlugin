package cceclipseplugin.ui;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Hard-coded permission map. Only to be used until the server is capable of
 * sending the defined permission map.
 * 
 * @author loganga
 *
 */
public class PermissionMap {
	public static final BiMap<String, Byte> permissions;

	static {
		permissions = HashBiMap.create();
		permissions.put("Blocked", (byte) 0);
		permissions.put("Read", (byte) 1);
		permissions.put("Write", (byte) 3);
		permissions.put("Admin", (byte) 5);
		permissions.put("Owner", (byte) 10);
	}
}
