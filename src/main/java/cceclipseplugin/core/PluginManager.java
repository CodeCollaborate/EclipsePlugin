package cceclipseplugin.core;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.PlatformUI;

import cceclipseplugin.editor.DocumentManager;
import cceclipseplugin.editor.listeners.EditorChangeListener;
import cceclipseplugin.ui.DialogInvalidResponseHandler;
import cceclipseplugin.ui.DialogRequestSendErrorHandler;
import cceclipseplugin.ui.UIManager;
import dataMgmt.DataManager;
import dataMgmt.MetadataManager;
import requestMgmt.RequestManager;
import websocket.ConnectException;
import websocket.WSManager;
import websocket.models.ConnectionConfig;
import websocket.models.Notification;

/**
 * Manager for the entire plugin. Should only be instantiated once.
 * 
 * @author Benedict
 *
 */
public class PluginManager {

	private static PluginManager instance;

	// PLUGIN SETTINGS (will be moved to preferences later)
	// final private String WS_ADDRESS = "ws://echo.websocket.org";
	// final private String WS_ADDRESS = "ws://localhost:8000/ws/";
	final private String WS_ADDRESS = "ws://cody.csse.rose-hulman.edu:8000/ws/";
	final private boolean RECONNECT = true;
	final private int MAX_RETRY_COUNT = 3;

	// PLUGIN MODULES
	private EditorChangeListener editorChangeListener;
	private final DocumentManager documentManager;
	private final MetadataManager metadataManager;
	private final DataManager dataManager;
	private final WSManager wsManager;
	private final UIManager uiManager;
	private final RequestManager requestManager;

	/**
	 * Get the active instance of the PluginManager class.
	 * 
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
		documentManager = new DocumentManager();
		metadataManager = new MetadataManager();
		dataManager = DataManager.getInstance();
		wsManager = new WSManager(new ConnectionConfig(WS_ADDRESS, RECONNECT, MAX_RETRY_COUNT));
		uiManager = new UIManager();
		requestManager = new RequestManager(dataManager, wsManager, new DialogRequestSendErrorHandler(), new DialogInvalidResponseHandler());

		new Thread(() -> {
			try {
				wsManager.connect();
			} catch (ConnectException e) {
				e.printStackTrace();
			}
		}).start();

		registerNotificationHooks();

		// Start editor & document listeners
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				editorChangeListener = new EditorChangeListener();
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService()
						.addPartListener(editorChangeListener);
			}
		});
		requestManager.fetchPermissionConstants();
		
		System.out.println("PROJECT_METADATA: " + metadataManager
				.getProjectMetadata(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/"));
	}
	
	public RequestManager getRequestManager() {
		return requestManager;
	}

	public WSManager getWSManager() {
		return wsManager;
	}
	
	public UIManager getUIManager() {
		return uiManager;
	}

	public DocumentManager getDocumentManager() {
		return documentManager;
	}

	public MetadataManager getMetadataManager() {
		return metadataManager;
	}

	public DataManager getDataManager() {
		return dataManager;
	}

	private void registerNotificationHooks() {
		wsManager.registerNotificationHandler("File", "Change",
				(Notification n) -> documentManager.handleNotification(n));
	}

}
