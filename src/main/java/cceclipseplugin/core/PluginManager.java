package cceclipseplugin.core;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.equinox.security.storage.ISecurePreferences;
import org.eclipse.equinox.security.storage.SecurePreferencesFactory;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import cceclipseplugin.Activator;
import cceclipseplugin.editor.DocumentManager;
import cceclipseplugin.editor.listeners.EditorChangeListener;
import cceclipseplugin.editor.listeners.DirectoryListener;
import cceclipseplugin.preferences.PreferenceConstants;
import cceclipseplugin.ui.DialogInvalidResponseHandler;
import cceclipseplugin.ui.DialogRequestSendErrorHandler;
import cceclipseplugin.ui.UIRequestErrorHandler;
import cceclipseplugin.ui.dialogs.DialogStrings;
import cceclipseplugin.ui.dialogs.MessageDialog;
import cceclipseplugin.ui.dialogs.OkCancelDialog;
import cceclipseplugin.ui.dialogs.WelcomeDialog;
import constants.CoreStringConstants;
import dataMgmt.DataManager;
import dataMgmt.MetadataManager;
import dataMgmt.SessionStorage;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import websocket.ConnectException;
import websocket.WSConnection;
import websocket.WSManager;
import websocket.models.ConnectionConfig;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.notifications.FileCreateNotification;
import websocket.models.notifications.FileDeleteNotification;
import websocket.models.notifications.FileMoveNotification;
import websocket.models.notifications.FileRenameNotification;
import websocket.models.notifications.ProjectRenameNotification;
import websocket.models.notifications.ProjectRevokePermissionsNotification;
import websocket.models.requests.FileCreateRequest;
import websocket.models.requests.ProjectLookupRequest;
import websocket.models.responses.FileCreateResponse;
import websocket.models.responses.ProjectLookupResponse;

/**
 * Manager for the entire plugin. Should only be instantiated once.
 *
 * @author Benedict
 *
 */
public class PluginManager {

	private static PluginManager instance;

	// PLUGIN SETTINGS
	// TODO: move to configuration file
	final private String WS_ADDRESS = "ws://cody.csse.rose-hulman.edu:8000/ws/";
	final private boolean RECONNECT = true;
	final private int MAX_RETRY_COUNT = 3;

	// LISTENERS
	private EditorChangeListener editorChangeListener;
	private DirectoryListener postChangeDirListener;
	
	// may have to switch to using a queue if we run into issues with the order notifications are received
	public HashMap<String, List<Class<?>>> fileDirectoryWatchWarnList = new HashMap<>();
	public HashMap<String, List<Class<?>>> projectDirectoryWatchWarnList = new HashMap<>();
	
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
		wsManager = new WSManager(new ConnectionConfig(WS_ADDRESS, RECONNECT, MAX_RETRY_COUNT));
		dataManager = DataManager.getInstance();
		dataManager.getPatchManager().setWsMgr(wsManager);
		dataManager.getPatchManager().setNotifHandler(documentManager);
		requestManager = new EclipseRequestManager(dataManager, wsManager, new DialogRequestSendErrorHandler(),
				new DialogInvalidResponseHandler());

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
		writeSubscribedProjects();
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
			ISecurePreferences secureStore = SecurePreferencesFactory.getDefault();
			final String[] username = {null};
			final String[] password = {null};
			try {
				username[0] = secureStore.get(PreferenceConstants.USERNAME, null);
				password[0] = secureStore.get(PreferenceConstants.PASSWORD, null);
			} catch (StorageException e) {
				e.printStackTrace();
			}
			IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();

			if (username[0] == null || username[0].equals("") || password[0] == null || password[0].equals("")) {
				Display.getDefault().asyncExec(() -> {
					Shell shell = Display.getDefault().getActiveShell();
					new WelcomeDialog(shell, secureStore).open();
				});
			} else {
				if (prefStore.getBoolean(PreferenceConstants.AUTO_CONNECT)) {
					new Thread(() -> {
						requestManager.login(username[0], password[0]);
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
			if (storage.getUsername().equals(n.revokeUsername)) {
				storage.removeProjectById(resId);
				getMetadataManager().projectDeleted(resId);
			} else {
				if (project != null) {
					if (project.getPermissions() == null) {
						project.setPermissions(new HashMap<>());
					}
					project.getPermissions().remove(n.revokeUsername);
				}
				storage.setProject(project);
			}
		});
		// Project.Delete
		wsManager.registerNotificationHandler("Project", "Delete", (notification) -> {
			long resId = notification.getResourceID();
			storage.removeProjectById(resId);
			ProjectMetadata meta = getMetadataManager().getProjectMetadata(resId);
			if (meta == null) {
				System.out.println("Received Project.Delete notification for non-existent project.");
				return;
			}
			IProject iproject = ResourcesPlugin.getWorkspace().getRoot()
					.getProject(getMetadataManager().getProjectMetadata(resId).getName());
			IFile metaFile = iproject.getFile(CoreStringConstants.CONFIG_FILE_NAME);
			getMetadataManager().projectDeleted(resId);
			if (metaFile.exists()) {
				try {
					metaFile.delete(true, true, new NullProgressMonitor());
				} catch (CoreException e) {
					e.printStackTrace();
				}
			}
		});

		// ~~~ file hooks ~~~
		// File.Create
		wsManager.registerNotificationHandler("File", "Create", (notification) -> {
			MetadataManager mm = dataManager.getMetadataManager();
			long resId = notification.getResourceID();
			ProjectMetadata pmeta = mm.getProjectMetadata(resId);
			if (pmeta == null) {
				System.out.println("Received File.Create notification for project with no metadata.");
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

			meta = new FileMetadata(n.file);
			List<FileMetadata> fmetas = pmeta.getFiles();
			fmetas.add(meta);
			pmeta.setFiles(fmetas);
			mm.putProjectMetadata(eclipseProject.getLocation().toString(), pmeta);
			mm.writeProjectMetadataToFile(pmeta, eclipseProject.getLocation().toString(), CoreStringConstants.CONFIG_FILE_NAME);
			String path = Paths.get(n.file.getRelativePath(), n.file.getFilename()).toString();
			putFileInWarnList(path, n.getClass());
			requestManager.pullFileAndCreate(eclipseProject, p, n.file, monitor, true);
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
			Project project = dataManager.getSessionStorage().getProjectById(mm.getProjectIDForFileID(resId));
			
			// old file
			String pathToFile = Paths.get(meta.getFilePath(), meta.getFilename()).normalize().toString();
			IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName());
			IFile file = p.getFile(pathToFile);
			
			// new file
			String newPathToFile = Paths.get(meta.getFilePath(), n.newName).normalize().toString();
			
			if (renameFile(file, new Path(newPathToFile), project.getProjectID())) {
				meta.setFilename(n.newName);
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
			Project project = dataManager.getSessionStorage().getProjectById(mm.getProjectIDForFileID(resId));
			
			// old file
			String pathToFile = Paths.get(meta.getFilePath(), meta.getFilename()).normalize().toString();
			IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName());
			IFile file = p.getFile(pathToFile);
			
			// new file
			String newPathToFile = Paths.get(n.newPath, meta.getFilename()).normalize().toString();
			
			if (renameFile(file, new Path(newPathToFile), project.getProjectID())) {
				meta.setRelativePath(n.newPath);
			}
		});
		// File.Delete
		wsManager.registerNotificationHandler("File", "Delete", (notification) -> {
			long resId = notification.getResourceID();
			MetadataManager mm = dataManager.getMetadataManager();
			FileMetadata meta = mm.getFileMetadata(resId);
			if (meta == null) {
				System.out.println(
						"Received File.Delete notification for unsubscribed project or file that does not exist.");
				return;
			}
			Project project = dataManager.getSessionStorage().getProjectById(mm.getProjectIDForFileID(resId));
			String pathToFile = Paths.get(meta.getFilePath(), meta.getFilename()).normalize().toString();
			IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(project.getName());
			IFile file = p.getFile(pathToFile);
			
			if (file.exists()) {
				if (documentManager.getEditor(pathToFile) != null) {
					Display.getDefault().asyncExec(() -> {
						String message = String.format(DialogStrings.DeleteWarningDialog_Message, file.getName());
						OkCancelDialog dialog = OkCancelDialog.createDialog(message,
								"Restore", IDialogConstants.OK_LABEL, true);
						// Restore is the "ok" option, because it should not be the default option
						if (dialog.open() == Window.OK) {
							tryRestoreFile(resId, file, meta, project.getProjectID());
						}
					});
					return;
				}
				try {
					putFileInWarnList(pathToFile, FileDeleteNotification.class);
					file.delete(true, new NullProgressMonitor());
					mm.fileDeleted(resId);
				} catch (CoreException e) {
					e.printStackTrace();
					removeFileFromWarnList(pathToFile, FileDeleteNotification.class);
					showErrorAndUnsubscribe(project.getProjectID());
				}
			} else {
				System.out.println("Tried to delete file that does not exist: " + pathToFile);
			}
		});
		// File.Change
		wsManager.registerNotificationHandler("File", "Change", dataManager.getPatchManager());
	}
		
	private boolean renameFile(IFile file, IPath newPath, long projId) {
		if (file.exists()) {
			try {
				NullProgressMonitor monitor = new NullProgressMonitor();
				putFileInWarnList(newPath.toString(), FileRenameNotification.class);
				file.move(newPath, true, monitor);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
				removeFileFromWarnList(newPath.toString(), FileRenameNotification.class);
				showErrorAndUnsubscribe(projId);
			}
		} else {
			System.out.println("Tried to rename file that does not exist: " + file.getFullPath().toString());
		}
		return false;
	}
	
	private void tryRestoreFile(long oldId, IFile file, FileMetadata meta, long projectId) {		
		// get file bytes
		byte[] fileBytes;
		try {
			fileBytes = EclipseRequestManager.inputStreamToByteArray(file.getContents());
		} catch (IOException | CoreException e) {
			e.printStackTrace();
			showErrorAndUnsubscribe(projectId);
			return;
		}
		
		MetadataManager mm = getMetadataManager();
		mm.fileDeleted(oldId);
		
		// send file create request
		Request request = new FileCreateRequest(file.getName(), meta.getFilePath(), projectId, fileBytes).getRequest(response -> {
			if (response.getStatus() == 200) {
				FileCreateResponse resp = (FileCreateResponse) response.getData();
				long fileId = resp.getFileID();
				meta.setFileID(fileId);
				meta.setFilename(meta.getFilename());
				meta.setRelativePath(meta.getRelativePath());
				meta.setVersion(0);
				mm.putFileMetadata(meta.getFilePath(), projectId, meta);
			} else {
				showErrorAndUnsubscribe(projectId);
			}
		}, () -> {
			showErrorAndUnsubscribe(projectId);
		});
		this.wsManager.sendAuthenticatedRequest(request);
	}
	
	private void showErrorAndUnsubscribe(long projectId) {
		Display.getDefault().asyncExec(() -> {
			PluginManager pm = PluginManager.getInstance();
			String projName = pm.getDataManager().getSessionStorage().getProjectById(projectId).getName();
			getRequestManager().unsubscribeFromProject(projectId);
			MessageDialog.createDialog("An error occured. Please re-subscribe to the project " + projName).open();
		});
	}
	
	public void registerResourceListeners() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		postChangeDirListener = new DirectoryListener();
		workspace.addResourceChangeListener(postChangeDirListener, IResourceChangeEvent.POST_BUILD);
	}
	
	public void deregisterResourceListeners() {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		workspace.removeResourceChangeListener(postChangeDirListener);
	}

	private void initPropertyListeners() {
		dataManager.getSessionStorage().addPropertyChangeListener((event) -> {
			if (event.getPropertyName().equals(SessionStorage.USERNAME)) {
				requestManager.fetchProjects();
				Display.getDefault().asyncExec(() -> {
					if (event.getOldValue() == null || !event.getOldValue().equals(event.getNewValue())) {
						if (Window.OK == OkCancelDialog
								.createDialog("Do you want to auto-subscribe to subscribed projets from the last session?\n"
											+ "This will overwrite any local changes made since the last online session.")
								.open()) {
							autoSubscribeForSession = true;
							autoSubscribe();
						} else {
							autoSubscribeForSession = false;
							removeAllSubscribedPrefs(false);
						}
						System.out.println("Auto-subscribe for session set to " + autoSubscribeForSession);
					}
				});
			} else if (event.getPropertyName().equals(SessionStorage.PROJECT_LIST)) {
				if (autoSubscribeForSession) {
					autoSubscribe();
				}
			}
		});
	}

	private void autoSubscribe() {
		SessionStorage storage = dataManager.getSessionStorage();
		List<Long> subscribedIdsFromPrefs = getSubscribedProjectIds();
		Set<Long> subscribedIds = storage.getSubscribedIds();

		for (Long id : subscribedIdsFromPrefs) {
			Project p = storage.getProjectById(id);
			if (p == null) {
				removeProjectIdFromPrefs(id);
			} else if (!subscribedIds.contains(id)) {
				requestManager.subscribeToProject(id);
			}
		}
	}

	/**
	 * Removes the "auto-subscribe" preference
	 * associated with the given projectID. Should be called when either the
	 * project is no longer on the server or the user no longer has permissions
	 * for a project.
	 *
	 * @param id
	 */
	public void removeProjectIdFromPrefs(long id) {
		System.out.println("Removing project " + id + "from auto-subscribe prefs");
		Preferences pluginPrefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
		try {
			String sid = Long.toString(id);
			if (projectPrefs.nodeExists(sid)) {
				Preferences thisProjectPrefs = projectPrefs.node(sid);
				thisProjectPrefs.removeNode();
				System.out.println("Node removed for " + sid);
			}
		} catch (BackingStoreException e) {
			MessageDialog.createDialog("Could not remove project from subscribe preferences.").open();
			e.printStackTrace();
		}
	}

	/**
	 * Returns a list of the project IDs that the user was subscribed to
	 * from their last session.
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
			System.out.println("Found " + projectIDs.length + " auto-subscribe preferences");
			for (int i = 0; i < projectIDs.length; i++) {
				System.out.println("Read subscribe pref for project " + projectIDs[i]);
				subscribedProjectIds.add(Long.parseLong(projectIDs[i]));
			}
		} catch (BackingStoreException e) {
			MessageDialog.createDialog("Could not read subscribed projects from preferences.").open();
			e.printStackTrace();
		}
		return subscribedProjectIds;
	}

	/**
	 * Removes all project ID nodes from the subscribe preferences.
	 *
	 * @param b
	 */
	public void removeAllSubscribedPrefs(boolean b) {
		Preferences pluginPrefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
		String[] projectIDs;
		try {
			projectIDs = projectPrefs.childrenNames();
			for (int i = 0; i < projectIDs.length; i++) {
				Preferences thisProjectPrefs = projectPrefs.node(projectIDs[i]);
				thisProjectPrefs.removeNode();
			}
			pluginPrefs.flush();
		} catch (BackingStoreException e) {
			MessageDialog.createDialog("Could not write subscribe preferences.").open();
			e.printStackTrace();
		}
	}

	/**
	 * Goes through all of the projects in session storage, determines if the
	 * user is currently subscribed, and then creates a node for that project in
	 * the subscribe preferences. If a node is found and the user is not
	 * subscribed, it is removed.
	 */
	public void writeSubscribedProjects() {
		System.out.println("Writing subscribed projects to auto-subscribe preferences...");
		SessionStorage ss = PluginManager.getInstance().getDataManager().getSessionStorage();
		Set<Long> subscribedIDs = ss.getSubscribedIds();
		List<Project> projects = ss.getProjects();
		Preferences pluginPrefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);

		for (Project p : projects) {
			boolean subscribed = subscribedIDs.contains(p.getProjectID());

			if (!subscribed) {
				// remove it from nodes if not subscribed
				try {
					if (projectPrefs.nodeExists(p.getProjectID() + "")) {
						Preferences thisProjectPrefs = projectPrefs.node(p.getProjectID() + "");
						thisProjectPrefs.removeNode();
						System.out.println("Node removed for " + p.getProjectID());
					}
				} catch (BackingStoreException e) {
					e.printStackTrace();
				}
			} else {
				// otherwise, make node
				Preferences thisProjectPrefs = projectPrefs.node(p.getProjectID() + "");
				// have to put something in it, otherwise the node will be
				// dumped
				thisProjectPrefs.putBoolean(PreferenceConstants.VAR_SUBSCRIBED, true);
				System.out.println("Wrote subscribed pref for project " + p.getProjectID());
			}
		}
		try {
			pluginPrefs.flush();
		} catch (BackingStoreException e) {
			System.out.println("Could not write subscribe preferences.");
			e.printStackTrace();
		}
	}
	
	
	public boolean isFileInWarnList(String fullFilePath, Class<?> notificationType) {
		if (fileDirectoryWatchWarnList.containsKey(fullFilePath)) {
			return fileDirectoryWatchWarnList.get(fullFilePath).contains(notificationType);
		}
		return false;
	}
	
	public void putFileInWarnList(String fullFilePath, Class<?> notificationType) {
		if (fileDirectoryWatchWarnList.containsKey(fullFilePath)) {
			List<Class<?>> notificationTypes = fileDirectoryWatchWarnList.get(fullFilePath);
			notificationTypes.add(notificationType);
			fileDirectoryWatchWarnList.put(fullFilePath, notificationTypes);
		} else {
			List<Class<?>> notificationTypes = new ArrayList<>();
			notificationTypes.add(notificationType);
			fileDirectoryWatchWarnList.put(fullFilePath, notificationTypes);
		} 
	}
	
	public void removeFileFromWarnList(String fullFilePath, Class<?> notificationType) {
		if (fileDirectoryWatchWarnList.containsKey(fullFilePath)) {
			fileDirectoryWatchWarnList.get(fullFilePath).remove(notificationType);
		}
	}
	
	public boolean isProjectInWarnList(String projectName, Class<?> notificationType) {
		if (projectDirectoryWatchWarnList.containsKey(projectName)) {
			return projectDirectoryWatchWarnList.get(projectName).contains(notificationType);
		}
		return false;
	}
	
	public void putProjectInWarnList(String projectName, Class<?> notificationType) {
		if (projectDirectoryWatchWarnList.containsKey(projectName)) {
			List<Class<?>> notificationTypes = projectDirectoryWatchWarnList.get(projectName);
			notificationTypes.add(notificationType);
			projectDirectoryWatchWarnList.put(projectName, notificationTypes);
		} else {
			List<Class<?>> notificationTypes = new ArrayList<>();
			notificationTypes.add(notificationType);
			projectDirectoryWatchWarnList.put(projectName, notificationTypes);
		}
	}
	
	public void removeProjectFromWarnList(String projectName, Class<?> notificationType) {
		if (projectDirectoryWatchWarnList.containsKey(projectName)) {
			projectDirectoryWatchWarnList.get(projectName).remove(notificationType);
		}
	}
}
