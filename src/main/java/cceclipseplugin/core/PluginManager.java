package cceclipseplugin.core;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import cceclipseplugin.Activator;
import cceclipseplugin.editor.DocumentManager;
import cceclipseplugin.editor.listeners.EditorChangeListener;
import cceclipseplugin.editor.listeners.PostChangeDirectoryListener;
import cceclipseplugin.editor.listeners.PreChangeDirectoryListener;
import cceclipseplugin.editor.listeners.SaveExecutionListener;
import cceclipseplugin.preferences.PreferenceConstants;
import cceclipseplugin.ui.DialogInvalidResponseHandler;
import cceclipseplugin.ui.DialogRequestSendErrorHandler;
import cceclipseplugin.ui.UIRequestErrorHandler;
import cceclipseplugin.ui.dialogs.MessageDialog;
import cceclipseplugin.ui.dialogs.OkCancelDialog;
import cceclipseplugin.ui.dialogs.WelcomeDialog;
import dataMgmt.DataManager;
import dataMgmt.MetadataManager;
import dataMgmt.SessionStorage;
import constants.CoreStringConstants;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import websocket.ConnectException;
import websocket.WSConnection;
import websocket.WSManager;
import websocket.models.ConnectionConfig;
import websocket.models.Notification;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.notifications.FileCreateNotification;
import websocket.models.notifications.FileMoveNotification;
import websocket.models.notifications.FileRenameNotification;
import websocket.models.notifications.ProjectRenameNotification;
import websocket.models.notifications.ProjectRevokePermissionsNotification;
import websocket.models.requests.ProjectLookupRequest;
import websocket.models.responses.ProjectLookupResponse;

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

	// LISTENERS
	private EditorChangeListener editorChangeListener;
	private PreChangeDirectoryListener preChangeDirListener;
	private PostChangeDirectoryListener postChangeDirListener;
	
	// PLUGIN MODULES
	private final DocumentManager documentManager;
	private final DataManager dataManager;
	private final WSManager wsManager;
	private final EclipseRequestManager requestManager;
	
	private boolean autoSubscribeForSession;

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
		requestManager = new EclipseRequestManager(dataManager, wsManager, new DialogRequestSendErrorHandler(), new DialogInvalidResponseHandler());

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
		
		registerWSHooks();
		registerResourceListeners();
		initPropertyListeners();
			
		new Thread(() -> {
			try {
				wsManager.connect();
			} catch (ConnectException e) {
				e.printStackTrace();
			}
		}).start();
		
		requestManager.fetchPermissionConstants();
	}
	
	public void onStop() {
		deregisterResourceListeners();
		wsManager.close();
	}

	public EclipseRequestManager getRequestManager() {
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
	
	private void registerWSHooks() {
		 wsManager.registerEventHandler(WSConnection.EventType.ON_CONNECT, () -> {
			IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
			String username = prefStore.getString(PreferenceConstants.USERNAME);
			String password = prefStore.getString(PreferenceConstants.PASSWORD);
			boolean showWelcomeDialog = (username == null || username.equals("") || password == null || password.equals(""));
			if (showWelcomeDialog) {
				Display.getDefault().asyncExec(() -> new WelcomeDialog(new Shell(), prefStore).open());
			} else {
				if (prefStore.getBoolean(PreferenceConstants.AUTO_CONNECT)) {
					new Thread(() -> {
						requestManager.login(username, password);
					}).start();
				}
			}
		 });
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
			ProjectMetadata meta = getMetadataManager().getProjectMetadata(resId);
			meta.setName(newName);
			storage.setProject(project);
		});
		// Project.GrantPermissions
		wsManager.registerNotificationHandler("Project", "GrantPermissions", (notification) -> {
			long resId = notification.getResourceID();
			ArrayList<Long> projects = new ArrayList<>();
			projects.add(resId);
			Request projectLookupRequest = new ProjectLookupRequest(projects).getRequest(response -> {
	        	ProjectLookupResponse r = (ProjectLookupResponse) response.getData();
	        	if (r.getProjects() == null || r.getProjects().length != 1) {
	        		System.out.println("Couldn't read projects from lookup");
	        	} else {
	        		Project p = r.getProjects()[0];
	    			storage.setProject(p);
	        	}
	        }, new UIRequestErrorHandler("Couldn't send Project Lookup Request."));
	        wsManager.sendAuthenticatedRequest(projectLookupRequest);
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
			if (storage.getUsername().equals(n.revokeUsername)) {
				storage.removeProjectById(resId);
				getMetadataManager().projectDeleted(resId);
			} else {
				storage.setProject(project);
			}
		});
		// Project.Delete
		wsManager.registerNotificationHandler("Project", "Delete", (notification) -> {
			long resId = notification.getResourceID();
			storage.removeProjectById(resId);
			getMetadataManager().projectDeleted(resId);
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
			
			Project p = dataManager.getSessionStorage().getProjectById(pmeta.getProjectID());
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			IProject eclipseProject = root.getProject(p.getName());
			IProgressMonitor monitor = new NullProgressMonitor();
			requestManager.pullFileAndCreate(eclipseProject, p, n.file, monitor);
		});
		// File.Rename
		wsManager.registerNotificationHandler("File", "Rename", (notification) -> {
			long resId = notification.getResourceID();
			MetadataManager mm = dataManager.getMetadataManager();
			FileMetadata meta = mm.getFileMetadata(resId);
			if (meta == null) {
				System.out.println("Received File.Rename notification for non-existent file.");
				return;
			}
			FileRenameNotification n = ((FileRenameNotification) notification.getData());
			String projectLocation = mm.getProjectLocation(mm.getProjectIDForFileID(resId));
			
			// TODO (fahslaj): Finish these blocks of code in integration of editor
//			StringBuilder pathBuilder = new StringBuilder();
//			pathBuilder.append(projectLocation);
//			pathBuilder.append(meta.getRelativePath());
//			pathBuilder.append(meta.getFilename());
//			
//			StringBuilder newPathBuilder = new StringBuilder();
//			newPathBuilder.append(projectLocation);
//			newPathBuilder.append(meta.getRelativePath());
//			newPathBuilder.append(n.newName);
//			
//			File file = new File(pathBuilder.toString());
//			if (file.exists()) {
//				file.renameTo(new File(newPathBuilder.toString()));
//				meta.setFilename(n.newName);
//			} else {
//				System.out.println("Tried to rename file that does not exist: " + pathBuilder.toString());
//				return;
//			}
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
			
			// TODO (fahslaj): Finish these blocks of code in integration of editor
//			StringBuilder pathBuilder = new StringBuilder();
//			pathBuilder.append(projectLocation);
//			pathBuilder.append(meta.getRelativePath());
//			pathBuilder.append(meta.getFilename());
//			
//			StringBuilder newPathBuilder = new StringBuilder();
//			newPathBuilder.append(projectLocation);
//			newPathBuilder.append(n.newPath);
//			newPathBuilder.append(meta.getFilename());
//			
//			File file = new File(pathBuilder.toString());
//			if (file.exists()) {
//				file.renameTo(new File(newPathBuilder.toString()));
//				meta.setRelativePath(n.newPath);
//			} else {
//				System.out.println("Tried to move file that does not exist: " + pathBuilder.toString());
//				return;
//			}
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
			
			// TODO (fahslaj): Finish these blocks of code in integration of editor
//			StringBuilder pathBuilder = new StringBuilder();
//			pathBuilder.append(projectLocation);
//			pathBuilder.append(meta.getRelativePath());
//			pathBuilder.append(meta.getFilename());
//			
//			File file = new File(pathBuilder.toString());
//			if (file.exists()) {
//				file.delete();
//				mm.fileDeleted(resId);
//			} else {
//				System.out.println("Tried to delete file that does not exist: " + pathBuilder.toString());
//				return;
//			}
		});
		// File.Change
		wsManager.registerNotificationHandler("File", "Change",
				(Notification n) -> documentManager.handleNotification(n));
	}
	
	public void registerResourceListeners() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		preChangeDirListener = new PreChangeDirectoryListener();
		postChangeDirListener = new PostChangeDirectoryListener();
		workspace.addResourceChangeListener(preChangeDirListener, IResourceChangeEvent.PRE_BUILD);
		workspace.addResourceChangeListener(postChangeDirListener, IResourceChangeEvent.POST_BUILD);
	}
	
	public void deregisterResourceListeners() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.removeResourceChangeListener(preChangeDirListener);
		workspace.removeResourceChangeListener(postChangeDirListener);
	}
	
	private void initPropertyListeners() {
		dataManager.getSessionStorage().addPropertyChangeListener((event) -> {
			if (event.getPropertyName().equals(SessionStorage.USERNAME)) {
				requestManager.fetchProjects();
				Display.getDefault().asyncExec(() -> {
					if (event.getOldValue() == null || !event.getOldValue().equals(event.getNewValue())) {
						if (Window.OK == OkCancelDialog.createDialog("Do you want to auto-subscribe to subscribed projets from the last session?\n"
								+ "This will overwrite any local changes made since the last online session.").open()) {
							autoSubscribeForSession = true;
						} else {
							autoSubscribeForSession = false;
							setAllSubscribedPrefs(false);
						}
					}
				});
			} else if (event.getPropertyName().equals(SessionStorage.PROJECT_LIST)) {
				SessionStorage storage = dataManager.getSessionStorage();
				List<Long> subscribedIdsFromPrefs = getSubscribedProjectIds();
				Set<Long> subscribedIds = storage.getSubscribedIds();
				if (autoSubscribeForSession) {
					for (Long id : subscribedIdsFromPrefs) {
						Project p = storage.getProjectById(id);
						if (p == null) {
							removeProjectIdFromPrefs(id);
						} else if (!subscribedIds.contains(id)) {
							requestManager.subscribeToProject(id);
						}
					}
				}
			}
		});
	}
	
	/**
	 * Removes the "auto-subscribe" preference associated with the given projectID.
	 * Should be called when either the project is no longer on the server or the
	 * user no longer has permissions for a project.
	 * 
	 * @param id
	 */
	public void removeProjectIdFromPrefs(long id) {
		Preferences pluginPrefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
		String[] projectIDs;
		try {
			projectIDs = projectPrefs.childrenNames();
			for (int i = 0; i < projectIDs.length; i++) {
				if (id == Long.parseLong(projectIDs[i])) {
					Preferences thisProjectPrefs = projectPrefs.node(projectIDs[i]);
					thisProjectPrefs.removeNode();
				}
			}
		} catch (BackingStoreException e) {
			MessageDialog.createDialog("Could not remove project from subscribe preferences.").open();
			e.printStackTrace();
		}
		
	}
	
	/**
	 * Returns a list of the project IDs that the user was subscribed to from their
	 * last session.
	 * 
	 * @return
	 */
	public List<Long> getSubscribedProjectIds() {
		List<Long> subscribedProjectIds = new ArrayList<>();
		Preferences pluginPrefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
		String[] projectIDs;
		try {
			projectIDs = projectPrefs.childrenNames();
			for (int i = 0; i < projectIDs.length; i++) {
				Preferences thisProjectPrefs = projectPrefs.node(projectIDs[i]);
				boolean subscribed = thisProjectPrefs.getBoolean(PreferenceConstants.VAR_SUBSCRIBED, false);
				if (subscribed) {
					subscribedProjectIds.add(Long.parseLong(projectIDs[i]));
				}
			}
		} catch (BackingStoreException e) {
			MessageDialog.createDialog("Could not read subscribed projects from preferences.").open();
			e.printStackTrace();
		}
		return subscribedProjectIds;
	}
	
	/**
	 * Sets the "auto-subscribe" preference for every project to the given parameter.
	 * 
	 * @param b
	 */
	public void setAllSubscribedPrefs(boolean b) {
		Preferences pluginPrefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
		String[] projectIDs;
		try {
			projectIDs = projectPrefs.childrenNames();
			for (int i = 0; i < projectIDs.length; i++) {
				Preferences thisProjectPrefs = projectPrefs.node(projectIDs[i]);
				thisProjectPrefs.putBoolean(PreferenceConstants.VAR_SUBSCRIBED, b);
			}
		} catch (BackingStoreException e) {
			MessageDialog.createDialog("Could not write subscribe preferences.").open();
			e.printStackTrace();
		}
	}
}
