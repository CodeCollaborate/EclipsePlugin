package cceclipseplugin.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;

public class ListViewsParent extends Composite {
	
	private ProjectsListView projectListView;
	private UsersListView userListView;

	public ListViewsParent(Composite parent, int style) {
		super(parent, style);
		this.initialize();
	}
	
	private void initialize() {
		FormLayout formLayout = new FormLayout();
		this.setLayout(formLayout);
		this.projectListView = this.createProjectsList();
		this.userListView = this.createUsersList(this.projectListView);
	}
	
	private ProjectsListView createProjectsList() {
		ProjectsListView listView = new ProjectsListView(this, SWT.BORDER);
		FormData data = new FormData();
		data.top = new FormAttachment(0);
		data.left = new FormAttachment(0);
		data.right = new FormAttachment(40);
		data.bottom = new FormAttachment(100);
		listView.setLayoutData(data);
		return listView;
	}
	
	private UsersListView createUsersList(ProjectsListView projectsListView) {
		UsersListView listView = new UsersListView(this, SWT.BORDER, projectListView);
		FormData data = new FormData();
		data.top = new FormAttachment(0);
		data.left = new FormAttachment(projectListView);
		data.right = new FormAttachment(100);
		data.bottom = new FormAttachment(100);
		listView.setLayoutData(data);
		return listView;
	}
	
	public ProjectsListView getProjectListView() {
		return projectListView;
	}
	
	public UsersListView getUserListView() {
		return userListView;
	}
}
