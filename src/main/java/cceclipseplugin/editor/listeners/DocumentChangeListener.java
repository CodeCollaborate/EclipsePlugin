package cceclipseplugin.editor.listeners;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.editor.DocumentManager;
import clientcore.models.FileChangeRequest;
import clientcore.models.NewRequest;
import patching.Diff;
import patching.Patch;

/**
 * Listens for document changes, and dispatches a new FileChangeRequest when changes occur.
 * @author Benedict
 */
public class DocumentChangeListener implements IDocumentListener {

	/**
	 * Called when document is about to be changed.
	 * @param event the DocumentEvent that triggered this listener.
	 */
	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {

		List<Diff> diffs = new ArrayList<>();
		String currDocument = event.getDocument().get();

		// Create removal diffs if needed
		if (event.getLength() > 0) {
			Diff diff = new Diff(false, event.getOffset(),
					currDocument.substring(event.getOffset(), event.getOffset() + event.getLength()));
			diffs.add(diff);
		}
		// Create insertion diffs if needed
		if (!event.getText().isEmpty()) {
			Diff patch = new Diff(true, event.getOffset(), event.getText());
			diffs.add(patch);
		}

		// If diffs were incoming, applied diffs, early-out
		DocumentManager docMgr = DocumentManager.getInstance();
		for (int i = 0; i < diffs.size(); i++) {
			while (!docMgr.getAppliedDiffs().isEmpty()) {
				if (diffs.get(i).equals(docMgr.getAppliedDiffs().poll())) {
					diffs.remove(i);
					i--;
				}
			}
		}
		
		// If no diffs left; abort
		if (diffs.isEmpty()) {
			return;
		}

		// Convert all diffs to LF format.
		List<Diff> newDiffs = new ArrayList<>();
		for (Diff diff : diffs) {
			newDiffs.add(diff.convertToLF(currDocument));
		}

		// Create the patch
		Patch patch = new Patch(0, newDiffs);

		// Send to server
		FileChangeRequest changeRequest = new FileChangeRequest(12345, Arrays.asList(patch.toString()), 0);

		// TODO: move this functionality to the client core
		NewRequest req = changeRequest.getRequest();
		try {
			PluginManager.getInstance().getWSManager().sendRequest(req);
		} catch (ConnectException e) {
			System.out.println("Failed to send change request.");
			e.printStackTrace();
		}

		/*
		 * Example application of a patch without triggering the listener.
		 */
		// Diff testDiff1 = new Diff("39:-2:" +
		// Utils.urlEncode(currDocument.substring(39, 41)));
		// Diff testDiff2 = new Diff("39:+1:a");
		// Patch testPatch = new Patch(0, Arrays.asList(testDiff1, testDiff2));
		// testPatch = testPatch.transform(new Patch(0, diffs));
		// DocumentManager.getInstance().applyPatch(
		// "D:/Workspaces/runtime-EclipseApplication/Test/src/test/TestClass3.java",
		// Arrays.asList(testPatch));
	}

	/**
	 * No nothing, simple stub.
	 * @param event The DocumentEvent that triggered this listener.
	 */
	@Override
	public void documentChanged(DocumentEvent event) {
	}
}
