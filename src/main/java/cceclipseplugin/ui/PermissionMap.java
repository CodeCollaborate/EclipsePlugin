package cceclipseplugin.ui;

import java.util.HashMap;

/**
 * Hard-coded permission map. Only to be used until the server is capable of
 * sending the defined permission map.
 * 
 * @author loganga
 *
 */
public class PermissionMap {
	public static final HashMap<Integer, String> permissions;

	static {
		permissions = new HashMap<>();
		permissions.put(0, "Blocked");
		permissions.put(1, "Read");
		permissions.put(3, "Write");
		permissions.put(5, "Admin");
		permissions.put(10, "Owner");

	}

}
