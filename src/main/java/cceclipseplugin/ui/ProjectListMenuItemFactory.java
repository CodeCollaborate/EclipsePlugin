package cceclipseplugin.ui;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.osgi.service.prefs.Preferences;

import cceclipseplugin.Activator;
import cceclipseplugin.core.PluginManager;
import cceclipseplugin.core.ProjectManager;
import cceclipseplugin.preferences.PreferenceConstants;
import cceclipseplugin.ui.dialogs.MessageDialog;
import dataMgmt.MetadataManager;
import websocket.models.File;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.ProjectGetFilesRequest;
import websocket.models.requests.ProjectSubscribeRequest;
import websocket.models.requests.ProjectUnsubscribeRequest;
import websocket.models.responses.ProjectGetFilesResponse;

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
									Display.getDefault().asyncExec(() -> MessageDialog.createDialog("Could not save preferences.").open());
								}
							} else {
								Display.getDefault().asyncExec(() -> MessageDialog.createDialog("Project subscribe request failed with status code " + response.getStatus()).open());
					}
				} , new UIRequestErrorHandler("Failed to send project subscribe request."));
				PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
				
				Request getFilesReq = (new ProjectGetFilesRequest(p.getProjectID())).getRequest(response -> {
					File[] files = ((ProjectGetFilesResponse) response.getData()).files;
					IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
					IProject eclipseProject = root.getProject(p.getName());
					ProjectManager pm = new ProjectManager();
					for (File f : files) {
						pm.pullFileAndCreate(eclipseProject, p, f, new NullProgressMonitor());
					}
				}, new UIRequestErrorHandler("Failed to send project get files request."));
				PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(getFilesReq);
			}
			
		});
	}

	public static void makeUnsubscribeItem(Menu parentMenu, Project p) {
		MenuItem unsub = new MenuItem(parentMenu, SWT.NONE);
		unsub.setText("Unsubscribe");
		unsub.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				Request req = (new ProjectUnsubscribeRequest(p.getProjectID())).getRequest(response -> {
					if (response.getStatus() == 200) {
						Preferences pluginPrefs = InstanceScope.INSTANCE.getNode(Activator.PLUGIN_ID);
						Preferences projectPrefs = pluginPrefs.node(PreferenceConstants.NODE_PROJECTS);
						Preferences thisProjectPrefs = projectPrefs.node(p.getProjectID() + "");
						thisProjectPrefs.putBoolean(PreferenceConstants.VAR_SUBSCRIBED, false);
						try {
							pluginPrefs.flush();
						} catch (Exception e) {
							Display.getDefault().asyncExec(() -> MessageDialog.createDialog("Could not save preferences.").open());
						}
					} else {
						Display.getDefault().asyncExec(() -> MessageDialog.createDialog("Project unsubscribe request failed with status code " + response.getStatus()).open());
					}
				} , new UIRequestErrorHandler("Failed to send project unsubscribe request."));

				PluginManager.getInstance().getWSManager().sendRequest(req);
			}
		});
	}
	
	public static void makeAddProjectToWorkspaceItem(Menu parentMenu, Project p) {
		MenuItem pull = new MenuItem(parentMenu, SWT.NONE);
		pull.setText("Add project to workspace");
		pull.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				Request req = (new ProjectGetFilesRequest(p.getProjectID())).getRequest(response -> {
					PluginManager pluginManager = PluginManager.getInstance();
					
					File[] files = ((ProjectGetFilesResponse) response.getData()).files;
					ProjectManager pm = pluginManager.getProjectManager();
					try {
						pm.createEclipseProject(p, files);
					} catch (Exception e) {
						Display.getDefault().asyncExec(() -> 
							MessageDialog.createDialog("Files were pulled but could not be put into an Eclipse project.").open());
						pm.deleteEclipseProject(p);
					}
				}, new UIRequestErrorHandler("Failed to send project get files request."));
				
				PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);				
			}
		});
	}
	
}
