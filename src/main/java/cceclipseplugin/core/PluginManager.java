package cceclipseplugin.core;

import org.eclipse.ui.PlatformUI;

import cceclipseplugin.editor.DocumentManager;
import cceclipseplugin.editor.listeners.EditorChangeListener;
import cceclipseplugin.ui.DialogInvalidResponseHandler;
import cceclipseplugin.ui.DialogRequestSendErrorHandler;
import dataMgmt.DataManager;
import dataMgmt.MetadataManager;
import dataMgmt.SessionStorage;
import requestMgmt.RequestManager;
import websocket.ConnectException;
import websocket.WSManager;
import websocket.models.ConnectionConfig;
import websocket.models.Notification;
import websocket.models.Permission;
import websocket.models.Project;
import websocket.models.notifications.ProjectGrantPermissionsNotification;
import websocket.models.notifications.ProjectRenameNotification;
import websocket.models.notifications.ProjectRevokePermissionsNotification;
import websocket.models.notifications.ProjectSubscribeNotification;
import websocket.models.notifications.ProjectUnsubscribeNotification;

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
	private final ProjectManager projectManager;

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
		projectManager = new ProjectManager();

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

	public ProjectManager getProjectManager() {
		return projectManager;
	}
	
	private void registerNotificationHooks() {
		SessionStorage storage = dataManager.getSessionStorage();
		// ~~~ project hooks ~~~
		// Project.Rename
		wsManager.registerNotificationHandler("Project", "Rename", (notification) -> {
			long resId = notification.getResourceID();
			String newName = (((ProjectRenameNotification) notification.getData()).newName);
			Project project = storage.getProjectById(resId);
			project.setName(newName);
			storage.setProject(project);
		});
		// Project.GrantPermissions
		wsManager.registerNotificationHandler("Project", "GrantPermissions", (notification) -> {
			long resId = notification.getResourceID();
			ProjectGrantPermissionsNotification n = ((ProjectGrantPermissionsNotification) notification.getData());
			Project project = storage.getProjectById(resId);
			Permission permy = new Permission(n.grantUsername, n.permissionLevel, null, null);
			project.getPermissions().put(permy.getUsername(), permy);
			storage.setProject(project);
		});
		// Project.RevokePermissions
		wsManager.registerNotificationHandler("Project", "RevokePermissions", (notification) -> {
			long resId = notification.getResourceID();
			ProjectRevokePermissionsNotification n = ((ProjectRevokePermissionsNotification) notification.getData());
			Project project = storage.getProjectById(resId);
			project.getPermissions().remove(n.revokeUsername);
			storage.setProject(project);
		});
		// Project.Subscribe
		wsManager.registerNotificationHandler("Project", "Subscribe", (notification) -> {
			long resId = notification.getResourceID();
			ProjectSubscribeNotification n = ((ProjectSubscribeNotification) notification.getData());
			// TODO: add pull files & set subscribed in prefs
		});
		// Project.Unsubscribe
		wsManager.registerNotificationHandler("Project", "Subscribe", (notification) -> {
			long resId = notification.getResourceID();
			ProjectUnsubscribeNotification n = ((ProjectUnsubscribeNotification) notification.getData());
			// TODO: add set subscribed in prefs
		});
		// Project.Delete
		wsManager.registerNotificationHandler("Project", "Delete", (notification) -> {
			long resId = notification.getResourceID();
			storage.removeProjectById(resId);
		});
		
		// ~~~ file hooks ~~~
		// File.Create
		wsManager.registerNotificationHandler("File", "Create", (notification) -> {
			// TODO: add metadata calls
		});
		// File.Rename
		wsManager.registerNotificationHandler("File", "Rename", (notification) -> {
			// TODO: add metadata calls
		});
		// File.Move
		wsManager.registerNotificationHandler("File", "Move", (notification) -> {
			// TODO: add metadata calls
		});
		// File.Delete
		wsManager.registerNotificationHandler("File", "Delete", (notification) -> {
			// TODO: add metadata calls
		});
		// File.Change
		wsManager.registerNotificationHandler("File", "Change",
				(Notification n) -> documentManager.handleNotification(n));
	}

}
