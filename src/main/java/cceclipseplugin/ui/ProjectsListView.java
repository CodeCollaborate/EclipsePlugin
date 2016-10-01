package cceclipseplugin.ui;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.window.Window;
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
import websocket.models.requests.UserProjectsRequest;
import websocket.models.responses.UserProjectsResponse;

public class ProjectsListView extends ListView {
	
	private Project[] projects;
	
	public ProjectsListView(Composite parent, int style) {
		super(parent, style, "Projects");
		this.initializeData();
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
		Semaphore waiter = new Semaphore(0);
		List list = this.getListWithButtons().getList();
		Request getProjectsRequest = new UserProjectsRequest()
				.getRequest(response -> {
					int status = response.getStatus();
					if (status != 200) {
						list.add("Error fetching projects");
					} else {
						projects = ((UserProjectsResponse) response.getData()).getProjects();
						list.removeAll(); // TODO: don't change duplicate items
						ProjectsListView.this.getDisplay().asyncExec(new Runnable() {
							@Override
							public void run() {
								for (Project p : projects) {
									list.add(p.getName());
									// TODO: store projects somewhere else (probably client core)
								}
							}
						});
					}
					waiter.release();
					// TODO: notify userslistview of possible changes
				}, new UIRequestErrorHandler(getShell(), 
						"Could not send user projects request"));
		try {
			PluginManager.getInstance().getWSManager().sendRequest(getProjectsRequest);
			if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
	            MessageDialog errDialog = new MessageDialog(getShell(), "Request timed out.");
	            errDialog.open();
			}
		} catch (InterruptedException ex) {
			MessageDialog err = new MessageDialog(getShell(), ex.getMessage());
			err.open();
		}
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
}
