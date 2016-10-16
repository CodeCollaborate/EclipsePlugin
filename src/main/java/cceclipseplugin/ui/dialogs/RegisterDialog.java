package cceclipseplugin.ui.dialogs;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Text;

import cceclipseplugin.Activator;
import cceclipseplugin.core.PluginManager;
import cceclipseplugin.preferences.PreferenceConstants;
import cceclipseplugin.ui.ControlPanel;
import cceclipseplugin.ui.RequestConfigurations;
import cceclipseplugin.ui.UIRequestErrorHandler;
import websocket.models.Request;
import websocket.models.requests.UserLoginRequest;
import websocket.models.requests.UserRegisterRequest;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class RegisterDialog extends Dialog {
	private Text usernameBox;
	private Text firstNameBox;
	private Text lastNameBox;
	private Text emailBox;
	private Text passwordBox;
	private Text confirmPasswordBox;

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
		gd_composite.widthHint = 341;
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

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		Button btnCreateAccountAnd = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		btnCreateAccountAnd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		btnCreateAccountAnd.setText(DialogStrings.RegisterDialog_RegisterButton);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(366, 300);
	}

	@Override
	protected void okPressed() {
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
						Request loginReq = (new UserLoginRequest(username, password))
								.getRequest(loginResponse -> {
							if (loginResponse.getStatus() != 200) {
								MessageDialog err = new MessageDialog(getShell(), DialogStrings.RegisterDialog_LoginFailedWithStatus
										+ loginResponse.getStatus() + DialogStrings.RegisterDialog_TryAgainMsg);
								getShell().getDisplay().asyncExec(() -> err.open());
							} else {
								MessageDialog err = new MessageDialog(getShell(), DialogStrings.RegisterDialog_LoginSuccessMsg);
								getShell().getDisplay().asyncExec(() -> err.open());
							}

							waiter.release();
						} , new UIRequestErrorHandler(getShell(), DialogStrings.RegisterDialog_UserLoginErr));
						try {
							PluginManager.getInstance().getWSManager().sendRequest(loginReq);
							if (!waiter.tryAcquire(1, RequestConfigurations.REQUST_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
								MessageDialog errDialog = new MessageDialog(getShell(), DialogStrings.RegisterDialog_TimeoutErr);
								getShell().getDisplay().asyncExec(() -> errDialog.open());
							}
						} catch (InterruptedException e) {
							String message = e.getMessage();
							MessageDialog errDialog = new MessageDialog(getShell(), message);
							getShell().getDisplay().asyncExec(() -> errDialog.open());
							return;
						}
						
						IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
						prefStore.setValue(PreferenceConstants.USERNAME, username);
						prefStore.setValue(PreferenceConstants.PASSWORD, password);
						
						ControlPanel cp = (ControlPanel) PluginManager.getInstance().getUIManager().getControlView();
						Display.getDefault().asyncExec(() -> cp.setEnabled(true));
					}

					waiter.release();
				} , new UIRequestErrorHandler(getShell(), DialogStrings.RegisterDialog_UserRegisterErr));

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
