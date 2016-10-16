package cceclipseplugin.core;

import org.eclipse.ui.PlatformUI;

import cceclipseplugin.editor.DocumentManager;
import cceclipseplugin.editor.listeners.EditorChangeListener;
import cceclipseplugin.ui.DialogInvalidResponseHandler;
import cceclipseplugin.ui.DialogRequestSendErrorHandler;
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
	final private String WS_ADDRESS = "ws://cody.csse.rose-hulman.edu:8000/ws/";
	final private boolean RECONNECT = true;
	final private int MAX_RETRY_COUNT = 3;

	// PLUGIN MODULES
	private EditorChangeListener editorChangeListener;
	private final DocumentManager documentManager;
	private final DataManager dataManager;
	private final WSManager wsManager;
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
		dataManager = DataManager.getInstance();
		wsManager = new WSManager(new ConnectionConfig(WS_ADDRESS, RECONNECT, MAX_RETRY_COUNT));
		requestManager = new RequestManager(dataManager, wsManager, new DialogRequestSendErrorHandler(), new DialogInvalidResponseHandler());

		// TODO: Not connect until login requested from user
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
	}
	
	public RequestManager getRequestManager() {
		return requestManager;
	}

	public WSManager getWSManager() {
		return wsManager;
	}

	public DocumentManager getDocumentManager() {
		return documentManager;
	}

	public MetadataManager getMetadataManager() {
		return dataManager.getMetadataManager();
	}

	public DataManager getDataManager() {
		return dataManager;
	}

	private void registerNotificationHooks() {
		wsManager.registerNotificationHandler("File", "Change",
				(Notification n) -> documentManager.handleNotification(n));
	}

}
