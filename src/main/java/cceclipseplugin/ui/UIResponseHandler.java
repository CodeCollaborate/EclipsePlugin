package cceclipseplugin.ui;

import java.util.concurrent.Semaphore;

import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import cceclipseplugin.ui.dialogs.MessageDialog;
import websocket.IResponseHandler;
import websocket.models.Response;

public class UIResponseHandler implements IResponseHandler {

	private Shell shell;
	private Semaphore waiter;
	private String requestName;
	
	public UIResponseHandler(Shell shell, Semaphore waiter, String requestName) {
		this.shell = shell;
		this.waiter = waiter;
		this.requestName = requestName;
	}
	
	public UIResponseHandler(Shell shell, String requestName) {
		this.shell = shell;
		this.requestName = requestName;
	}
	
	@Override
	public void handleResponse(Response response) {
		if (response.getStatus() != 200) {
			MessageDialog err = new MessageDialog(shell, requestName + " failed with status code " + response.getStatus() + ".");
			Display.getDefault().asyncExec(() -> err.open());
		}
		
		if (waiter != null)
			waiter.release();
		
	}
	
}
