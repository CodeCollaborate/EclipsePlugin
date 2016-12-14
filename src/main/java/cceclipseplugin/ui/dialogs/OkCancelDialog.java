package cceclipseplugin.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;

public class OkCancelDialog extends Dialog {

	private String message;
	
	protected OkCancelDialog(Shell parentShell) {
		super(parentShell);
	}
	
	protected OkCancelDialog(Shell parentShell, String msg) {
		super(parentShell);
		message = msg;
	}
	
	public static OkCancelDialog createDialog(String msg) {
		Shell s = Display.getDefault().getActiveShell();
		return new OkCancelDialog(s, msg);
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
		GridData gd_label = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		label.setLayoutData(gd_label);
		label.setText(message);

		return container;
	}
	
	@Override
	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText("CodeCollaborate");
	}
}
