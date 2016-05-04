package cceclipseplugin.editor.listeners;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

import cceclipseplugin.editor.DocumentManager;
import patcher.Patch;

public class DocumentChangeListener implements IDocumentListener {

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
		// prevFileContents = document.get();
	}

	@Override
	public void documentChanged(DocumentEvent event) {

		Patch patch = new Patch(event.getOffset(), event.getLength(), event.getText());

		// If patch was an incoming, applied patch, early-out
		DocumentManager docMgr = DocumentManager.getInstance();
		while (!docMgr.getAppliedPatches().isEmpty()) {
			if (patch.equals(docMgr.getAppliedPatches().poll())) {
				return;
			}
		}

		System.out.println(patch);

		/*
		 * Example application of a patch without triggering the listener.
		 * Must be run on separate thread, to make sure we don't block GUI thread.
		 */
//		Thread t = new Thread(new Runnable() {
//			@Override
//			public void run() {
				try {
//					Thread.sleep(500);
					DocumentManager.getInstance().applyPatch(
							"D:/Workspaces/runtime-EclipseApplication/Test/src/test/TestClass3.java",
							new Patch("45:+a"));
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
//				} catch (InterruptedException e) {
//					// TODO Auto-generated catch block
//					e.printStackTrace();
				}
//			}
//		});
//		t.start();
	}
}
