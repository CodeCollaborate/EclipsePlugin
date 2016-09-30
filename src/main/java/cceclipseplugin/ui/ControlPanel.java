package cceclipseplugin.ui;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import cceclipseplugin.core.PluginManager;

public class ControlPanel extends ViewPart {

	private Label lblInsert;
	protected ListViewer projectsListViewer;
	protected ListViewer usersListViewer;
	private DataBindingContext m_bindingContext;

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
		
		StatusBar statusBar = new StatusBar(parent, SWT.BORDER);
		GridData statusData = new GridData();
		statusData.grabExcessHorizontalSpace = true;
		statusData.horizontalAlignment = GridData.FILL;
		statusBar.setLayoutData(statusData);
		
		PluginManager.getInstance().getUIManager().popupWelcomePrompt();
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}
}
