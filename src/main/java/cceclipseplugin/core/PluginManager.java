package cceclipseplugin.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashMap;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import cceclipseplugin.Activator;
import cceclipseplugin.constants.StringConstants;
import cceclipseplugin.editor.DocumentManager;
import cceclipseplugin.editor.listeners.EditorChangeListener;
import cceclipseplugin.preferences.PreferenceConstants;
import cceclipseplugin.ui.DialogInvalidResponseHandler;
import cceclipseplugin.ui.DialogRequestSendErrorHandler;
import cceclipseplugin.ui.dialogs.WelcomeDialog;
import dataMgmt.DataManager;
import dataMgmt.MetadataManager;
import dataMgmt.SessionStorage;
import requestMgmt.RequestManager;
import constants.CoreStringConstants;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import websocket.ConnectException;
import websocket.WSConnection;
import websocket.WSManager;
import websocket.models.ConnectionConfig;
import websocket.models.Notification;
import websocket.models.Permission;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.notifications.FileCreateNotification;
import websocket.models.notifications.FileMoveNotification;
import websocket.models.notifications.FileRenameNotification;
import websocket.models.notifications.ProjectGrantPermissionsNotification;
import websocket.models.notifications.ProjectRenameNotification;
import websocket.models.notifications.ProjectRevokePermissionsNotification;
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
		
		 wsManager.registerEventHandler(WSConnection.EventType.ON_CONNECT, () -> {
			IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
			String username = prefStore.getString(PreferenceConstants.USERNAME);
			String password = prefStore.getString(PreferenceConstants.PASSWORD);
			if (username.equals(dataManager.getSessionStorage().getUsername())) {
				System.out.println("Already logged in skipping login");
				return;
			}
			boolean showWelcomeDialog = (username == null || username.equals("") || password == null || password.equals(""));
			if (showWelcomeDialog) {
				Display.getDefault().asyncExec(() -> new WelcomeDialog(new Shell(), prefStore).open());
			} else {
				if (prefStore.getBoolean(PreferenceConstants.AUTO_CONNECT)) {
					new Thread(() -> {
						PluginManager.getInstance().getRequestManager().loginAndSubscribe(username, password);
					}).start();
				}
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
			requestManager.fetchProjects();
//			long resId = notification.getResourceID();
//			ProjectGrantPermissionsNotification n = ((ProjectGrantPermissionsNotification) notification.getData());
//			Project project = storage.getProjectById(resId);
//			Permission permy = new Permission(n.grantUsername, n.permissionLevel, null, null);
//			if (project == null) {
//				
//			}
//			if (project.getPermissions() == null) {
//				project.setPermissions(new HashMap<>());
//			}
//			project.getPermissions().put(permy.getUsername(), permy);
//			storage.setProject(project);
		});
		// Project.RevokePermissions
		wsManager.registerNotificationHandler("Project", "RevokePermissions", (notification) -> {
			long resId = notification.getResourceID();
			ProjectRevokePermissionsNotification n = ((ProjectRevokePermissionsNotification) notification.getData());
			Project project = storage.getProjectById(resId);
			if (project.getPermissions() == null) {
				project.setPermissions(new HashMap<>());
			}
			project.getPermissions().remove(n.revokeUsername);
			storage.setProject(project);
		});
		// Project.Delete
		wsManager.registerNotificationHandler("Project", "Delete", (notification) -> {
			long resId = notification.getResourceID();
			storage.removeProjectById(resId);
		});
		
		// ~~~ file hooks ~~~
		// File.Create
		wsManager.registerNotificationHandler("File", "Create", (notification) -> {
			MetadataManager mm = dataManager.getMetadataManager();
			long resId = notification.getResourceID();
			ProjectMetadata pmeta = mm.getProjectMetadata(resId);
			if (pmeta == null) {
				System.out.println("Received File.Create notification for unsubscribed project.");
				return;
			}
			FileCreateNotification n = ((FileCreateNotification) notification.getData());
			FileMetadata meta = mm.getFileMetadata(n.file.getFileID());
			if (meta != null) {
				return;
			}
			meta = new FileMetadata();
			meta.setFileID(n.file.getFileID());
			meta.setFilename(n.file.getFilename());
			meta.setRelativePath(n.file.getRelativePath());
			meta.setVersion(n.file.getFileVersion());
			meta.setCreator(n.file.getCreator());
			meta.setCreationDate(n.file.getCreationDate());
			
			StringBuilder pathBuilder = new StringBuilder();
			pathBuilder.append(mm.getProjectLocation(resId));
			pathBuilder.append(n.file.getRelativePath());
			pathBuilder.append(n.file.getFilename());
			
			File file = new File(pathBuilder.toString());
			if (file.exists()) {
				file.delete();
			}
			try {
				file.createNewFile();
				mm.putFileMetadata(pathBuilder.toString(), resId, meta);
				// TODO: pull file contents
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		// File.Rename
		wsManager.registerNotificationHandler("File", "Rename", (notification) -> {
			long resId = notification.getResourceID();
			MetadataManager mm = dataManager.getMetadataManager();
			FileMetadata meta = mm.getFileMetadata(resId);
			if (meta == null) {
				System.out.println("Received File.Rename notification for unsubscribed project.");
				return;
			}
			FileRenameNotification n = ((FileRenameNotification) notification.getData());
			String projectLocation = mm.getProjectLocation(mm.getProjectIDForFileID(resId));
			
			StringBuilder pathBuilder = new StringBuilder();
			pathBuilder.append(projectLocation);
			pathBuilder.append(meta.getRelativePath());
			pathBuilder.append(meta.getFilename());
			
			StringBuilder newPathBuilder = new StringBuilder();
			newPathBuilder.append(projectLocation);
			newPathBuilder.append(meta.getRelativePath());
			newPathBuilder.append(n.newName);
			
			File file = new File(pathBuilder.toString());
			if (file.exists()) {
				file.renameTo(new File(newPathBuilder.toString()));
				meta.setFilename(n.newName);
			} else {
				System.out.println("Tried to rename file that does not exist: " + pathBuilder.toString());
				return;
			}
		});
		// File.Move
		wsManager.registerNotificationHandler("File", "Move", (notification) -> {
			long resId = notification.getResourceID();
			MetadataManager mm = dataManager.getMetadataManager();
			FileMetadata meta = mm.getFileMetadata(resId);
			if (meta == null) {
				System.out.println("Received File.Move notification for unsubscribed project.");
				return;
			}
			FileMoveNotification n = ((FileMoveNotification) notification.getData());
			String projectLocation = mm.getProjectLocation(mm.getProjectIDForFileID(resId));
			
			StringBuilder pathBuilder = new StringBuilder();
			pathBuilder.append(projectLocation);
			pathBuilder.append(meta.getRelativePath());
			pathBuilder.append(meta.getFilename());
			
			StringBuilder newPathBuilder = new StringBuilder();
			newPathBuilder.append(projectLocation);
			newPathBuilder.append(n.newPath);
			newPathBuilder.append(meta.getFilename());
			
			File file = new File(pathBuilder.toString());
			if (file.exists()) {
				file.renameTo(new File(newPathBuilder.toString()));
				meta.setRelativePath(n.newPath);
			} else {
				System.out.println("Tried to move file that does not exist: " + pathBuilder.toString());
				return;
			}
		});
		// File.Delete
		wsManager.registerNotificationHandler("File", "Delete", (notification) -> {
			long resId = notification.getResourceID();
			MetadataManager mm = dataManager.getMetadataManager();
			FileMetadata meta = mm.getFileMetadata(resId);
			if (meta == null) {
				System.out.println("Received File.Delete notification for unsubscribed project or file that does not exist.");
				return;
			}
			String projectLocation = mm.getProjectLocation(mm.getProjectIDForFileID(resId));
			
			StringBuilder pathBuilder = new StringBuilder();
			pathBuilder.append(projectLocation);
			pathBuilder.append(meta.getRelativePath());
			pathBuilder.append(meta.getFilename());
			
			File file = new File(pathBuilder.toString());
			if (file.exists()) {
				file.delete();
				mm.fileDeleted(resId);
			} else {
				System.out.println("Tried to delete file that does not exist: " + pathBuilder.toString());
				return;
			}
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
