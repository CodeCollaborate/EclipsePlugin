package cceclipseplugin.ui.dialogs;

import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.PermissionMap;
import cceclipseplugin.ui.RequestConfigurations;
import cceclipseplugin.ui.UIRequestErrorHandler;
import websocket.models.Permission;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.ProjectGrantPermissionsRequest;
import websocket.models.requests.UserLookupRequest;
import websocket.models.responses.UserLookupResponse;
import org.eclipse.wb.swt.SWTResourceManager;

import com.google.common.collect.BiMap;

public class AddNewUserDialog extends Dialog {
	private Text text;
	private CCombo combo;
	private Label errorLabel;
	private String username;
	private int permission;
	private Button okButton;
	private Project selectedProject;
	private BiMap<String, Byte> permissionMap;
	private Text usernameBox;
	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public AddNewUserDialog(Shell parentShell, Project selectedProject) {
		super(parentShell);
		this.selectedProject = selectedProject;
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		Label lblAddANew = new Label(container, SWT.NONE);
		lblAddANew.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		lblAddANew.setText(DialogStrings.AddNewUserDialog_AddByUsername);

		usernameBox = new Text(container, SWT.BORDER);
		GridData gd_text = new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1);
		usernameBox.setLayoutData(gd_text);

		combo = new CCombo(container, SWT.BORDER);
		combo.setEditable(false);
		combo.setText(DialogStrings.AddNewUserDialog_ChoosePermission);
		GridData gd_combo = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		combo.setLayoutData(gd_combo);
		
		permissionMap = PluginManager.getInstance().getDataManager().getSessionStorage().getPermissionConstants();
		if (permissionMap == null || permissionMap.isEmpty())
			permissionMap = PermissionMap.permissions;
		
		for (Map.Entry<String, Byte> e : permissionMap.entrySet()) {
			combo.add(e.getValue() + " : " + e.getKey()); //$NON-NLS-1$
		}
		final boolean[] permissionSelected = {false};
		final boolean[] usernameNotEmpty = {false};
		combo.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				permissionSelected[0] = true;
				if (permissionSelected[0] && usernameNotEmpty[0]) {
					okButton.setEnabled(true);
				} else {
					okButton.setEnabled(false);
				}
			}
		});		
		
		usernameBox.addModifyListener((event) -> {
			if (usernameBox.getText() != "") {
				usernameNotEmpty[0] = true;
			} else {
				usernameNotEmpty[0] = false;
			}
			if (permissionSelected[0] && usernameNotEmpty[0]) {
				okButton.setEnabled(true);
			} else {
				okButton.setEnabled(false);
			}
		});
		
		errorLabel = new Label(container, SWT.NONE);
		errorLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		GridData gd_errorLabel = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		errorLabel.setLayoutData(gd_errorLabel);
		errorLabel.setText(DialogStrings.AddNewUserDialog_ErrLabelPlaceholder);
		errorLabel.setVisible(false);

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		okButton.setText(DialogStrings.AddNewUserDialog_AddButton);
		okButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void okPressed() {
		username = usernameBox.getText();
		permission = Integer.parseInt(combo.getItem(combo.getSelectionIndex()).split(" . ")[0]);
		System.out.println("username: "+username+" permy: "+permission);
		if (username != null && permission != -1) {
			Request req = new ProjectGrantPermissionsRequest(selectedProject.getProjectID(), username, permission).getRequest((response) -> {
				if (response.getStatus() == 200) {
					// TODO: remove this when server supports notifications to the person who sent the request
					Project proj = PluginManager.getInstance().getDataManager().getSessionStorage()
							.getProjectById(selectedProject.getProjectID());
					proj.getPermissions().put(username, new Permission(username, permission, null, null));
					PluginManager.getInstance().getDataManager().getSessionStorage().setProject(proj);
				} else {
					MessageDialog err = new MessageDialog(getParentShell(), "Error granting permissions: "+response.getStatus());
					getShell().getDisplay().asyncExec(() -> err.open());
				}
			}, new UIRequestErrorHandler("Could not send request to grant permissions."));
			PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
			super.okPressed();
		}
	}

	public String getNewUserName() {
		return username;
	}
	
	public int getNewUserPermission() {
		return permission;
	}

	@Override
	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText(DialogStrings.AddNewUserDialog_Title);
	}
}
