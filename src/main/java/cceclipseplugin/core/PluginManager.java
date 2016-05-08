package cceclipseplugin.core;

import cceclipseplugin.editor.listeners.EditorChangeListener;
import dataMgmt.MetadataManager;
import websocket.WSManager;
import websocket.models.ConnectionConfig;

import org.eclipse.ui.PlatformUI;

import cceclipseplugin.editor.*;

public class PluginManager {
	
	private static PluginManager instance;
	
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


    /**
     * Get the active instance of the PluginManager class.
     * @return the instance of the PluginManager class
     */
    public static PluginManager getInstance() {
        if (instance == null) {
            synchronized (PluginManager.class) {
                if (instance == null) {
                    instance = new PluginManager();
                }
            }
        }
        return instance;
    }
	
	private PluginManager() {
		documentManager = DocumentManager.getInstance();
		metadataManager = new MetadataManager();
		wsManager = new WSManager(new ConnectionConfig(WS_ADDRESS, RECONNECT, MAX_RETRY_COUNT));

		// Start listeners
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				editorChangeListener = EditorChangeListener.getInstance();
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService()
						.addPartListener(editorChangeListener);
			}
		});
	}
	
	
}
