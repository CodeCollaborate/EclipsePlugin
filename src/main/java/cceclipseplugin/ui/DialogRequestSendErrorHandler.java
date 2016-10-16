package cceclipseplugin.ui;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.ui.dialogs.MessageDialog;
import websocket.IRequestSendErrorHandler;

public class DialogRequestSendErrorHandler implements IRequestSendErrorHandler {

	@Override
	public void handleRequestSendError() {
		MessageDialog err = new MessageDialog(new Shell(), "Could not send request.");
		Display.getDefault().asyncExec(() -> err.open());
	}

}
