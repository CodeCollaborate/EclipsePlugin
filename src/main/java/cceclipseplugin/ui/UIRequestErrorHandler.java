package cceclipseplugin.ui;

import org.eclipse.swt.widgets.Shell;

import websocket.IRequestSendErrorHandler;

public class UIRequestErrorHandler implements IRequestSendErrorHandler {

	private Shell shell;
	private String errorMsg;
	
	public UIRequestErrorHandler(Shell parentShell, String errorMsg) {
		shell = parentShell;
		this.errorMsg = errorMsg;
	}
	
	@Override
	public void handleRequestSendError() {
		MessageDialog err = new MessageDialog(shell, errorMsg);
        err.create();
        err.open();
	}

}
