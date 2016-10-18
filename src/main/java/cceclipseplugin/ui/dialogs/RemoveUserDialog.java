package cceclipseplugin.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.UIRequestErrorHandler;
import websocket.models.Request;
import websocket.models.requests.ProjectRevokePermissionsRequest;

import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class RemoveUserDialog extends Dialog {

	private String username;
	private String projectName;
	private long projectId;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 * @wbp.parser.constructor
	 */
	public RemoveUserDialog(Shell parentShell) {
		super(parentShell);
	}

	public RemoveUserDialog(Shell parentShell, String username, String projectName, long projectId) {
		super(parentShell);
		this.username = username;
		this.projectName = projectName;
		this.projectId = projectId;
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);

		Label lblAreYouSure = new Label(container, SWT.WRAP | SWT.CENTER);
		GridData gd_lblAreYouSure = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		lblAreYouSure.setLayoutData(gd_lblAreYouSure);
		lblAreYouSure.setText(DialogStrings.RemoveUserDialog_AreYouSure1 + username + DialogStrings.RemoveUserDialog_AreYouSure2 + projectName + DialogStrings.RemoveUserDialog_AreYouSure3);

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
		// TODO: move to okPRessed()
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Request removeUserRequest = (new ProjectRevokePermissionsRequest(projectId, username)).getRequest((response) -> {
					if (response.getStatus() == 200) {
						PluginManager.getInstance().getRequestManager().fetchProjects();
					} else {
						MessageDialog err = new MessageDialog(getShell(), "Server error revoking permissions for: "+username+" "+projectName);
						getShell().getDisplay().asyncExec(() -> err.open());
					}
				}, new UIRequestErrorHandler("Error sending revoke permissions request for: "+username+" "+projectName));
				PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(removeUserRequest);
			}
		});
		button.setText(DialogStrings.RemoveUserDialog_RemoveButton);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText(DialogStrings.RemoveUserDialog_Title);
	}

}
