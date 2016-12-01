package cceclipseplugin.ui.dialogs;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.util.OSUtil;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;

public class RecoverPasswordDialog extends Dialog {
	
	/**
	 * Create the dialog.
	 * @param parentShell
	 */
	public RecoverPasswordDialog(Shell parentShell) {
		super(parentShell);
	}

	/**
	 * Create contents of the dialog.
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		container.setLayout(new GridLayout(1, false));
		
		Label recoveryMessage = new Label(container, SWT.WRAP | SWT.CENTER);
		GridData gd_recoveryMessage = new GridData(SWT.CENTER, SWT.CENTER, false, false, 1, 1);
		gd_recoveryMessage.widthHint = 500;
		recoveryMessage.setLayoutData(gd_recoveryMessage);
		recoveryMessage.setText(DialogStrings.RecoverPasswordDialog_Message);
		
		String mailTo = "mailto:codecollaboratesup@gmail.com?subject=CodeCollaborate Password Recovery Request";
		Link link = new Link(container, SWT.NONE);
		link.setText("<a href=\"" + mailTo + "\">codecollaboratesup@gmail.com</a>");
		link.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
		link.addListener(SWT.Selection, (event) -> {
			try {
				if (OSUtil.isWindows()) {
					Program.launch(mailTo);
				} else if (OSUtil.isLinux()) {
					Runtime.getRuntime().exec("xdg-open " + mailTo);
				} // TODO: add one-click mailto launching for Mac (and other versions of Linux launchers)
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		return container;
	}

	/**
	 * Create contents of the button bar.
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, false);
	}
	
	@Override
	protected void configureShell(Shell shell) {
	      super.configureShell(shell);
	      shell.setText(DialogStrings.RecoverPasswordDialog_Title);
	}
}
