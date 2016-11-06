package cceclipseplugin.ui.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import cceclipseplugin.core.PluginManager;
import websocket.models.Project;
import org.eclipse.wb.swt.SWTResourceManager;

import com.google.common.collect.BiMap;

public class AddNewUserDialog extends Dialog {
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
		BiMap<Byte, String> inversePermissionMap = permissionMap.inverse();
		@SuppressWarnings("unchecked")
		List<Byte> permissionCodes = new ArrayList<>(permissionMap.values());
		Collections.sort(permissionCodes);
		for (Byte b : permissionCodes) {
			combo.add(b + " : " + inversePermissionMap.get(b));
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
		if (username != null && permission != -1) {
			PluginManager.getInstance().getRequestManager()
				.addUserToProject(selectedProject.getProjectID(), username, permission);
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
