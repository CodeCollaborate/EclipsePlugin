package cceclipseplugin.editor.listeners;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

import cceclipseplugin.editor.DocumentManager;
import cceclipseplugin.utils.Utils;
import clientcore.models.FileChangeRequest;
import clientcore.models.Request;
import patcher.Diff;
import patcher.Patch;
import websocket.WSManager;

public class DocumentChangeListener implements IDocumentListener {

	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {

		List<Diff> diffs = new ArrayList<>();
		String currDocument = event.getDocument().get();

		if (event.getLength() > 0) {
			Diff patch = new Diff(false, event.getOffset(),
					currDocument.substring(event.getOffset(), event.getOffset() + event.getLength()));
			diffs.add(patch);
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

		for (Diff patch : diffs) {
			patch.convertToLF(currDocument);
		}

		// Send to server
		System.out.println(diffs);
		String[] diffStrings = new String[diffs.size()];
		for (String s : diffStrings) {
			s = diffs.toString();
		}
		FileChangeRequest changeRequest = new FileChangeRequest(12345, diffStrings, 0);
		
		// TODO: move this functionality to the client core
		Request req = changeRequest.getRequest();
		

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
