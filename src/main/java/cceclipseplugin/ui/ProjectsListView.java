package cceclipseplugin.ui;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.window.Window;
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
import websocket.models.Request;
import websocket.models.requests.UserProjectsRequest;
import websocket.models.responses.UserProjectsResponse;

public class ProjectsListView extends ListView {
	
	private Project[] projects;
	
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
				
				String selectedName = list.getItem(selected);
				
				Project selectedProj = null;
				for (Project p : projects) {
					if (p.getName().equals(selectedName)) {
						selectedProj = p;
						break;
					}
				}
				
				
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
//		Timer timer = new Timer();
//		// TODO: register notification listener instead of querying every minute
//		timer.schedule(new TimerTask() {
//			@Override
//			public void run() {
//				queryForProjects();
//			}
//		}, 60*1000, 60*1000);
		new Runnable() {

			@Override
			public void run() {
				queryForProjects();
			}
		}.run();
	}
	
	private void queryForProjects() {
		// query for projects
//		Semaphore waiter = new Semaphore(0);
		List list = this.getListWithButtons().getList();
		Request getProjectsRequest = new UserProjectsRequest()
				.getRequest(response -> {
					int status = response.getStatus();
					if (status != 200) {
						list.add("Error fetching projects");
					} else {
						projects = ((UserProjectsResponse) response.getData()).getProjects();
						list.getDisplay().asyncExec(() -> {
							list.removeAll(); // TODO: don't change duplicate items
							for (Project p : projects) {
								list.add(p.getName());
								// TODO: store projects somewhere else (probably client core)
							}
						});
					}
//					waiter.release();
					// TODO: notify userslistview of possible changes
				}, new UIRequestErrorHandler(getShell(), 
						"Could not send user projects request"));
		PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(getProjectsRequest);

//		try {
//			PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(getProjectsRequest);
//			if (!waiter.tryAcquire(1, 5, TimeUnit.SECONDS)) {
//	            MessageDialog errDialog = new MessageDialog(getShell(), "Request timed out.");
//	            errDialog.open();
//			}
//		} catch (InterruptedException ex) {
//			MessageDialog err = new MessageDialog(getShell(), ex.getMessage());
//			err.open();
//		}
	}
	
	public void initSelectionListener(Listener listener) {
		List list = this.getListWithButtons().getList();
		list.addListener(SWT.Selection, listener);
		VerticalButtonBar bar = this.getListWithButtons().getButtonBar();
		bar.getPlusButton().addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				AddProjectDialog dialog = new AddProjectDialog(new Shell());
				if (Window.OK == dialog.open()) {
					// TODO: Refresh project list
				}
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
	
	// TODO: Move this method to a class that allows you to access project-level prefs
	private boolean getSubscribedVarFromPrefs(Project p) {
		Preferences pluginPrefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
		Preferences thisProjectPrefs = projectPrefs.node(p.getProjectID() + "");
		return thisProjectPrefs.getBoolean(PreferenceConstants.VAR_SUBSCRIBED, true);
	}
}
