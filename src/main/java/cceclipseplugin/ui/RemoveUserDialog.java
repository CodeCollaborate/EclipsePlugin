package cceclipseplugin.ui;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

public class RemoveUserDialog extends Dialog {

	private String username;
	private String projectName;

	/**
	 * Create the dialog.
	 * 
	 * @param parentShell
	 * @wbp.parser.constructor
	 */
	public RemoveUserDialog(Shell parentShell) {
		super(parentShell);
		parentShell.setText("Remove User");
	}

	public RemoveUserDialog(Shell parentShell, String username, String projectName) {
		super(parentShell);
		this.username = username;
		this.projectName = projectName;
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
		gd_lblAreYouSure.widthHint = 334;
		gd_lblAreYouSure.heightHint = 33;
		lblAreYouSure.setLayoutData(gd_lblAreYouSure);
		lblAreYouSure.setText("Are you sure you want to remove " + username + " from the " + projectName + " project?");

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
		button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
			}
		});
		button.setText("Remove");
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(362, 132);
	}
	
	@Override
	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText("CodeCollaborate - Remove User");
	}

}
