package cceclipseplugin.ui;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.dialogs.MessageDialog;
import cceclipseplugin.ui.dialogs.OkCancelDialog;
import websocket.models.Project;

public class ProjectListMenuItemFactory {
	
	public static void makeSubscribeItem(Menu parentMenu, Project p) {
		MenuItem sub = new MenuItem(parentMenu, SWT.NONE);
		sub.setText("Subscribe");
		sub.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				if (Window.OK == OkCancelDialog.createDialog("Subscribing will overwrite all local changes with those that are on the server.").open()) {
					PluginManager.getInstance().getRequestManager().subscribeToProject(p.getProjectID());
				}
			}
			
		});
	}

	public static void makeUnsubscribeItem(Menu parentMenu, Project p) {
		MenuItem unsub = new MenuItem(parentMenu, SWT.NONE);
		unsub.setText("Unsubscribe");
		unsub.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event arg0) {
				PluginManager.getInstance().getRequestManager().unsubscribeFromProject(p.getProjectID());
			}
		});
	}
	
}
