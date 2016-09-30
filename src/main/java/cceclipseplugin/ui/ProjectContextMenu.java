package cceclipseplugin.ui;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.Preferences;

import cceclipseplugin.Activator;
import cceclipseplugin.core.PluginManager;
import cceclipseplugin.preferences.PreferenceConstants;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.ProjectSubscribeRequest;
import websocket.models.requests.ProjectUnsubscribeRequest;

public class ProjectContextMenu extends Menu {
	
	private Control parent;
	private long projectID;
	
	public ProjectContextMenu(Control parent, Project p) {
		super(parent);
		
		projectID = p.getProjectID();
		
		Preferences pluginPrefs = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
		Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
		Preferences thisProjectPrefs = projectPrefs.node(projectID + "");
		boolean subscribed = thisProjectPrefs.getBoolean(PreferenceConstants.VAR_SUBSCRIBED, true);
		if (subscribed)
			makeUnsubscribeItem();
		else
			makeSubscribeItem();
	}
	
	private MenuItem makeSubscribeItem() {
		MenuItem sub = new MenuItem(this, SWT.NONE);
		sub.setText("Subscribe");
		sub.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				// send request
				Semaphore waiter = new Semaphore(0);
				Request req = (new ProjectSubscribeRequest(projectID)).getRequest(
						response -> {
							parent.setEnabled(true);
							Preferences pluginPrefs = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
							Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
							Preferences thisProjectPrefs = projectPrefs.node(projectID + "");
							thisProjectPrefs.putBoolean(PreferenceConstants.VAR_SUBSCRIBED, true);
							
						},
						new UIRequestErrorHandler(new Shell(), "Failed to send project subscribe request."));
				
				PluginManager.getInstance().getWSManager().sendRequest(req);
				
				try {
					if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
						MessageDialog errDialog = new MessageDialog(new Shell(), "Request timed out.");
						errDialog.open();
					}
				} catch (InterruptedException e) {
					MessageDialog errDialog = new MessageDialog(new Shell(), e.getMessage());
					errDialog.open();
				}
			}
			
		});
		return sub;
	}
	
	private MenuItem makeUnsubscribeItem() {
		MenuItem unsub = new MenuItem(this, SWT.NONE);
		unsub.setText("Unsubscribe");
		unsub.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				
				Semaphore waiter = new Semaphore(0);
				Request req = (new ProjectUnsubscribeRequest(projectID)).getRequest(
						response -> {
							parent.setEnabled(false);
							Preferences pluginPrefs = DefaultScope.INSTANCE.getNode(Activator.PLUGIN_ID);
							Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
							Preferences thisProjectPrefs = projectPrefs.node(projectID + "");
							thisProjectPrefs.putBoolean(PreferenceConstants.VAR_SUBSCRIBED, false);
						},
						new UIRequestErrorHandler(new Shell(), "Failed to send project unsubscribe request."));
				
				PluginManager.getInstance().getWSManager().sendRequest(req);
				
				try {
					if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
						MessageDialog errDialog = new MessageDialog(new Shell(), "Request timed out.");
						errDialog.open();
					}
				} catch (InterruptedException e) {
					MessageDialog errDialog = new MessageDialog(new Shell(), e.getMessage());
					errDialog.open();
				}
			}
			
		});
		return unsub;
	}
	
}
