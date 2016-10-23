package cceclipseplugin.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.Preferences;

import cceclipseplugin.Activator;
import cceclipseplugin.core.PluginManager;
import cceclipseplugin.preferences.PreferenceConstants;
import cceclipseplugin.ui.dialogs.AddProjectDialog;
import cceclipseplugin.ui.dialogs.DeleteProjectDialog;
import cceclipseplugin.ui.dialogs.MessageDialog;
import dataMgmt.SessionStorage;
import websocket.models.Project;

public class ProjectsListView extends ListView {
		
	public ProjectsListView(Composite parent, int style) {
		super(parent, style, "Projects");
		this.initializeData();
		this.initContextMenu();
		this.initButtonListeners();
	}
	
	private void initContextMenu() {
		List list = this.getListWithButtons().getList();
		Menu menu = new Menu(list);
		list.setMenu(menu);
		menu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				int selected = list.getSelectionIndex();
				
				for (MenuItem item : menu.getItems()) {
					item.dispose();
				}
				
				if (selected < 0 || selected > list.getItemCount()) {
					return;
				}
								
				Project selectedProj = null;
				java.util.List<Project> projects = PluginManager.getInstance().getDataManager().getSessionStorage().getSortedProjects();
				selectedProj = projects.get(selected);

				boolean subscribed = PluginManager.getInstance().getDataManager().getSessionStorage()
						.getSubscribedIds().contains(selectedProj.getProjectID());
				if (subscribed) {
					ProjectListMenuItemFactory.makeUnsubscribeItem(menu, selectedProj);
				} else {
					ProjectListMenuItemFactory.makeSubscribeItem(menu, selectedProj);
				}
			}
		});
	}
	
	PropertyChangeListener projectListListener;
	private void initializeData() {	
		// register handler for projects
		List list = getListWithButtons().getList();
		projectListListener = new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				if (event.getPropertyName() != SessionStorage.PROJECT_LIST) {
					return;
				}
				Display.getDefault().asyncExec(() -> {
					if (!list.isDisposed()) {
						list.removeAll();
					}
					for (Project p : PluginManager.getInstance().getDataManager().getSessionStorage().getSortedProjects()) {
						if (!list.isDisposed()) {
							list.add(p.getName());
						}
					}
				}); 
			}
		};
		PluginManager.getInstance().getDataManager().getSessionStorage().addPropertyChangeListener(projectListListener);
	}
	
	private void removePropertyListeners() {
		PluginManager.getInstance().getDataManager().getSessionStorage().removePropertyChangeListener(projectListListener);
	}
	
	private void initButtonListeners() {
		List list = getListWithButtons().getList();
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
					MessageDialog.createDialog("No project is selected.").open();
					return;
				}
				
				java.util.List<Project> projects = PluginManager.getInstance().getDataManager().getSessionStorage().getSortedProjects();
				Project selectedProject = projects.get(list.getSelectionIndex());
				DeleteProjectDialog delete = new DeleteProjectDialog(new Shell(), selectedProject);
				delete.open();
			}
		});
	}
	
	public void initSelectionListener(Listener listener) {
		List list = this.getListWithButtons().getList();
		list.addListener(SWT.Selection, listener);
	}
	
	private boolean getSubscribedVarFromPrefs(Project p) {
		Preferences pluginPrefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
		Preferences thisProjectPrefs = projectPrefs.node(p.getProjectID() + "");
		return thisProjectPrefs.getBoolean(PreferenceConstants.VAR_SUBSCRIBED, true);
	}
	
	@Override
	public void dispose() {
		removePropertyListeners();
		super.dispose();
	}
}
