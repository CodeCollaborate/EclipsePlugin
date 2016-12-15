package cceclipseplugin.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.wb.swt.SWTResourceManager;

/**
 * A dialog that displays a custom message. Mainly used for notifying the user
 * that a request was a success or failure.
 * 
 * @author loganga
 *
 */
public class MessageDialog extends Dialog {

	private String displayMessage;
	private int textColor = SWT.NONE;
	
	/**
	 * Create the dialog with a non-initialized error message. should only be
	 * used for testing.
	 * 
	 * @param parentShell
	 * @wbp.parser.constructor
	 */
	public MessageDialog(Shell parentShell) {
		super(parentShell);
		displayMessage = StringConstants.NOT_INITIALIZED;
	}

	/**
	 * Create the dialog with the given error message.
	 * 
	 * @param parentShell
	 * @param message
	 */
	private MessageDialog(Shell parentShell, String message) {
		super(parentShell);
		this.displayMessage = message;
	}
	
	private MessageDialog(Shell parentShell, String message, int color) {
		this(parentShell, message);
		this.textColor = color;
	}
	
	public static MessageDialog createDialog(String message) {
        final MessageDialog[] dialog = new MessageDialog[1];
        Display.getDefault().syncExec(() -> {
            Shell shell = Display.getDefault().getActiveShell();
            dialog[0] =  new MessageDialog(shell, message);
        });
		return dialog[0];
	}
	
	public static MessageDialog createDialog(String message, int textColor) {
        final MessageDialog[] dialog = new MessageDialog[1];
        Display.getDefault().syncExec(() -> {
            Shell shell = Display.getDefault().getActiveShell();
            dialog[0] = new MessageDialog(shell, message, textColor);
        });
        return dialog[0];
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
		int color;
		if (textColor != SWT.NONE) {
			color = textColor;
		} else {
			color = SWT.COLOR_RED;
		}
		label.setForeground(SWTResourceManager.getColor(color));
		GridData gd_label = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		label.setLayoutData(gd_label);
		label.setText(displayMessage);

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
	
	@Override
	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText("CodeCollaborate");
	}

}
