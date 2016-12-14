package cceclipseplugin.ui.dialogs;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.core.PluginManager;

public class ConfirmSubscribeDialog extends OkCancelDialog {

	protected ConfirmSubscribeDialog(Shell parentShell) {
		super(parentShell);
	}
	
	protected ConfirmSubscribeDialog(Shell parentShell, String message) {
		super(parentShell, message);
	}
	
	public static ConfirmSubscribeDialog createDialog(String msg) {
        final ConfirmSubscribeDialog[] dialog = new ConfirmSubscribeDialog[1];
        Display.getDefault().syncExec(() -> {
            Shell shell = Display.getDefault().getActiveShell();
            dialog[0] = new ConfirmSubscribeDialog(shell, msg);
        });
        return dialog[0];
	}
	
	@Override
	public void okPressed() {
		PluginManager pm = PluginManager.getInstance();
		pm.getRequestManager().fetchAndSubscribeAll(pm.getSubscribedProjectIds());
	}
}
