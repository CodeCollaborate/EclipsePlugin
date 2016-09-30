package cceclipseplugin.ui;

import java.util.HashMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.ui.dialogs.AddNewUserDialog;
import cceclipseplugin.ui.dialogs.RemoveUserDialog;
import websocket.models.Permission;
import websocket.models.Project;

public class UsersListView extends ListView {
	
	private Shell dialogShell = new Shell();
	private Project currentProject = null;
	
	public UsersListView(Composite parent, int style, ProjectsListView listView) {
		super(parent, style, "Users");
		this.initializeListeners(listView);
	}
	
	private void initializeListeners(ProjectsListView listView) {
		listView.initSelectionListener(new Listener() {

			@Override
			public void handleEvent(Event event) {
				setProject(listView.getProjectAt(event.index));
			}
		});
		VerticalButtonBar bar = this.getListWithButtons().getButtonBar();
		bar.getPlusButton().addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				Dialog addUserDialog = new AddNewUserDialog(dialogShell);
				addUserDialog.open();
			}
		});
		bar.getMinusButton().addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				// TODO: remove 'username' and replace with actual username query
				if (currentProject == null) {
					return;
				}
				Dialog removeUserDialog = new RemoveUserDialog(dialogShell, "username", currentProject.getName());
				removeUserDialog.open();
			}
		});
	}
	
	public void setProject(Project project) {
		this.currentProject = project;
		List list = this.getListWithButtons().getList();
		list.removeAll();
		HashMap<String, Permission> permissions = project.getPermissions();
		for (String key : permissions.keySet()) {
			list.add(key + " " + permissions.get(key));
		}
	}
}
