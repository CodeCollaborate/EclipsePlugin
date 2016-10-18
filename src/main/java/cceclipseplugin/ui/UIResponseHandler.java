package cceclipseplugin.ui;

import java.util.concurrent.Semaphore;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.ui.dialogs.MessageDialog;
import websocket.IResponseHandler;
import websocket.models.Response;

public class UIResponseHandler implements IResponseHandler {

	private Semaphore waiter;
	private String requestName;
	
	// TODO: remove shell parameter and make new shell
	public UIResponseHandler(Semaphore waiter, String requestName) {
		this.waiter = waiter;
		this.requestName = requestName;
	}
	
	public UIResponseHandler(String requestName) {
		this.requestName = requestName;
	}
	
	@Override
	public void handleResponse(Response response) {
		if (response.getStatus() != 200) {
			Display.getDefault().asyncExec(() -> 
				new MessageDialog(new Shell(), requestName + " failed with status code " + response.getStatus() + ".").open());
		}
		
		if (waiter != null) {
			waiter.release();
		}
	}
}
