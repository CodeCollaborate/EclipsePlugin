package cceclipseplugin.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.dialogs.AddNewUserDialog;
import cceclipseplugin.ui.dialogs.RemoveUserDialog;
import dataMgmt.SessionStorage;
import websocket.models.Permission;
import websocket.models.Project;

public class UsersListView extends ListView {
	private Project currentProject = null;

	public UsersListView(Composite parent, int style, ProjectsListView listView) {
		super(parent, style, "Users");
		this.initializeListeners(listView);
	}
	
	private Project getProjectAt(int index) {
		java.util.List<Project> projects = PluginManager.getInstance().getDataManager().getSessionStorage().getSortedProjects();
		if (index < 0 || index >= projects.size()) {
			return null;
		}
		return projects.get(index);
	}
	
	private void refreshSelected(ProjectsListView listView) {
		Display.getDefault().asyncExec(() -> {
			int selectedListIndex = listView.getListWithButtons().getList().getSelectionIndex();
			Project proj = getProjectAt(selectedListIndex);
			if (proj != null && PluginManager.getInstance().getDataManager().getSessionStorage()
					.getSubscribedIds().contains(proj.getProjectID())) {
				setProject(proj);
				listView.getListWithButtons().getButtonBar().getMinusButton().setEnabled(true);
			} else {
				getListWithButtons().getList().removeAll();
				String projName = proj == null ? "a project" : proj.getName();
				String message = "You must be subscribed to " + 
						projName + " to view users.";
				getListWithButtons().getList().add(message);
				VerticalButtonBar bar = getListWithButtons().getButtonBar();
				bar.getPlusButton().setEnabled(false);
				bar.getMinusButton().setEnabled(false);
				bar.getReloadButton().setEnabled(false);
			}
		});
	}

	PropertyChangeListener projectListListener;
	
	private void initializeListeners(ProjectsListView listView) {
		// project listview selection
		listView.initSelectionListener(new Listener() {
			@Override
			public void handleEvent(Event event) {
				refreshSelected(listView);
			}
		});
		
		// project list property change
		projectListListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getPropertyName().equals(SessionStorage.PROJECT_LIST)) {
					if (!listView.isDisposed()) {
						Display.getDefault().asyncExec(() -> {
							int selectedListIndex = listView.getListWithButtons().getList().getSelectionIndex();
							if (selectedListIndex != -1) {
								SessionStorage storage = (SessionStorage) event.getSource();
								java.util.List<Project> projects = storage.getSortedProjects();
								Project project = projects.get(selectedListIndex);
								setProject(project);
							} else {
								setProject(null);
							}
						});
					}
				} else if (event.getPropertyName().equals(SessionStorage.SUBSCRIBED_PROJECTS)) {
					refreshSelected(listView);
				}
			}
		};
		PluginManager.getInstance().getDataManager().getSessionStorage().addPropertyChangeListener(projectListListener);
		
		// plus button pressed
		VerticalButtonBar bar = this.getListWithButtons().getButtonBar();
		bar.getPlusButton().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				Display.getDefault().asyncExec(() -> {
					Shell shell = Display.getDefault().getActiveShell();
					List projectList = listView.getListWithButtons().getList();
					if (projectList.getSelectionIndex() == -1) {
						return;
					}
					Project p = PluginManager.getInstance().getDataManager().getSessionStorage()
							.getSortedProjects().get(projectList.getSelectionIndex());
					AddNewUserDialog addUserDialog = new AddNewUserDialog(shell, p);
					addUserDialog.open();
				});
			}
		});
		
		// minus button pressed
		bar.getMinusButton().addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				if (currentProject == null) {
					return;
				}
				List list = getListWithButtons().getList();
				Shell shell = Display.getDefault().getActiveShell();
				Dialog removeUserDialog = new RemoveUserDialog(shell, list.getItem(list.getSelectionIndex()),
						currentProject.getName(), currentProject.getProjectID());
				removeUserDialog.open();
			}
		});

		// reload button
		bar.getReloadButton().addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				refreshSelected(listView);
			}

		});

		// user list selection
		List list = this.getListWithButtons().getList();
		list.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				getListWithButtons().getButtonBar().getMinusButton().setEnabled(true);
			}
		});
	}
	
	private void removePropertyChangeListeners() {
		PluginManager.getInstance().getDataManager().getSessionStorage().removePropertyChangeListener(projectListListener);
	}

	public void setProject(Project project) {
		this.currentProject = project;
		Display.getDefault().asyncExec(() -> {
			List list = this.getListWithButtons().getList();
			if (!list.isDisposed()) {
				list.removeAll();
			}
			getListWithButtons().getButtonBar().getPlusButton().setEnabled(false);
			getListWithButtons().getButtonBar().getMinusButton().setEnabled(false);
			if (project == null) {
				return;
			}
			HashMap<String, Permission> permissions = project.getPermissions();
			if (permissions != null) {
				for (String key : permissions.keySet()) {
					@SuppressWarnings("unused")
					Permission permy = permissions.get(key);
					// TODO: add permision level strings to the list (or table, in the future)
					if (!list.isDisposed()) {
						list.add(key);
					}
				}
			}
			VerticalButtonBar bar = getListWithButtons().getButtonBar();
			bar.getPlusButton().setEnabled(true);
			bar.getReloadButton().setEnabled(true);
		});
	}
	
	@Override
	public void dispose() {
		removePropertyChangeListeners();
		super.dispose();
	}
}
