package cceclipseplugin.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import cceclipseplugin.core.PluginManager;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;

public class RecoverPasswordDialog extends Dialog {
	
	private Text usernameBox;
	private Button okButton;
	
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public RecoverPasswordDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));
		
		Label recoveryMessage = new Label(container, SWT.WRAP | SWT.CENTER);
		GridData gd_recoveryMessage = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		gd_recoveryMessage.widthHint = 500;
		recoveryMessage.setLayoutData(gd_recoveryMessage);
		recoveryMessage.setText(DialogStrings.RecoverPasswordDialog_Message);

		usernameBox = new Text(container, SWT.BORDER);
		GridData gd_text = new GridData(SWT.CENTER, SWT.CENTER, true, false, 1, 1);
		gd_text.widthHint = 100;
		usernameBox.setLayoutData(gd_text);
		final boolean[] usernameNotEmpty = {false};
		usernameBox.addModifyListener((event) -> {
			if (usernameBox.getText() != "") {
				usernameNotEmpty[0] = true;
			} else {
				usernameNotEmpty[0] = false;
			}
			if (usernameNotEmpty[0]) {
				okButton.setEnabled(true);
			} else {
				okButton.setEnabled(false);
			}
		});
		
		return container;
	}
	
	@Override 
	protected void okPressed() {		
//		PluginManager.getInstance().getRequestManager().sendRecoveryEmail(usernameBox.getText());
		MessageDialog.createDialog(DialogStrings.RecoverPasswordDialog_ThankYou, SWT.COLOR_BLACK).open();
		super.okPressed();
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		okButton.setText(DialogStrings.RecoverPasswordDialog_SendRequest);
		okButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText(DialogStrings.RecoverPasswordDialog_Title);
	}

}
