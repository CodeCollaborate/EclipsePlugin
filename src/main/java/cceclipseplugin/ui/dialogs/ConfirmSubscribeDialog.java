package cceclipseplugin.ui.dialogs;

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
		return new ConfirmSubscribeDialog(new Shell(), msg);
	}
	
	@Override
	public void okPressed() {
		PluginManager pm = PluginManager.getInstance();
		pm.getRequestManager().fetchAndSubscribeAll(pm.getSubscribedProjectIds());
	}
}
