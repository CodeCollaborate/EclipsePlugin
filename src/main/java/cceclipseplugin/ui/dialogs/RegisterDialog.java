package cceclipseplugin.ui.dialogs;

import java.net.ConnectException;
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
		lblNewLabel.setText("Please fill in the details below:");

		Composite composite = new Composite(container, SWT.NONE);
		GridData gd_composite = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_composite.widthHint = 341;
		composite.setLayoutData(gd_composite);
		composite.setLayout(new GridLayout(2, false));

		Label lblUsername = new Label(composite, SWT.NONE);
		lblUsername.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblUsername.setText("Username");

		usernameBox = new Text(composite, SWT.BORDER);
		usernameBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblFirstName = new Label(composite, SWT.NONE);
		lblFirstName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblFirstName.setText("First Name");

		firstNameBox = new Text(composite, SWT.BORDER);
		firstNameBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblLastName = new Label(composite, SWT.NONE);
		lblLastName.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblLastName.setText("Last Name");

		lastNameBox = new Text(composite, SWT.BORDER);
		lastNameBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblEmail = new Label(composite, SWT.NONE);
		lblEmail.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblEmail.setText("E-Mail");

		emailBox = new Text(composite, SWT.BORDER);
		emailBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblPassword = new Label(composite, SWT.NONE);
		lblPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPassword.setText("Password");

		passwordBox = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		passwordBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblConfirmPassword = new Label(composite, SWT.NONE);
		lblConfirmPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblConfirmPassword.setText("Confirm Password");

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
		btnCreateAccountAnd.setText("Register");
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

		Request registerReq = (new UserRegisterRequest(usernameBox.getText(), firstNameBox.getText(),
				lastNameBox.getText(), emailBox.getText(), passwordBox.getText())).getRequest(response -> {
					if (response.getStatus() != 200) {
						MessageDialog err = new MessageDialog(getShell(),
								"User registration failed with status code " + response.getStatus() + ".");
						err.open();
						return;
					} else {
						MessageDialog msg = new MessageDialog(getShell(), "Registration successful!");
						msg.open();

						// Send login request
						Request loginReq = (new UserLoginRequest(usernameBox.getText(), passwordBox.getText()))
								.getRequest(loginResponse -> {
							if (loginResponse.getStatus() != 200) {
								MessageDialog err = new MessageDialog(getShell(), "Login failed with status code "
										+ loginResponse.getStatus() + ". Please try again later.");
								err.open();
							} else {
								MessageDialog err = new MessageDialog(getShell(), "Login successful!");
								err.open();
							}

							waiter.release();
						} , new UIRequestErrorHandler(getShell(), "Failed to send user login request."));
						try {
							PluginManager.getInstance().getWSManager().sendRequest(loginReq);
							if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
								MessageDialog errDialog = new MessageDialog(getShell(), "Request timed out.");
								errDialog.open();
							}
						} catch (InterruptedException e) {
							String message = e.getMessage();
							MessageDialog errDialog = new MessageDialog(getShell(), message);
							errDialog.open();
						}

					}

					waiter.release();
				} , new UIRequestErrorHandler(getShell(), "Failed to send user register request."));

		try {
			PluginManager.getInstance().getWSManager().sendRequest(registerReq);
			if (!waiter.tryAcquire(2, 5, TimeUnit.SECONDS)) {
				MessageDialog errDialog = new MessageDialog(getShell(), "Request timed out.");
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
		shell.setText("CodeCollaborate - Register");
	}
}
