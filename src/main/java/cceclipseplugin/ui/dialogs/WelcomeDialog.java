package cceclipseplugin.ui.dialogs;

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
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

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
		lblNewLabel.setText(DialogStrings.WelcomeDialog_InstructionsLabel);

		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		GridData gd_composite = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
		gd_composite.heightHint = 62;
		gd_composite.widthHint = 287;
		composite.setLayoutData(gd_composite);

		Label lblUsername = new Label(composite, SWT.NONE);
		lblUsername.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblUsername.setText(DialogStrings.WelcomeDialog_UsernameLabel);

		usernameBox = new Text(composite, SWT.BORDER);
		usernameBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));

		Label lblPassword = new Label(composite, SWT.NONE);
		lblPassword.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		lblPassword.setText(DialogStrings.WelcomeDialog_PasswordLabel);

		passwordBox = new Text(composite, SWT.BORDER | SWT.PASSWORD);
		passwordBox.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		Composite composite_1 = new Composite(container, SWT.NONE);
		composite_1.setLayout(new GridLayout(5, false));
		GridData gd_composite_1 = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		gd_composite_1.widthHint = 284;
		gd_composite_1.heightHint = 34;
		composite_1.setLayoutData(gd_composite_1);
		new Label(composite_1, SWT.NONE);
		new Label(composite_1, SWT.NONE);
		new Label(composite_1, SWT.NONE);
		
		Label lblDontHaveAn = new Label(composite_1, SWT.NONE);
		lblDontHaveAn.setText(DialogStrings.WelcomeDialog_DontHaveAccount);
		
		Button btnRegister = new Button(composite_1, SWT.NONE);
		btnRegister.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				RegisterDialog dialog = new RegisterDialog(new Shell());
				dialog.open();
				close();
			}
		});
		GridData gd_btnRegister = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gd_btnRegister.widthHint = 79;
		btnRegister.setLayoutData(gd_btnRegister);
		btnRegister.setText(DialogStrings.WelcomeDialog_ReigsterLabel);

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
		button.setText(DialogStrings.WelcomeDialog_LoginLabel);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(316, 240);
	}

	@Override
	protected void okPressed() {
		String username = usernameBox.getText();
		String password = passwordBox.getText();
		
//		IPreferenceStore prefStore = Activator.getDefault().getPreferenceStore();
//		prefStore.setValue(PreferenceConstants.USERNAME, username);
//		prefStore.setValue(PreferenceConstants.PASSWORD, password);
//		
		PluginManager.getInstance().getRequestManager().loginAndSubscribe(username, password);

		super.okPressed();
	}
	
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(DialogStrings.WelcomeDialog_Title);
	}
}
