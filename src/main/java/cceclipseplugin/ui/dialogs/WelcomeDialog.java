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

import org.eclipse.swt.widgets.Button;

public class WelcomeDialog extends Dialog {
	private Text usernameBox;
	private Text passwordBox;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public WelcomeDialog(Shell parentShell) {
		super(parentShell);
		parentShell.setText("Welcome to CodeCollaborate!");
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);

		Label lblNewLabel = new Label(container, SWT.WRAP | SWT.CENTER);
		GridData gd_lblNewLabel = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		gd_lblNewLabel.widthHint = 288;
		gd_lblNewLabel.heightHint = 29;
		lblNewLabel.setLayoutData(gd_lblNewLabel);
		lblNewLabel.setText("To get started, please log in below with a valid CodeCollaborate account.");

		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		GridData gd_composite = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_composite.heightHint = 62;
		gd_composite.widthHint = 287;
		composite.setLayoutData(gd_composite);

		Label lblUsername = new Label(composite, SWT.NONE);
		lblUsername.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblUsername.setText("Username");

		usernameBox = new Text(composite, SWT.BORDER);
		usernameBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblPassword = new Label(composite, SWT.NONE);
		lblPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPassword.setText("Password");

		passwordBox = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		passwordBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

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
		button.setText("Login");
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(316, 198);
	}

	@Override
	protected void okPressed() {
		Semaphore waiter = new Semaphore(0);

		Request loginReq = (new UserLoginRequest(usernameBox.getText(), passwordBox.getText())).getRequest(response -> {
			if (response.getStatus() != 200) {
				MessageDialog err = new MessageDialog(getShell(),
						"Login failed with status code " + response.getStatus() + ". Please try again later.");
				err.open();
				return;
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

		super.okPressed();
	}

}
