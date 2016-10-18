package cceclipseplugin.ui;

import org.eclipse.swt.widgets.Display;
import cceclipseplugin.ui.dialogs.MessageDialog;
import websocket.IRequestSendErrorHandler;

public class DialogRequestSendErrorHandler implements IRequestSendErrorHandler {

	@Override
	public void handleRequestSendError() {
		// TODO: Log more detailed message
		Display.getDefault().asyncExec(() -> MessageDialog.createDialog("Could not send request.").open());
	}
}
