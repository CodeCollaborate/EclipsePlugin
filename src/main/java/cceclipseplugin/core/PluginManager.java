package cceclipseplugin.core;

import java.nio.file.Paths;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.PlatformUI;

import cceclipseplugin.constants.StringConstants;
import cceclipseplugin.editor.DocumentManager;
import cceclipseplugin.editor.listeners.EditorChangeListener;
import constants.CoreStringConstants;
import dataMgmt.DataManager;
import dataMgmt.MetadataManager;
import dataMgmt.models.ProjectMetadata;
import websocket.ConnectException;
import websocket.WSManager;
import websocket.models.ConnectionConfig;
import websocket.models.Notification;
import websocket.models.Request;
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
	// final private String WS_ADDRESS = "ws://echo.websocket.org";
	final private String WS_ADDRESS = "ws://localhost:8000/ws/";
	final private boolean RECONNECT = true;
	final private int MAX_RETRY_COUNT = 3;

	// PLUGIN MODULES
	private EditorChangeListener editorChangeListener;
	private final DocumentManager documentManager;
	private final MetadataManager metadataManager;
	private final DataManager dataManager;
	private final WSManager wsManager;

	// TODO: Add GUI modules and setup listeners in init()

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
		
		// wsManager.registerEventHandler(WSConnection.EventType.ON_CONNECT, ()
		// -> {
		// try {
		// websocketLogin();
		// } catch (ConnectException | InterruptedException e) {
		// throw new IllegalStateException("Failed to run login", e);
		// }
		// });
		//
		// new Thread(() -> {
		// try {
		// wsManager.connect();
		// } catch (ConnectException e) {
		// e.printStackTrace();
		// }
		// }).start();
	}

	public WSManager getWSManager() {
		return wsManager;
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
									for(ProjectMetadata metadata : metadataManager.getAllProjects()){
									wsManager.sendRequest(new ProjectSubscribeRequest(metadata.getProjectId())
											.getRequest(null, null));
									}
								} catch (ConnectException e) {
									e.printStackTrace();
								}

								// For creating new file
								// IRequestData data = new
								// FileCreateRequest(StringConstants.FILE_NAME,
								// StringConstants.FILE_PATH,
								// StringConstants.PROJ_ID,
								// ("package testPkg1;\n" + "\n" + "public class
								// TestClass1 {\n"
								// + "\n" + "}").getBytes());
								// try {
								// getWSManager().sendRequest(data.getRequest());
								// } catch (ConnectException e) {
								// // TODO Auto-generated catch block
								// e.printStackTrace();
								// }
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
