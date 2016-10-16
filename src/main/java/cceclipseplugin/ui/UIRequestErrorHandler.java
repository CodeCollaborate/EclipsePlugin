package cceclipseplugin.ui;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.ui.dialogs.MessageDialog;
import websocket.IRequestSendErrorHandler;

public class UIRequestErrorHandler implements IRequestSendErrorHandler {

	private Shell shell;
	private String errorMsg;
	
	// TODO: remove parentShell parameter and make new Shell()
	public UIRequestErrorHandler(Shell parentShell, String errorMsg) {
		shell = parentShell;
		this.errorMsg = errorMsg;
	}
	
	@Override
	public void handleRequestSendError() {
		MessageDialog err = new MessageDialog(shell, errorMsg);
		Display.getDefault().asyncExec(() -> err.open());
	}
}
