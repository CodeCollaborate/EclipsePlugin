package cceclipseplugin.core;

import cceclipseplugin.editor.listeners.EditorChangeListener;
import dataMgmt.MetadataManager;
import websocket.WSManager;
import websocket.models.ConnectionConfig;
import cceclipseplugin.editor.*;

public class PluginManager {
	
	// PLUGIN SETTINGS (will be moved to preferences later)
	final private String WS_ADDRESS = "ws://echo.websocket.org";
	final private boolean RECONNECT = true;
	final private int MAX_RETRY_COUNT = 3;
	
	// PLUGIN MODULES
	private EditorChangeListener editorChangeListener;
	private DocumentManager documentManager;
	private MetadataManager metadataManager;
	private WSManager wsManager;
	//TODO: Add GUI modules and setup listeners in init()
	
	public void init() {
		editorChangeListener = EditorChangeListener.getInstance();
		documentManager = DocumentManager.getInstance();
		metadataManager = new MetadataManager();
		wsManager = new WSManager(new ConnectionConfig(WS_ADDRESS, RECONNECT, MAX_RETRY_COUNT));
	}
	
}
