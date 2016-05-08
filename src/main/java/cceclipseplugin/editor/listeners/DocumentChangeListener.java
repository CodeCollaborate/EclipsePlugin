package cceclipseplugin.editor.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

import cceclipseplugin.editor.DocumentManager;
import clientcore.models.FileChangeRequest;
import patcher.Diff;
import patcher.Patch;

public class DocumentChangeListener implements IDocumentListener {

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {

		List<Diff> diffs = new ArrayList<>();
		String currDocument = event.getDocument().get();

		if (event.getLength() > 0) {
			Diff diff = new Diff(false, event.getOffset(),
					currDocument.substring(event.getOffset(), event.getOffset() + event.getLength()));
			diffs.add(diff);
		}
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
		if (diffs.isEmpty()) {
			return;
		}

		List<Diff> newDiffs = new ArrayList<>();
		for (Diff diff : diffs) {
			newDiffs.add(diff.convertToLF(currDocument));
		}
		
		Patch patch = new Patch(0, newDiffs);

		// Send to server
		FileChangeRequest changeRequest = new FileChangeRequest(12345, Arrays.asList(patch.toString()), 0);
		
		System.out.println(patch.toString());
		
		// TODO: move this functionality to the client core
//		Request req = changeRequest.getRequest();
//		try {
//			Plugin.manager.sendRequest(req);
//		} catch (ConnectException e) {
//			System.out.println("Failed to send change request.");
//			e.printStackTrace();
//		}
		

		/*
		 * Example application of a patch without triggering the listener. Must
		 * be run on separate thread, to make sure we don't block GUI thread.
		 */

//		Diff testDiff1 = new Diff("39:-2:" + Utils.urlEncode(currDocument.substring(39, 41)));
//		Diff testDiff2 = new Diff("39:+1:a");
//		Patch testPatch = new Patch(0, Arrays.asList(testDiff1, testDiff2));
//		testPatch = testPatch.transform(new Patch(0, diffs));
//		DocumentManager.getInstance().applyPatch(
//				"D:/Workspaces/runtime-EclipseApplication/Test/src/test/TestClass3.java", Arrays.asList(testPatch));
	}

	@Override
	public void documentChanged(DocumentEvent event) {
		// prevFileContents = document.get();
	}
}
