package cceclipseplugin.ui;

import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.widgets.Composite;
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
import websocket.models.Project;

public class ProjectsListView extends ListView {
		
	public ProjectsListView(Composite parent, int style) {
		super(parent, style, "Projects");
		this.initializeData();
		this.initContextMenu();
	}
	
	private void initContextMenu() {
		List list = this.getListWithButtons().getList();
		Menu menu = new Menu(list);
		list.setMenu(menu);
		menu.addMenuListener(new MenuAdapter() {
			public void menuShown(MenuEvent e) {
				int selected = list.getSelectionIndex();
				
				if (selected < 0 || selected > list.getItemCount())
					return;
								
				Project selectedProj = null;
				java.util.List<Project> projects = PluginManager.getInstance().getDataManager().getSessionStorage().getProjects();
				selectedProj = projects.get(selected);
				
				for (MenuItem item : menu.getItems())
					item.dispose();
				
				boolean subscribed = getSubscribedVarFromPrefs(selectedProj);
				if (subscribed)
					ProjectListMenuItemFactory.makeUnsubscribeItem(menu, selectedProj);
				else
					ProjectListMenuItemFactory.makeSubscribeItem(menu, selectedProj);
			}
		});
	}
	
	private void initializeData() {	
		new Thread(new Runnable() {

			@Override
			public void run() {
				// register handler for projects
				List list = getListWithButtons().getList();
				PluginManager.getInstance().getDataManager().getSessionStorage().addPropertyChangeListener((event) -> {
					list.getDisplay().asyncExec(() -> {
						list.removeAll();
						for (Project p : PluginManager.getInstance().getDataManager().getSessionStorage().getProjects()) {
							list.add(p.getName());
						}
					}); 
				});
				PluginManager.getInstance().getRequestManager().fetchProjects();
			}
		}).start();
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
				
				java.util.List<Project> projects = PluginManager.getInstance().getDataManager().getSessionStorage().getProjects();
				Project selectedProject = projects.get(list.getSelectionIndex());
				DeleteProjectDialog delete = new DeleteProjectDialog(new Shell(), selectedProject);
				delete.open();
			}
			
		});
	}
	
	public Project getProjectAt(int index) {
		java.util.List<Project> projects = PluginManager.getInstance().getDataManager().getSessionStorage().getProjects();
		if (index < 0 || index >= projects.size())
			return null;
		return projects.get(index);
	}
	
	// TODO: Move this method to a class that allows you to access project-level prefs
	private boolean getSubscribedVarFromPrefs(Project p) {
		Preferences pluginPrefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
		Preferences thisProjectPrefs = projectPrefs.node(p.getProjectID() + "");
		return thisProjectPrefs.getBoolean(PreferenceConstants.VAR_SUBSCRIBED, true);
	}
}
