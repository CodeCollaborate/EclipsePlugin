package cceclipseplugin.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Text;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.preferences.PreferenceConstants;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class WelcomeDialog extends Dialog {
	private Text usernameBox;
	private Text passwordBox;
	private IPreferenceStore prefStore;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 */
	public WelcomeDialog(Shell parentShell, IPreferenceStore prefStore) {
		super(parentShell);
		this.prefStore = prefStore;
		setShellStyle(SWT.DIALOG_TRIM | SWT.ON_TOP | SWT.APPLICATION_MODAL);
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
		lblNewLabel.setLayoutData(gd_lblNewLabel);
		lblNewLabel.setText(DialogStrings.WelcomeDialog_InstructionsLabel);

		Composite composite = new Composite(container, SWT.NONE);
		composite.setLayout(new GridLayout(2, false));
		GridData gd_composite = new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1);
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
				RegisterDialog dialog = new RegisterDialog(null);
				close();
				dialog.open();
			}
		});
		GridData gd_btnRegister = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
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

	@Override
	protected void okPressed() {
		String username = usernameBox.getText();
		String password = passwordBox.getText();
		
		PluginManager.getInstance().getRequestManager().login(username, password);
		
		prefStore.setValue(PreferenceConstants.USERNAME, username);
		prefStore.setValue(PreferenceConstants.PASSWORD, password);

		super.okPressed();
	}
	
	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(DialogStrings.WelcomeDialog_Title);
	}
}
