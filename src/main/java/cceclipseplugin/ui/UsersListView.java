package cceclipseplugin.ui;

import java.util.HashMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.dialogs.AddNewUserDialog;
import cceclipseplugin.ui.dialogs.RemoveUserDialog;
import websocket.models.Permission;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.ProjectGrantPermissionsRequest;

public class UsersListView extends ListView {
	
	private Project currentProject = null;
	
	public UsersListView(Composite parent, int style, ProjectsListView listView) {
		super(parent, style, "Users");
		this.initializeListeners(listView);
	}
	
	private void initializeListeners(ProjectsListView listView) {
		listView.initSelectionListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				System.out.println("Handling click: "+event.index+" "+listView.getProjectAt(event.index));
				setProject(listView.getProjectAt(event.index));
			}
		});
		VerticalButtonBar bar = this.getListWithButtons().getButtonBar();
		bar.getPlusButton().addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				AddNewUserDialog addUserDialog = new AddNewUserDialog(new Shell());
				String username = null;
				int permission = -1;
				if (Window.OK == addUserDialog.open()) {
					username = addUserDialog.getNewUserName();
					permission = addUserDialog.getNewUserPermission();
				}
				List projectList = listView.getListWithButtons().getList();
				Project p = PluginManager.getInstance().getDataManager().getSessionStorage().getProjects().get(projectList.getSelectionIndex());
				
				Request req = new ProjectGrantPermissionsRequest(p.getProjectID(), username, permission).getRequest(
						new UIResponseHandler(new Shell(), "Project grant permissions request"), 
						new UIRequestErrorHandler(new Shell(), "Could not send request."));
				PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
				
			}
		});
		bar.getMinusButton().addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				if (currentProject == null) {
					return;
				}
				List list = getListWithButtons().getList();
				Dialog removeUserDialog = new RemoveUserDialog(new Shell(), list.getItem(list.getSelectionIndex()), currentProject.getName());
				removeUserDialog.open();
			}
		});
	}
	
	public void setProject(Project project) {
		this.currentProject = project;
		List list = this.getListWithButtons().getList();
		list.removeAll();
		HashMap<String, Permission> permissions = project.getPermissions();
		if (permissions != null) {
			for (String key : permissions.keySet()) {
				list.add(key + " " + permissions.get(key));
			}
		}
	}
}
