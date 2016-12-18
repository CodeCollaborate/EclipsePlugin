package cceclipseplugin.ui;

import org.eclipse.swt.widgets.Display;
import cceclipseplugin.ui.dialogs.MessageDialog;
import websocket.IRequestSendErrorHandler;

public class UIRequestErrorHandler implements IRequestSendErrorHandler {

	private String errorMsg;
	
	public UIRequestErrorHandler(String errorMsg) {
		this.errorMsg = errorMsg;
	}
	
	@Override
	public void handleRequestSendError() {
		Display.getDefault().asyncExec(() -> MessageDialog.createDialog(errorMsg).open());
	}
}
