package cceclipseplugin.editor.listeners;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IExecutionListener;
import org.eclipse.core.commands.NotHandledException;

import cceclipseplugin.core.PluginManager;

public class SaveExecutionListener implements IExecutionListener {

	@Override
	public void notHandled(String arg0, NotHandledException arg1) {
		System.out.println("Save not handled");
	}

	@Override
	public void postExecuteFailure(String arg0, ExecutionException arg1) {
		System.out.println("Save execution failed. Re-registered resource listeners.");
		PluginManager.getInstance().registerResourceListeners();
	}

	@Override
	public void postExecuteSuccess(String arg0, Object arg1) {
		System.out.println("Re-registered resource listeners");
		PluginManager.getInstance().registerResourceListeners();
	}

	@Override
	public void preExecute(String arg0, ExecutionEvent arg1) {
		System.out.println("Deregistered resource listeners");
		PluginManager.getInstance().deregisterResourceListeners();
	}

}
