package cceclipseplugin.ui.dialogs;

import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.TableEditor;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Table;
import websocket.models.Permission;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.ProjectGrantPermissionsRequest;
import websocket.models.requests.ProjectRenameRequest;
import websocket.models.requests.ProjectRevokePermissionsRequest;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.wb.swt.SWTResourceManager;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.RequestConfigurations;
import cceclipseplugin.ui.UIRequestErrorHandler;
import cceclipseplugin.ui.UIResponseHandler;

import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class ProjectSettingsDialog extends Dialog {

	protected Object result;
	protected Shell shlCodecollbaorateProject;
	private Text projectNameText;
	private Table table;
	
	private Project project;

	private String projectName;
	private String creationDate;
	private HashMap<String, Permission> users = new HashMap<>();
	private HashMap<String, Integer> usersToAdd = new HashMap<>();
	private List<String> usersToRemove;
	private HashMap<Integer, String> permissionConstants = new HashMap<>();
	private HashMap<String, Combo> permissionCombos = new HashMap<>();
	
	/**
	 * Create the dialog.
	 * @param parent
	 * @param style
	 */
	public ProjectSettingsDialog(Shell parent, int style) {
		super(parent, style);
		setText(DialogStrings.ProjectSettingsDialog_Title);
		projectName = StringConstants.NOT_INITIALIZED;
		creationDate = StringConstants.NOT_INITIALIZED;
		users = new HashMap<>();
		users.put(StringConstants.NOT_INITIALIZED, new Permission("", -1, "", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	public ProjectSettingsDialog(Shell parent, int style, Project p) {
		super(parent, style);
		projectName = p.getName();
		creationDate = "TODO: add creation date to Project class"; //$NON-NLS-1$
		users = p.getPermissions();
		project = p;
	}

	// Temporary
	public void Display(String projectName, String creationDate, HashMap<String, Permission> users) {
		this.projectName = projectName;
		this.creationDate = creationDate;
		this.users = users;
		open();
	}
	
	/**
	 * Open the dialog.
	 * @return the result
	 */
	public Object open() {
		createContents();
		shlCodecollbaorateProject.open();
		shlCodecollbaorateProject.layout();
		Display display = getParent().getDisplay();
		while (!shlCodecollbaorateProject.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		return result;
	}

	/**
	 * Create contents of the dialog.
	 */
	@SuppressWarnings("rawtypes")
	private void createContents() {
		shlCodecollbaorateProject = new Shell(getParent(), getStyle());
		shlCodecollbaorateProject.setSize(521, 365);
		shlCodecollbaorateProject.setText(DialogStrings.ProjectSettingsDialog_Title);
		shlCodecollbaorateProject.setLayout(new GridLayout(2, false));
		new Label(shlCodecollbaorateProject, SWT.NONE);
		new Label(shlCodecollbaorateProject, SWT.NONE);
		new Label(shlCodecollbaorateProject, SWT.NONE);
		
		Composite composite = new Composite(shlCodecollbaorateProject, SWT.NONE);
		GridData gd_composite = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_composite.widthHint = 459;
		composite.setLayoutData(gd_composite);
		
		Label lblProjectName = new Label(composite, SWT.NONE);
		lblProjectName.setBounds(0, 3, 76, 15);
		lblProjectName.setText(DialogStrings.ProjectSettingsDialog_ProjectNameLabel);
		
		projectNameText = new Text(composite, SWT.BORDER);
		projectNameText.setBounds(82, 0, 352, 21);
		projectNameText.setText(projectName);
		new Label(shlCodecollbaorateProject, SWT.NONE);
		
		Composite composite_1 = new Composite(shlCodecollbaorateProject, SWT.NONE);
		
		Label lblCreationDate = new Label(composite_1, SWT.NONE);
		lblCreationDate.setBounds(0, 0, 459, 15);
		lblCreationDate.setText(DialogStrings.ProjectSettingsDialog_CreationDateLabel + creationDate);
		new Label(shlCodecollbaorateProject, SWT.NONE);
		new Label(shlCodecollbaorateProject, SWT.NONE);
		new Label(shlCodecollbaorateProject, SWT.NONE);
		
		Composite composite_2 = new Composite(shlCodecollbaorateProject, SWT.NONE);
		GridData gd_composite_2 = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_composite_2.widthHint = 460;
		gd_composite_2.heightHint = 138;
		composite_2.setLayoutData(gd_composite_2);
		
		Composite composite_3 = new Composite(composite_2, SWT.NONE);
		composite_3.setBounds(427, 0, 31, 138);
		
		Button button = new Button(composite_3, SWT.CENTER);
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String newUser;
				int permission;
				AddNewUserDialog dialog = new AddNewUserDialog(shlCodecollbaorateProject);
				if (Window.OK == dialog.open()) {
					newUser = dialog.getNewUserName();
					permission = dialog.getNewUserPermission();
					if (usersToRemove.contains(newUser)) {
						usersToRemove.remove(newUser);
						users.put(newUser, new Permission(newUser, permission, null, null));
						// for now, assume permission level strings will be ints. 
						// should eventually look up their string mapping
						// other fields for constructor will be generated by server
					} else
						usersToAdd.put(newUser, permission);
					
					TableItem item = new TableItem(table, SWT.NONE);
					item.setText(newUser);
					TableEditor editor = new TableEditor(table);
					Combo c = createNewPermissionCombo(newUser, table);
					c.select(c.indexOf(permission + ""));
					editor.setEditor(c, item, 1);
				}
			}
		});
		button.setText("+"); //$NON-NLS-1$
		button.setFont(SWTResourceManager.getFont("Segoe UI", 9, SWT.BOLD)); //$NON-NLS-1$
		button.setBounds(0, 0, 30, 30);
		
		Button button_1 = new Button(composite_3, SWT.NONE);
		button_1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TableItem item = table.getItem(table.getSelectionIndex());
				String username = item.getText(0);
				RemoveUserDialog dialog = new RemoveUserDialog(shlCodecollbaorateProject, item.getText(0), project.getName());
				if (Window.OK == dialog.open()) {
					if (users.containsKey(username)) {
						users.remove(username);
						usersToRemove.add(username);
					} else if (usersToAdd.containsKey(username)) {
						usersToAdd.containsKey(username);
					} else {
						MessageDialog err = new MessageDialog(shlCodecollbaorateProject, DialogStrings.ProjectSettingsDialog_InternalError);
						err.open();
					}
				}
			}
		});
		button_1.setBounds(0, 36, 30, 30);
		button_1.setText("-"); //$NON-NLS-1$
		
		table = new Table(composite_2, SWT.BORDER | SWT.FULL_SELECTION);
		table.setLocation(0, 0);
		table.setSize(421, 138);
		table.setHeaderVisible(true);
		table.setLinesVisible(true);
		
		TableColumn tblclmnNewColumn = new TableColumn(table, SWT.NONE);
		tblclmnNewColumn.setWidth(124);
		tblclmnNewColumn.setText(DialogStrings.ProjectSettingsDialog_UserTableColumn);
		
		TableColumn tblclmnNewColumn_1 = new TableColumn(table, SWT.NONE);
		tblclmnNewColumn_1.setWidth(287);
		tblclmnNewColumn_1.setText(DialogStrings.ProjectSettingsDialog_PermissionTableColumn);
		
		
		for (Map.Entry entry : users.entrySet()) {
			TableItem item = new TableItem(table, SWT.NONE);
			item.setText((String) entry.getKey());
			int permLevel = ((Permission) entry.getValue()).getPermissionLevel();
			// TODO: Request PermissionLevel constants from server and figure out
			// Permission level name from permission map
			TableEditor editor = new TableEditor(table);
			Combo c = createNewPermissionCombo((String) entry.getKey(), table);
			c.select(c.indexOf(permLevel + "")); //$NON-NLS-1$
			editor.setEditor(c, item, 1);
		}
		
		new Label(shlCodecollbaorateProject, SWT.NONE);
		
		Group grpAdvancedOptions = new Group(shlCodecollbaorateProject, SWT.NONE);
		grpAdvancedOptions.setLayout(new GridLayout(2, false));
		GridData gd_grpAdvancedOptions = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_grpAdvancedOptions.heightHint = 37;
		gd_grpAdvancedOptions.widthHint = 456;
		grpAdvancedOptions.setLayoutData(gd_grpAdvancedOptions);
		grpAdvancedOptions.setText(DialogStrings.ProjectSettingsDialog_AdvancedOptionsLabel);
		
		Button btnDeleteProject = new Button(grpAdvancedOptions, SWT.NONE);
		btnDeleteProject.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DeleteProjectDialog dpd = new DeleteProjectDialog(shlCodecollbaorateProject, project);
				dpd.create();
				dpd.open();
			}
		});
		btnDeleteProject.setForeground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_FOREGROUND));
		btnDeleteProject.setText(DialogStrings.ProjectSettingsDialog_DeleteProjectButton);
		
		Button btnTransferOwnership = new Button(grpAdvancedOptions, SWT.NONE);
		btnTransferOwnership.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TransferOwnershipDialog dialog = new TransferOwnershipDialog(shlCodecollbaorateProject, (String[]) users.keySet().toArray());
				if (Window.OK == dialog.open()) {
					dialog.getNewOwner();
				}
			}
		});
		btnTransferOwnership.setText(DialogStrings.ProjectSettingsDialog_TransferOwnershipButton);
		new Label(shlCodecollbaorateProject, SWT.NONE);
		
		Composite composite_4 = new Composite(shlCodecollbaorateProject, SWT.NONE);
		composite_4.setLayout(null);
		GridData gd_composite_4 = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		gd_composite_4.widthHint = 460;
		composite_4.setLayoutData(gd_composite_4);
		
		Button btnCancel = new Button(composite_4, SWT.NONE);
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				shlCodecollbaorateProject.close();
			}
		});
		btnCancel.setBounds(402, 5, 48, 25);
		btnCancel.setText(DialogStrings.ProjectSettingsDialog_CancelButton);
		
		Button btnApplyChanges = new Button(composite_4, SWT.NONE);
		btnApplyChanges.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!projectNameText.getText().equals(project.getName()))
					sendNameChangeRequest(projectNameText.getText());
				if (!usersToAdd.isEmpty()) {
					for (String user : usersToAdd.keySet()) {
						Combo permissionCombo = permissionCombos.get(user);
						sendGrantPermissionRequest(user, permissionCombo.getItem(permissionCombo.getSelectionIndex()));
					}
				}
				if (!usersToRemove.isEmpty())
					for (String user : usersToRemove)
						sendRevokePermissionRequest(user);
				for (String user : users.keySet()) {
					Combo permissionCombo = permissionCombos.get(user);
					String comboPermissionLevel = permissionCombo.getItem(permissionCombo.getSelectionIndex());
					if (!(users.get(user).getPermissionLevel() + "").equals(comboPermissionLevel)) //$NON-NLS-1$
						sendGrantPermissionRequest(user, comboPermissionLevel);
				}
			}
		});
		btnApplyChanges.setBounds(304, 5, 92, 25);
		btnApplyChanges.setText(DialogStrings.ProjectSettingsDialog_ApplyChangesButton);

	}
	
	private void sendNameChangeRequest(String newName) {
		Semaphore waiter = new Semaphore(0);
		
		Request req = (new ProjectRenameRequest(project.getProjectID(), newName)).getRequest(
				new UIResponseHandler(shlCodecollbaorateProject, waiter, DialogStrings.ProjectSettingsDialog_ProjRenameRespHandlerMsg),
				new UIRequestErrorHandler(shlCodecollbaorateProject, DialogStrings.ProjectSettingsDialog_ProjRenameErr));
		try {
			PluginManager.getInstance().getWSManager().sendRequest(req);
			if (!waiter.tryAcquire(1, RequestConfigurations.REQUST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
	            MessageDialog errDialog = new MessageDialog(shlCodecollbaorateProject, DialogStrings.ProjectSettingsDialog_TimeoutErr);
	            errDialog.open();
			}
			
		} catch (InterruptedException e) {
			String message = e.getMessage();
			MessageDialog errDialog = new MessageDialog(shlCodecollbaorateProject, message);
			errDialog.open();
		}
	}
	
	private void sendGrantPermissionRequest(String user, String permission) {
		Semaphore waiter = new Semaphore(0);
		
		Request req = (new ProjectGrantPermissionsRequest(project.getProjectID(), user, Integer.parseInt(permission))).getRequest(
				new UIResponseHandler(shlCodecollbaorateProject, waiter, DialogStrings.ProjectSettingsDialog_ProjPermissionRespHandlerMsg),
				new UIRequestErrorHandler(shlCodecollbaorateProject, DialogStrings.ProjectSettingsDialog_GrantPermissionErr));
		try {
			PluginManager.getInstance().getWSManager().sendRequest(req);
			if (!waiter.tryAcquire(1, RequestConfigurations.REQUST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
	            MessageDialog errDialog = new MessageDialog(shlCodecollbaorateProject, DialogStrings.ProjectSettingsDialog_TimeoutErr);
	            errDialog.open();
			}
			
		} catch (InterruptedException e) {
			String message = e.getMessage();
			MessageDialog errDialog = new MessageDialog(shlCodecollbaorateProject, message);
			errDialog.open();
		}
	}
	
	private void sendRevokePermissionRequest(String user) {
		Semaphore waiter = new Semaphore(0);
		
		Request req = (new ProjectRevokePermissionsRequest(project.getProjectID(), 0)).getRequest( // TODO: CHANGE THIS after request model is fixed
				new UIResponseHandler(shlCodecollbaorateProject, waiter, DialogStrings.ProjectSettingsDialog_PermissionRevokeRespHandlerMsg),
				new UIRequestErrorHandler(shlCodecollbaorateProject, DialogStrings.ProjectSettingsDialog_RevokePermissionErr));
		try {
			PluginManager.getInstance().getWSManager().sendRequest(req);
			if (!waiter.tryAcquire(1, RequestConfigurations.REQUST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
	            MessageDialog errDialog = new MessageDialog(shlCodecollbaorateProject, DialogStrings.ProjectSettingsDialog_TimeoutErr);
	            errDialog.open();
			}
			
		} catch (InterruptedException e) {
			String message = e.getMessage();
			MessageDialog errDialog = new MessageDialog(shlCodecollbaorateProject, message);
			errDialog.open();
		}
	}
	
	private Combo createNewPermissionCombo(String user, Composite parent) {
		Combo combo = new Combo(parent, SWT.NONE);
		
		for(int i : permissionConstants.keySet()) {
			combo.add(i + ""); //$NON-NLS-1$
		}
		permissionCombos.put(user, combo);
		
		return combo;
	}
}
