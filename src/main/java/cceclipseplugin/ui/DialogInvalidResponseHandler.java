package cceclipseplugin.ui;

import org.eclipse.swt.widgets.Display;
import cceclipseplugin.ui.dialogs.MessageDialog;
import requestMgmt.IInvalidResponseHandler;

public class DialogInvalidResponseHandler implements IInvalidResponseHandler {

	@Override
	public void handleInvalidResponse(int errorCode, String message) {
		// TODO: Make more specific for different error codes
		Display.getDefault().asyncExec(() -> MessageDialog.createDialog(message).open());
	}

}
