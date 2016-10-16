package cceclipseplugin.ui;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.osgi.service.prefs.Preferences;

import cceclipseplugin.Activator;
import cceclipseplugin.core.PluginManager;
import cceclipseplugin.preferences.PreferenceConstants;
import cceclipseplugin.ui.dialogs.MessageDialog;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.ProjectSubscribeRequest;
import websocket.models.requests.ProjectUnsubscribeRequest;

public class ProjectListMenuItemFactory {
	
	public static void makeSubscribeItem(Menu parentMenu, Project p) {
		MenuItem sub = new MenuItem(parentMenu, SWT.NONE);
		sub.setText("Subscribe");
		sub.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				Request req = (new ProjectSubscribeRequest(p.getProjectID())).getRequest(
						response -> {
							if (response.getStatus() == 200) {
								Preferences pluginPrefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
								Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
								Preferences thisProjectPrefs = projectPrefs.node(p.getProjectID() + "");
								thisProjectPrefs.putBoolean(PreferenceConstants.VAR_SUBSCRIBED, true);
								try {
									pluginPrefs.flush();
								} catch (Exception e) {
									MessageDialog errDialog = new MessageDialog(new Shell(), "Could not save preferences.");
									Display.getDefault().asyncExec(() ->errDialog.open());
								}
							} else {
								Display.getDefault().asyncExec(() -> 
									new MessageDialog(new Shell(), "Project subscribe request failed with status code " + response.getStatus()).open());
							}
						},
						new UIRequestErrorHandler(new Shell(), "Failed to send project subscribe request."));
				
				PluginManager.getInstance().getWSManager().sendRequest(req);
			}
			// TODO: make call to pull files in clientcore directory watching system
			// TODO: import the directory of the project as a project in eclipse using programmatic import
		});
	}
	
	public static void makeUnsubscribeItem(Menu parentMenu, Project p) {
		MenuItem unsub = new MenuItem(parentMenu, SWT.NONE);
		unsub.setText("Unsubscribe");
		unsub.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				Request req = (new ProjectUnsubscribeRequest(p.getProjectID())).getRequest(
						response -> {
							if (response.getStatus() == 200) {
								Preferences pluginPrefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
								Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
								Preferences thisProjectPrefs = projectPrefs.node(p.getProjectID() + "");
								thisProjectPrefs.putBoolean(PreferenceConstants.VAR_SUBSCRIBED, false);
								try {
									pluginPrefs.flush();
								} catch (Exception e) {
									MessageDialog errDialog = new MessageDialog(new Shell(), "Could not save preferences.");
									Display.getDefault().asyncExec(() ->errDialog.open());
								}
							} else {
								Display.getDefault().asyncExec(() -> 
									new MessageDialog(new Shell(), "Project unsubscribe request failed with status code " + response.getStatus()).open());								
							}
						},
						new UIRequestErrorHandler(new Shell(), "Failed to send project unsubscribe request."));
				
				PluginManager.getInstance().getWSManager().sendRequest(req);
			}
		});
	}
	
}
