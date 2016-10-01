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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.PermissionMap;
import cceclipseplugin.ui.UIRequestErrorHandler;
import websocket.models.Request;
import websocket.models.requests.UserLookupRequest;
import websocket.models.responses.UserLookupResponse;

public class AddNewUserDialog extends Dialog {
	private Text text;
	private CCombo combo;
	private Label errorLabel;
	private String username;
	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public AddNewUserDialog(Shell parentShell) {
		super(parentShell);
		parentShell.setText("Add New User");
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
		lblAddANew.setText("Add a new user to this project by Username or Email:");

		text = new Text(container, SWT.BORDER);
		GridData gd_text = new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1);
		gd_text.widthHint = 292;
		text.setLayoutData(gd_text);

		combo = new CCombo(container, SWT.BORDER);
		combo.setEditable(false);
		combo.setText("Choose a permission level");
		GridData gd_combo = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		gd_combo.widthHint = 300;
		combo.setLayoutData(gd_combo);
		// TODO: Make Combo populate with server permission levels once implemented
		for (Map.Entry<Integer, String> e : PermissionMap.permissions.entrySet()) {
			combo.add(e.getKey() + " : " + e.getValue());
		}
		//
		// Request permLevelReq = new Request("Project",
		// "GetPermissionConstants", );
		// try {
		// PluginManager.getInstance().getWSManager().sendRequest(permLevelReq);
		// } catch (ConnectException e) {
		// ErrorDialog err = new ErrorDialog(getShell(), e.getMessage());
		// err.open();
		// close();
		// }

		errorLabel = new Label(container, SWT.NONE);
		errorLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		errorLabel.setText("<error label>");
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
		Button button = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		button.setText("Add");
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(383, 199);
	}

	@Override
	protected void okPressed() {
		Semaphore waiter = new Semaphore(0);

		Request userLookupReq = (new UserLookupRequest(new String[] { text.getText() })).getRequest(
				response -> {

					int status = response.getStatus();
					if (status != 200) {
						errorLabel.setText("Failed with status code " + status + ".");
						errorLabel.setVisible(true);
					} else {
						username = ((UserLookupResponse) response.getData()).getUsers()[0].getUsername();
						errorLabel.setVisible(false);
					}
					waiter.release();
				},
				new UIRequestErrorHandler(getShell(), "Could not send user lookup request."));
		
		try {
			PluginManager.getInstance().getWSManager().sendRequest(userLookupReq);
			if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
	            MessageDialog errDialog = new MessageDialog(getShell(), "Request timed out.");
	            errDialog.open();
			}
		} catch (InterruptedException ex) {
			// ErrorDialog err = new ErrorDialog(getShell(), ex.getMessage());
			MessageDialog err = new MessageDialog(getShell(), ex.getMessage());
			err.open();
		}
		
		if (username != null)
			super.okPressed();
	}

	public String getNewUserName() {
		return username;
	}
	
	public String getNewUserPermission() {
		return combo.getItem(combo.getSelectionIndex());
	}

	@Override
	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText("CodeCollaborate - Add New User");
	}
}
