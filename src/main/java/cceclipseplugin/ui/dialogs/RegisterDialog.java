package cceclipseplugin.ui.dialogs;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Text;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.RequestConfigurations;
import cceclipseplugin.ui.UIRequestErrorHandler;
import websocket.models.Request;
import websocket.models.requests.UserRegisterRequest;

import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class RegisterDialog extends Dialog {
	private Text usernameBox;
	private Text firstNameBox;
	private Text lastNameBox;
	private Text emailBox;
	private Text passwordBox;
	private Text confirmPasswordBox;
	private Button okButton;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public RegisterDialog(Shell parentShell) {
		super(parentShell);
		setShellStyle(SWT.DIALOG_TRIM);
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);

		Label lblNewLabel = new Label(container, SWT.NONE);
		lblNewLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1));
		lblNewLabel.setText(DialogStrings.RegisterDialog_InstructionsLabel);

		Composite composite = new Composite(container, SWT.NONE);
		GridData gd_composite = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		composite.setLayoutData(gd_composite);
		composite.setLayout(new GridLayout(2, false));

		Label lblUsername = new Label(composite, SWT.NONE);
		lblUsername.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblUsername.setText(DialogStrings.RegisterDialog_UsernameLabel);

		usernameBox = new Text(composite, SWT.BORDER);
		usernameBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblFirstName = new Label(composite, SWT.NONE);
		lblFirstName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblFirstName.setText(DialogStrings.RegisterDialog_FirstNameLabel);

		firstNameBox = new Text(composite, SWT.BORDER);
		firstNameBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblLastName = new Label(composite, SWT.NONE);
		lblLastName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblLastName.setText(DialogStrings.RegisterDialog_LastNameLabel);

		lastNameBox = new Text(composite, SWT.BORDER);
		lastNameBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblEmail = new Label(composite, SWT.NONE);
		lblEmail.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblEmail.setText(DialogStrings.RegisterDialog_EmailLabel);

		emailBox = new Text(composite, SWT.BORDER);
		emailBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblPassword = new Label(composite, SWT.NONE);
		lblPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPassword.setText(DialogStrings.RegisterDialog_PasswordLabel);

		passwordBox = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		passwordBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblConfirmPassword = new Label(composite, SWT.NONE);
		lblConfirmPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblConfirmPassword.setText(DialogStrings.RegisterDialog_ConfirmPasswordLabel);

		confirmPasswordBox = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		confirmPasswordBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
				
		ModifyListener listener = (modifyEvent) -> {
			if (!usernameBox.getText().equals("") && !passwordBox.getText().equals("") && 
					!firstNameBox.getText().equals("") && !lastNameBox.getText().equals("") &&
					!emailBox.getText().equals("") && !passwordBox.getText().equals("") &&
					passwordBox.getText().equals(confirmPasswordBox.getText())) {
				okButton.setEnabled(true);
			} else {
				okButton.setEnabled(false);
			}
		};
		usernameBox.addModifyListener(listener);
		passwordBox.addModifyListener(listener);
		firstNameBox.addModifyListener(listener);
		lastNameBox.addModifyListener(listener);
		emailBox.addModifyListener(listener);
		passwordBox.addModifyListener(listener);
		confirmPasswordBox.addModifyListener(listener);
		
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
		okButton.setText(DialogStrings.RegisterDialog_RegisterButton);
		okButton.setEnabled(false);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void okPressed() {
		// TODO: Let user know that passwords aren't equal and disable ok button until they are
		if (!passwordBox.getText().equals(confirmPasswordBox.getText()))
			return;
		Semaphore waiter = new Semaphore(0);
		
		String username = usernameBox.getText();
		String password = passwordBox.getText();
		String email = emailBox.getText();
		String firstName = firstNameBox.getText();
		String lastName = lastNameBox.getText();

		Request registerReq = (new UserRegisterRequest(username, firstName,
				lastName, email, password)).getRequest(response -> {
					if (response.getStatus() != 200) {
						MessageDialog err = new MessageDialog(getShell(),
								DialogStrings.RegisterDialog_UserRegistrationErr + response.getStatus() + "."); //$NON-NLS-2$
						err.open();
						return;
					} else {
						MessageDialog msg = new MessageDialog(getShell(), DialogStrings.RegisterDialog_RegistrationSuccessMsg);
						msg.open();

						// Send login request
						PluginManager.getInstance().getRequestManager().loginAndSubscribe(username, password);
					}

					waiter.release();
				} , new UIRequestErrorHandler(DialogStrings.RegisterDialog_UserRegisterErr));

		try {
			PluginManager.getInstance().getWSManager().sendRequest(registerReq);
			if (!waiter.tryAcquire(1, RequestConfigurations.REQUST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
				MessageDialog errDialog = new MessageDialog(getShell(), DialogStrings.RegisterDialog_TimeoutErr);
				errDialog.open();
			}
		} catch (InterruptedException e) {
			String message = e.getMessage();
			MessageDialog errDialog = new MessageDialog(getShell(), message);
			errDialog.open();
		}

		super.okPressed();
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(DialogStrings.RegisterDialog_Title);
	}
}
