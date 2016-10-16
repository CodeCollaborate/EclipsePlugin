package cceclipseplugin.ui;

import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.ViewPart;

import cceclipseplugin.constants.StringConstants;
import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.dialogs.MessageDialog;
import dataMgmt.SessionStorage;
import websocket.WSConnection;
import websocket.WSConnection.State;
import websocket.models.Permission;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.notifications.ProjectGrantPermissionsNotification;
import websocket.models.requests.ProjectLookupRequest;
import websocket.models.responses.ProjectLookupResponse;
import websocket.WSManager;

public class ControlPanel extends ViewPart {

	protected ListViewer projectsListViewer;
	protected ListViewer usersListViewer;
	private StatusBar statusBar;
	private ListViewsParent views;
	
	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		parent.setLayout(layout);
		
		views = new ListViewsParent(parent, SWT.NONE);

		GridData viewsData = new GridData();
		viewsData.grabExcessHorizontalSpace = true;
		viewsData.horizontalAlignment = GridData.FILL;
		viewsData.grabExcessVerticalSpace = true;
		viewsData.verticalAlignment = GridData.FILL;
		views.setLayoutData(viewsData);
		
		statusBar = new StatusBar(parent, SWT.BORDER);
		GridData statusData = new GridData();
		statusData.grabExcessHorizontalSpace = true;
		statusData.horizontalAlignment = GridData.FILL;
		statusBar.setLayoutData(statusData);
		initializePropertyChangeListeners();
		initializeNotificationHandlers();
	}
	
	private void initializePropertyChangeListeners() {
		PluginManager.getInstance().getDataManager().getSessionStorage().addPropertyChangeListener((event) -> {
			if (!event.getPropertyName().equals(SessionStorage.USERNAME)) {
				return;
			}
			
			if (event.getNewValue() != null) {
				Display.getDefault().asyncExec(() -> this.setEnabled(true));
			} else {
				Display.getDefault().asyncExec(() -> this.setEnabled(false));
			}
		});
	}
	
	private void initializeNotificationHandlers() {
		WSManager wsManager = PluginManager.getInstance().getWSManager();
		SessionStorage sessionStorage = PluginManager.getInstance().getDataManager().getSessionStorage();
		Shell shell = new Shell();
		
		// project handlers
		
		// grant permissions
		wsManager.registerNotificationHandler("Project", "GrantPermissions", notification -> {
			ProjectGrantPermissionsNotification pgpnotif = ((ProjectGrantPermissionsNotification) notification.getData());
			if (pgpnotif.grantUsername.equals(sessionStorage.getUsername())) {
				Long[] projectIds = { notification.getResourceID() };
				Request getProjectDetails = new ProjectLookupRequest(projectIds).getRequest(response -> {
					int status = response.getStatus();
					if (status == 200) {
						Project[] projectResponse = ((ProjectLookupResponse) response.getData()).getProjects();
						if (projectResponse.length == 1) {
							sessionStorage.addProject(projectResponse[0]);
						} else {
							// TODO: Change to use UI logger
							MessageDialog errDialog = new MessageDialog(shell, "Invalid number of projects updated for notification: " + projectResponse.length);
							shell.getDisplay().asyncExec(() -> errDialog.open());
						}
					} else {
						MessageDialog errDialog = new MessageDialog(shell, "Unable to fetch project information.");
						shell.getDisplay().asyncExec(() -> errDialog.open());
					}
				}, new UIRequestErrorHandler(shell, "Could not send lookup project request"));
				wsManager.sendRequest(getProjectDetails);
			} else {
				Project toUpdate = sessionStorage.getProjectById(notification.getResourceID());
				toUpdate.getPermissions().put(pgpnotif.grantUsername, new Permission(pgpnotif.grantUsername, pgpnotif.permissionLevel, null, null));
				sessionStorage.addProject(toUpdate);
			}
		});
		
		// status bar handlers
		wsManager.registerEventHandler(WSConnection.EventType.ON_CONNECT, () -> {
			statusBar.getDisplay().asyncExec(() -> statusBar.setStatus(StringConstants.CONNECT_MESSAGE));
		});
		wsManager.registerEventHandler(WSConnection.EventType.ON_CLOSE, () -> {
			statusBar.getDisplay().asyncExec(() -> statusBar.setStatus(StringConstants.CLOSE_MESSAGE));
		});
		wsManager.registerEventHandler(WSConnection.EventType.ON_ERROR, () -> {
			statusBar.getDisplay().asyncExec(() -> statusBar.setStatus(StringConstants.ERROR_MESSAGE));
		});
		State s = wsManager.getConnectionState();
		switch (s) {
		case CLOSE:
			statusBar.setStatus(StringConstants.CLOSE_MESSAGE);
			break;
		case CONNECT:
			statusBar.setStatus(StringConstants.CONNECT_MESSAGE);
			break;
		case ERROR:
			statusBar.setStatus(StringConstants.ERROR_MESSAGE);
			break;
		default:
			break;
		}
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
	}
	
	public void setEnabled(boolean b) {
		views.getProjectListView().getListWithButtons().setEnabled(b);
		views.getUserListView().getListWithButtons().setEnabled(b);
	}
}
