package cceclipseplugin.editor.listeners;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;

import cceclipseplugin.constants.StringConstants;
import cceclipseplugin.core.PluginManager;
import cceclipseplugin.editor.DocumentManager;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import patching.Diff;
import patching.Patch;
import websocket.ConnectException;
import websocket.IRequestSendErrorHandler;
import websocket.IResponseHandler;
import websocket.models.Request;
import websocket.models.requests.FileChangeRequest;
import websocket.models.responses.FileChangeResponse;

/**
 * Listens for document changes, and dispatches a new FileChangeRequest when
 * changes occur.
 * 
 * @author Benedict
 */
public class DocumentChangeListener implements IDocumentListener {

	/**
	 * Called when document is about to be changed.
	 * 
	 * @param event
	 *            the DocumentEvent that triggered this listener.
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
		DocumentManager docMgr = PluginManager.getInstance().getDocumentManager();
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
		ProjectMetadata projMeta = PluginManager.getInstance().getMetadataManager()
				.getProjectMetadata(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/");

		FileMetadata fileMeta = PluginManager.getInstance().getMetadataManager()
				.getFileMetadata(StringConstants.FILE_ID);

		Request req = getFileChangeRequest(StringConstants.FILE_ID, new String[] { patch.toString() }, response -> {
					fileMeta.setVersion(((FileChangeResponse) response.getData()).getFileVersion());
					PluginManager.getInstance().getMetadataManager().writeProjectMetadata(projMeta,
							ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/");
				}, null, 1);

		try {
			PluginManager.getInstance().getWSManager().sendRequest(req);
		} catch (ConnectException e) {
			System.out.println("Failed to send change request.");
			e.printStackTrace();
		}
	}

	private Request getFileChangeRequest(long fileID, String[] changes, IResponseHandler respHandler,
			IRequestSendErrorHandler sendErrHandler, int retryCount) {
		FileMetadata fileMeta = PluginManager.getInstance().getMetadataManager()
				.getFileMetadata(StringConstants.FILE_ID);

		return new FileChangeRequest(fileID, changes, fileMeta.getVersion()).getRequest(response -> {
			
			// If we failed the first time around, update the fileVersion and retry.
			if(response.getStatus() == 409 && retryCount > 0){
				Request req = getFileChangeRequest(fileID, changes, respHandler, sendErrHandler, retryCount - 1);
				try {
					PluginManager.getInstance().getWSManager().sendRequest(req);
				} catch (ConnectException e) {
					System.out.println("Failed to send change request.");
					e.printStackTrace();
				}
				return;
			}

			respHandler.handleResponse(response);
		}, sendErrHandler);
	}

	/**
	 * No nothing, simple stub.
	 * 
	 * @param event
	 *            The DocumentEvent that triggered this listener.
	 */
	@Override
	public void documentChanged(DocumentEvent event) {
	}
}
