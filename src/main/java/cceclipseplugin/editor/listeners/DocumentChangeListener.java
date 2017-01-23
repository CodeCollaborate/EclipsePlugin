package cceclipseplugin.editor.listeners;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.texteditor.ITextEditor;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.editor.DocumentManager;
import constants.CoreStringConstants;
import dataMgmt.DataManager;
import dataMgmt.MetadataManager;
import dataMgmt.SessionStorage;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import patching.Diff;
import patching.Patch;
import websocket.ConnectException;
import websocket.models.responses.FileChangeResponse;

/**
 * Listens for document changes, and dispatches a new FileChangeRequest when
 * changes occur.
 * 
 * @author Benedict
 */
public class DocumentChangeListener implements IDocumentListener {

	private final Logger logger = LogManager.getLogger("documentChangeListener");
	
	/**
	 * Called when document is about to be changed.
	 * 
	 * @param event
	 *            the DocumentEvent that triggered this listener.
	 */
	@Override
	public void documentAboutToBeChanged(DocumentEvent event) {
		System.out.printf("DocumentChangeListener %s got event %s", this, event.toString());

		List<Diff> diffs = new ArrayList<>();
		String currDocument = event.getDocument().get();

		MetadataManager mm = PluginManager.getInstance().getMetadataManager();
		DocumentManager docMgr = PluginManager.getInstance().getDocumentManager();
		SessionStorage ss = PluginManager.getInstance().getDataManager().getSessionStorage();

		ITextEditor editor = docMgr.getEditor(docMgr.getCurrFile());
		IFile file = editor.getEditorInput().getAdapter(IFile.class);
		IProject proj = file.getProject();
		ProjectMetadata projMeta = mm.getProjectMetadata(proj.getLocation().toString());
		String fullPath = file.getFullPath().toString();
		FileMetadata fileMeta = mm.getFileMetadata(fullPath);
		if (projMeta == null || fileMeta == null || !ss.getSubscribedIds().contains(projMeta.getProjectID())
				|| fileMeta.getFilename().contains(CoreStringConstants.CONFIG_FILE_NAME)) {
			// TODO: Remove these debug statements
			if (fileMeta == null) {
				logger.warn("File metadata was null");
			}
			if (projMeta == null) {
				logger.warn("Project metadata was null");
			}
			return;
		}

		if (fileMeta.getVersion() == 0) {
			logger.error("File version was 0");
		}
		
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

		// If diffs were not incoming, applied diffs, convert to LF

		List<Diff> newDiffs = new ArrayList<>();
		diffLoop: for (int i = 0; i < diffs.size(); i++) {
			while (!docMgr.getAppliedDiffs().isEmpty()) {
				Diff appliedDiff = docMgr.getAppliedDiffs().poll();
				System.out.printf("DEBUG SEND-ON-NOTIF: %s ?= %s; %b\n", diffs.get(i).toString(),
						appliedDiff.toString(), diffs.get(i).equals(appliedDiff));
				if (diffs.get(i).equals(appliedDiff)) {
					continue diffLoop;
				}
			}
			newDiffs.add(diffs.get(i).convertToLF(currDocument));
		}

		// If no diffs left; abort
		if (newDiffs.isEmpty()) {
			logger.debug("No new diffs, aborting");
			return;
		}

		// Create the patch
		Patch patch = new Patch(fileMeta.getVersion(), newDiffs);

		logger.debug("DocumentManager sending change request");;

		try {
			String projRootPath = proj.getLocation().toString();
			DataManager.getInstance().getPatchManager().sendPatch(fileMeta.getFileID(), fileMeta.getVersion(),
					new Patch[] { patch }, response -> {
						synchronized (fileMeta) {
							long version = ((FileChangeResponse) response.getData()).getFileVersion();
							if (version == 0) {
								logger.error("File version returned from server was 0");
							}
							fileMeta.setVersion(((FileChangeResponse) response.getData()).getFileVersion());
						}
						PluginManager.getInstance().getMetadataManager().writeProjectMetadataToFile(projMeta,
								projRootPath, CoreStringConstants.CONFIG_FILE_NAME);
					}, null);
//			editor.doSave(null);
		} catch (ConnectException e) {
			logger.error("Failed to send change request", e);
		}
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
