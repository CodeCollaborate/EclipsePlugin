package cceclipseplugin.ui;

import org.eclipse.swt.widgets.Display;
import cceclipseplugin.ui.dialogs.MessageDialog;
import websocket.IResponseHandler;
import websocket.models.Response;

public class UIResponseHandler implements IResponseHandler {

	private String requestName;
	
	public UIResponseHandler(String requestName) {
		this.requestName = requestName;
	}
	
	@Override
	public void handleResponse(Response response) {
		if (response.getStatus() != 200) {
			Display.getDefault().asyncExec(() -> MessageDialog.createDialog(requestName + " failed with status code " + response.getStatus() + ".").open());
		}
	}
}
