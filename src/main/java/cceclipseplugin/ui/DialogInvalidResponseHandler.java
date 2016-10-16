package cceclipseplugin.ui;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.ui.dialogs.MessageDialog;
import requestMgmt.IInvalidResponseHandler;

public class DialogInvalidResponseHandler implements IInvalidResponseHandler {

	@Override
	public void handleInvalidResponse(int errorCode, String message) {
		// TODO: Make more specific for different error codes
		Display.getDefault().asyncExec(() -> (new MessageDialog(new Shell(), message)).open());
	}

}
