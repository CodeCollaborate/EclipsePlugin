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
import websocket.models.Request;
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
	private BiMap<String, Byte> permissionMap;
	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public AddNewUserDialog(Shell parentShell) {
		super(parentShell);
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

		text = new Text(container, SWT.BORDER);
		GridData gd_text = new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1);
		gd_text.widthHint = 292;
		text.setLayoutData(gd_text);

		combo = new CCombo(container, SWT.BORDER);
		combo.setEditable(false);
		combo.setText(DialogStrings.AddNewUserDialog_ChoosePermission);
		GridData gd_combo = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		gd_combo.widthHint = 300;
		combo.setLayoutData(gd_combo);
		
		permissionMap = PluginManager.getInstance().getDataManager().getSessionStorage().getPermissionConstants();
		if (permissionMap == null || permissionMap.isEmpty())
			permissionMap = PermissionMap.permissions;
		
		for (Map.Entry<String, Byte> e : permissionMap.entrySet()) {
			combo.add(e.getValue() + " : " + e.getKey()); //$NON-NLS-1$
		}
		combo.addListener(SWT.Selection, new Listener() {

			@Override
			public void handleEvent(Event arg0) {
				okButton.setEnabled(true);
			}
			
		});		
		
		errorLabel = new Label(container, SWT.NONE);
		errorLabel.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		GridData gd_errorLabel = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
		gd_errorLabel.widthHint = 355;
		gd_errorLabel.heightHint = 21;
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

		permission = Integer.parseInt(combo.getItem(combo.getSelectionIndex()).split(" . ")[0]);
		
		// TODO: Potentially get rid of lookup request and move GrantPermissionsRequest to here
		Request userLookupReq = (new UserLookupRequest(new String[] { text.getText() })).getRequest(
				response -> {

					int status = response.getStatus();
					if (status != 200) {
						Display.getDefault().asyncExec(() -> errorLabel.setText("User does not exist."));
						Display.getDefault().asyncExec(() -> errorLabel.setVisible(true));
					} else {
						username = ((UserLookupResponse) response.getData()).getUsers()[0].getUsername();
					}
					waiter.release();
				},
				new UIRequestErrorHandler(new Shell(), DialogStrings.AddNewUserDialog_UserLookupErr));
		
		try {
			PluginManager.getInstance().getWSManager().sendRequest(userLookupReq);
			if (!waiter.tryAcquire(1, RequestConfigurations.REQUST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
	            MessageDialog errDialog = new MessageDialog(new Shell(), DialogStrings.AddNewUserDialog_TimeoutErr);
	            Display.getDefault().asyncExec(() -> errDialog.open());
			}
		} catch (InterruptedException ex) {
			MessageDialog err = new MessageDialog(new Shell(), ex.getMessage());
			Display.getDefault().asyncExec(() -> err.open());
		}
		
		if (username != null)
			super.okPressed();
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
