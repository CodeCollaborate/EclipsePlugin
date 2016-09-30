package cceclipseplugin.ui;

import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

import cceclipseplugin.core.PluginManager;
import websocket.WSConnection;

public class ControlPanel extends ViewPart {

	protected ListViewer projectsListViewer;
	protected ListViewer usersListViewer;
	private StatusBar statusBar;
	
	@Override
	public void createPartControl(Composite parent) {
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		parent.setLayout(layout);
		
		ListViewsParent views = new ListViewsParent(parent, SWT.NONE);
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
		
		PluginManager.getInstance().getUIManager().popupWelcomePrompt();
	}
	
	private void initializeStatusBarListeners() {
		PluginManager.getInstance().getWSManager().registerEventHandler(WSConnection.EventType.ON_CONNECT, () -> {
			System.out.println("CONNECT");
			statusBar.setStatus("Connected to CodeCollaborate server.");
		});
		
		PluginManager.getInstance().getWSManager().registerEventHandler(WSConnection.EventType.ON_CLOSE, () -> {
			System.out.println("CLOSE");
			statusBar.setStatus("Disconnected from CodeCollaborate server.");
		});
		
		PluginManager.getInstance().getWSManager().registerEventHandler(WSConnection.EventType.ON_ERROR, () -> {
			System.out.println("ERROR");
			statusBar.setStatus("Error on CodeCollaborate server.");
		});
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}
}
