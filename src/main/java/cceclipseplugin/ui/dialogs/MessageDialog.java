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
import org.eclipse.wb.swt.SWTResourceManager;

import cceclipseplugin.ui.StringConstants;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;

/**
 * A dialog that displays a custom message. Mainly used for notifying the user
 * that a request was a success or failure.
 * 
 * @author loganga
 *
 */
public class MessageDialog extends Dialog {

	private String errorMessage;

	/**
	 * Create the dialog with a non-initialized error message. should only be
	 * used for testing.
	 * 
	 * @param parentShell
	 * @wbp.parser.constructor
	 */
	public MessageDialog(Shell parentShell) {
		super(parentShell);
		errorMessage = StringConstants.NOT_INITIALIZED;
	}

	/**
	 * Create the dialog with the given error message.
	 * 
	 * @param parentShell
	 * @param message
	 */
	public MessageDialog(Shell parentShell, String message) {
		super(parentShell);
		this.errorMessage = message;
	}

	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));

		Label label = new Label(container, SWT.WRAP | SWT.CENTER);
		label.setForeground(SWTResourceManager.getColor(SWT.COLOR_RED));
		GridData gd_label = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		gd_label.heightHint = 44;
		gd_label.widthHint = 240;
		label.setLayoutData(gd_label);
		label.setText(errorMessage);

		return container;
	}

	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		// cancel button inherently exists in superclass, but is removed since
		// it serves the same functionality in this instance
	}

	/**
	 * Return the initial size of the dialog.
	 */
	@Override
	protected Point getInitialSize() {
		return new Point(271, 149);
	}
	
	@Override
	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText("CodeCollaborate");
	}

}
