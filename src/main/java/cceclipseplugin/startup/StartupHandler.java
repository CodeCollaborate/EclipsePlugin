package cceclipseplugin.startup;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IStartup;
import org.eclipse.ui.PlatformUI;

import cceclipseplugin.editor.listeners.EditorChangeListener;

public class StartupHandler implements IStartup {

	@Override
	public void earlyStartup() {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService()
						.addPartListener(EditorChangeListener.getInstance());
			}
		});

	}

	public void MakeChangeWithoutTriggeringListener(AbstractDocument document) {
		try {
			// ((IDocumentExtension2) document).stopListenerNotification();
			document.replace(0, 1, "!!");
		} catch (BadLocationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
