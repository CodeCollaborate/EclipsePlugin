package cceclipseplugin.ui;

import java.util.Arrays;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.dialogs.AddProjectDialog;
import cceclipseplugin.ui.dialogs.DeleteProjectDialog;
import cceclipseplugin.ui.dialogs.MessageDialog;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.notifications.ProjectGrantPermissionsNotification;
import websocket.models.requests.ProjectLookupRequest;
import websocket.models.requests.UserProjectsRequest;
import websocket.models.responses.ProjectLookupResponse;
import websocket.models.responses.UserProjectsResponse;

public class ProjectsListView extends ListView {
	
	private Project[] projects;
	private Shell shell;
	
	public ProjectsListView(Composite parent, int style) {
		super(parent, style, "Projects");
		shell = getShell();
		this.initializeData();
	}
	
	private void initializeData() {	
		new Thread(new Runnable() {

			@Override
			public void run() {
				// query for projects
				List list = getListWithButtons().getList();
				sendRequestForProjects(list);
				
				// register a notification handler for new projects
				PluginManager.getInstance().getWSManager().registerNotificationHandler("Projects", "GrantPermissions", notification -> {
					ProjectGrantPermissionsNotification pgpnotif = ((ProjectGrantPermissionsNotification) notification.getData());
					if (pgpnotif.grantUsername.equals(PluginManager.getInstance().getDataManager().getSessionStorage().getUsername())) {
						sendRequestForProjects(list);
					} else {
						Long[] projectIds = { notification.getResourceID() };
						Request getProjectDetails = new ProjectLookupRequest(projectIds).getRequest(response -> {
							int status = response.getStatus();
							if (status != 200) {
								MessageDialog errDialog = new MessageDialog(shell, "Unable to fetch project information.");
								shell.getDisplay().asyncExec(() -> errDialog.open());
							} else {
								Project[] projectResponse = ((ProjectLookupResponse) response.getData()).getProjects();
								if (projectResponse.length != 1) {
									MessageDialog errDialog = new MessageDialog(shell, "Invalid number of projects updated for notification: " + projectResponse.length);
									shell.getDisplay().asyncExec(() -> errDialog.open());
								} else {
									PluginManager.getInstance().getDataManager().getSessionStorage().addProject(projectResponse[0]);
								}
							}
						}, new UIRequestErrorHandler(shell, "Could not send lookup project request"));
						PluginManager.getInstance().getWSManager().sendRequest(getProjectDetails);
					}
				});
			}
		}).start();
	}
	
	private void sendRequestForProjects(List listToUpdate) {
		Request getProjectsRequest = new UserProjectsRequest()
				.getRequest(response -> {
					int status = response.getStatus();
					if (status != 200) {
						MessageDialog errDialog = new MessageDialog(shell, "Unable to fetch projects.");
						shell.getDisplay().asyncExec(() -> errDialog.open());
					} else {
						projects = ((UserProjectsResponse) response.getData()).getProjects();
						PluginManager.getInstance().getDataManager().getSessionStorage().setProjects(Arrays.asList(projects));
						listToUpdate.getDisplay().asyncExec(() -> {
							listToUpdate.removeAll();
							for (Project p : projects) {
								listToUpdate.add(p.getName());
							}
						});
					}
					// TODO: notify userslistview of possible changes
				}, new UIRequestErrorHandler(shell,  
						"Could not send user projects request"));
		PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(getProjectsRequest);
	}
	
	public void initSelectionListener(Listener listener) {
		List list = this.getListWithButtons().getList();
		list.addListener(SWT.Selection, listener);
		VerticalButtonBar bar = this.getListWithButtons().getButtonBar();
		bar.getPlusButton().addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				AddProjectDialog dialog = new AddProjectDialog(new Shell());
				getShell().getDisplay().asyncExec(()-> dialog.open());
			}
		});
		bar.getMinusButton().addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				if (list.getSelectionIndex() == -1) {
					MessageDialog err = new MessageDialog(new Shell(), "No project is selected.");
					err.open();
					return;
				}
				
				Project selectedProject = projects[list.getSelectionIndex()];
				DeleteProjectDialog delete = new DeleteProjectDialog(new Shell(), selectedProject);
				delete.open();
			}
			
		});
	}
	
	public Project getProjectAt(int index) {
		if (index < 0 || index >= projects.length)
			return null;
		return projects[index];
	}
}
