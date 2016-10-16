package cceclipseplugin.ui;

import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import cceclipseplugin.constants.StringConstants;
import cceclipseplugin.core.PluginManager;
import websocket.WSConnection;
import websocket.WSConnection.State;

public class ControlPanel extends ViewPart {

	protected ListViewer projectsListViewer;
	protected ListViewer usersListViewer;
	private StatusBar statusBar;
	private ListViewsParent views;
	
	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		parent.setLayout(layout);
		
		views = new ListViewsParent(parent, SWT.NONE);
		GridData viewsData = new GridData();
		viewsData.grabExcessHorizontalSpace = true;
		viewsData.horizontalAlignment = GridData.FILL;
		viewsData.grabExcessVerticalSpace = true;
		viewsData.verticalAlignment = GridData.FILL;
		views.setLayoutData(viewsData);
		
		statusBar = new StatusBar(parent, SWT.BORDER);
		GridData statusData = new GridData();
		statusData.grabExcessHorizontalSpace = true;
		statusData.horizontalAlignment = GridData.FILL;
		statusBar.setLayoutData(statusData);
		initializeStatusBarListeners();
	}
	
	private void initializeStatusBarListeners() {
		PluginManager.getInstance().getWSManager().registerEventHandler(WSConnection.EventType.ON_CONNECT, () -> {
			statusBar.getDisplay().asyncExec(() -> statusBar.setStatus("Connected to CodeCollaborate server."));
		});
		PluginManager.getInstance().getWSManager().registerEventHandler(WSConnection.EventType.ON_CLOSE, () -> {
			statusBar.getDisplay().asyncExec(() -> statusBar.setStatus("Disconnected from CodeCollaborate server."));
		});
		PluginManager.getInstance().getWSManager().registerEventHandler(WSConnection.EventType.ON_ERROR, () -> {
			statusBar.getDisplay().asyncExec(() -> statusBar.setStatus("Error on CodeCollaborate server."));
		});
		State s = PluginManager.getInstance().getWSManager().getConnectionState();
		switch (s) {
		case CLOSE:
			statusBar.setStatus(StringConstants.CLOSE_MESSAGE);
			break;
		case CONNECT:
			statusBar.setStatus(StringConstants.CONNECT_MESSAGE);
			break;
		case ERROR:
			statusBar.setStatus(StringConstants.ERROR_MESSAGE);
			break;
		default:
			System.out.println(s);
			break;
		}
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}
	
	public void setEnabled(boolean b) {
		views.getProjectListView().getListWithButtons().setEnabled(b);
		views.getUserListView().getListWithButtons().setEnabled(b);
	}
}
