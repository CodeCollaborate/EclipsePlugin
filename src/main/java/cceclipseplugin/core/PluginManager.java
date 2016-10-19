package cceclipseplugin.core;

import java.nio.file.Paths;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.PlatformUI;

import cceclipseplugin.constants.StringConstants;
import cceclipseplugin.editor.DocumentManager;
import cceclipseplugin.editor.listeners.EditorChangeListener;
import cceclipseplugin.ui.DialogInvalidResponseHandler;
import cceclipseplugin.ui.DialogRequestSendErrorHandler;
import dataMgmt.DataManager;
import dataMgmt.MetadataManager;
import dataMgmt.SessionStorage;
import requestMgmt.RequestManager;
import constants.CoreStringConstants;
import dataMgmt.models.ProjectMetadata;
import websocket.ConnectException;
import websocket.WSConnection;
import websocket.WSManager;
import websocket.models.ConnectionConfig;
import websocket.models.Notification;
import websocket.models.Permission;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.notifications.ProjectGrantPermissionsNotification;
import websocket.models.notifications.ProjectRenameNotification;
import websocket.models.notifications.ProjectRevokePermissionsNotification;
import websocket.models.notifications.ProjectSubscribeNotification;
import websocket.models.notifications.ProjectUnsubscribeNotification;
import websocket.models.requests.ProjectSubscribeRequest;
import websocket.models.requests.UserLoginRequest;
import websocket.models.requests.UserRegisterRequest;
import websocket.models.responses.UserLoginResponse;

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

		IWorkspaceRoot workspaceRoot = ResourcesPlugin.getWorkspace().getRoot();
		IProject[] projects = workspaceRoot.getProjects();
		MetadataManager metadataManager = dataManager.getMetadataManager();
		for (int i = 0; i < projects.length; i++) {
			IProject project = projects[i];
			if (project.isOpen()) {
				if (project.getName().equalsIgnoreCase("RemoteSystemsTempFiles")) {
					continue;
				}
				String projRoot = project.getLocation().toString();
				try {
					metadataManager.readProjectMetadataFromFile(projRoot, CoreStringConstants.CONFIG_FILE_NAME);
					System.out.println("Loaded metadata from "
							+ Paths.get(projRoot, CoreStringConstants.CONFIG_FILE_NAME).toString());
				} catch (IllegalArgumentException e) {
					System.out.println("No such config file: "
							+ Paths.get(projRoot, CoreStringConstants.CONFIG_FILE_NAME).toString());
				} catch (IllegalStateException e) {
					System.out.println("Incorrect config file format: "
							+ Paths.get(projRoot, CoreStringConstants.CONFIG_FILE_NAME).toString());
				}
			}
		}

		System.out.println("Enumerated all files");
		
		 wsManager.registerEventHandler(WSConnection.EventType.ON_CONNECT, ()
			 -> {
				 try {
					 websocketLogin();
				 } catch (ConnectException | InterruptedException e) {
					 throw new IllegalStateException("Failed to run login", e);
				 }
			 });
			
			 new Thread(() -> {
				 try {
					 wsManager.connect();
				 } catch (ConnectException e) {
					 e.printStackTrace();
				 }
		 }).start();
		
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
			// check if exists
			// create file on disk w/ filePullRequest
			// create file metadata from file in notification and metadatamanager.putFileMetadata()
		});
		// File.Rename
		wsManager.registerNotificationHandler("File", "Rename", (notification) -> {
			// TODO: add metadata calls
			// check if has name
			// rename file on disk
			// rename file's metadata
		});
		// File.Move
		wsManager.registerNotificationHandler("File", "Move", (notification) -> {
			// TODO: add metadata calls
			// edit on disk
			// edit metadata
		});
		// File.Delete
		wsManager.registerNotificationHandler("File", "Delete", (notification) -> {
			// TODO: add metadata calls
			// delete on disk, if exists
			// delete metadata
		});
		// File.Change
		wsManager.registerNotificationHandler("File", "Change",
				(Notification n) -> documentManager.handleNotification(n));
	}

	private void websocketLogin() throws ConnectException, InterruptedException {
		Request req1 = new UserRegisterRequest(StringConstants.PREFERENCES_USERNAME,
				StringConstants.PREFERENCES_FIRSTNAME, StringConstants.PREFERENCES_LASTNAME,
				StringConstants.PREFERENCES_EMAIL, StringConstants.PREFERENCES_PASSWORD).getRequest(resp -> {

					Request req2 = new UserLoginRequest(StringConstants.PREFERENCES_USERNAME,
							StringConstants.PREFERENCES_PASSWORD).getRequest(response -> {
								// TODO(wongb) Add login logic for server
								if (response.getStatus() != 200) {
									throw new IllegalStateException("Failed to log in");
								}

								getWSManager().setAuthInfo(StringConstants.PREFERENCES_USERNAME,
										((UserLoginResponse) response.getData()).getToken());

								try {
									// Subscribe to all projects that are CCProjects
									for(ProjectMetadata metadata : dataManager.getMetadataManager().getAllProjects()){
									wsManager.sendRequest(new ProjectSubscribeRequest(metadata.getProjectID())
											.getRequest(null, null));
									}
								} catch (ConnectException e) {
									e.printStackTrace();
								}
							}, null);
					try {
						getWSManager().sendRequest(req2, -1);
					} catch (ConnectException e) {
						throw new IllegalStateException("Failed to connect while attempting to log in", e);
					}
				}, null);
		getWSManager().sendRequest(req1, -1);
	}
}
