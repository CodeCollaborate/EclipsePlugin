package cceclipseplugin.editor;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import cceclipseplugin.core.PluginManager;
import clientcore.models.FileChangeNotification;
import dataMgmt.FileContentWriter;
import dataMgmt.MetadataManager;
import dataMgmt.models.FileMetadata;
import patching.Diff;
import patching.Patch;
import websocket.models.Notification;

/**
 * Manages documents, finds editors as needed.
 * 
 * @author Benedict
 *
 */
public class DocumentManager {

	private String currFile = null;
	private HashMap<String, ITextEditor> openEditors = new HashMap<>();
	private Queue<Diff> appliedDiffs = new LinkedList<>();

	public DocumentManager() {

	}

	/**
	 * Gets file path of current file
	 * 
	 * @return current file's path
	 */
	public String getCurrFile() {
		return currFile;
	}

	/**
	 * Path of file that is currently active/open
	 * 
	 * @param filePath
	 *            File path of active file
	 */
	public void setCurrFile(String filePath) {
		this.currFile = filePath;
	}

	/**
	 * FilePath of editor that was just opened
	 * 
	 * @param filePath
	 *            File path of opened editor
	 * @param editor
	 *            Editor that is opened for the given file
	 */
	public void openedEditor(String filePath, ITextEditor editor) {
		this.openEditors.put(filePath, editor);
	}

	/**
	 * Call when a document is closed.
	 * 
	 * @param filePath
	 *            filePath of file that was closed.
	 */
	public void closedDocument(String filePath) {
		if (this.currFile.equals(filePath)) {
			setCurrFile(null);
		}
		this.openEditors.remove(filePath);
	}

	/**
	 * Gets the editor for the given file
	 * 
	 * @param filePath
	 *            path of file that is open
	 * @return ITextEditor instance for given filePath
	 */
	public ITextEditor getEditor(String filePath) {
		return this.openEditors.get(filePath);
	}

	/**
	 * Get the queue of diffs that were just applied.
	 * 
	 * @return The queue of diffs that was applied.
	 */
	public Queue<Diff> getAppliedDiffs() {
		return appliedDiffs;
	}

	/**
	 * Gets the active document for a given editor
	 * 
	 * @param editor
	 *            the editor to retrieve the document from
	 * @return the document which was retrieved.
	 */
	private AbstractDocument getDocumentForEditor(ITextEditor editor) {
		return (AbstractDocument) editor.getDocumentProvider().getDocument(editor.getEditorInput());
	}

	/**
	 * Notification handler for document manager. Parses generic notification to
	 * FileChangeNotification.
	 * 
	 * @param n
	 *            Notification of file changes.
	 */
	public void handleNotification(Notification n) {
		JsonNode notificationJSON = n.getData();

		// Convert to correct notification types
		FileChangeNotification notification = new ObjectMapper().convertValue(notificationJSON,
				FileChangeNotification.class);

		// Parse list of patches.
		List<Patch> patches = new ArrayList<>();
		for (String patchStr : notification.getChanges()) {
			patches.add(new Patch(patchStr));
		}

		// Get file path to write to.
		FileMetadata fileMetaData = PluginManager.getInstance().getMetadataManager().getFileMetadata(notification.getFileID());
		String projectRootPath = PluginManager.getInstance().getMetadataManager().getProjectPath(fileMetaData.getProjectId());
		String filepath = Paths.get(projectRootPath, fileMetaData.getFilePath()).toString();

		this.applyPatch(notification.getFileID(), filepath, patches);
	}

	/**
	 * If the document is open, patch it in memory. Otherwise, send it back to
	 * client core for file patching.
	 * 
	 * @param fileId
	 *            fileId to patch; this is mainly used for passing to clientCore
	 * @param filePath
	 *            absolute file path; used as key in editorMap, and patches.
	 * @param patches
	 *            the list of patches to apply, in order.
	 */
	public void applyPatch(long fileId, String filePath, List<Patch> patches) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				ITextEditor editor = getEditor(filePath);
				if (editor != null) {

					// Get reference to open document
					AbstractDocument document = getDocumentForEditor(editor);

					// Get text in document.
					String newDocument = document.get();

					// If CRLFs are found, apply patches in CRLF mode.
					boolean useCRLF = newDocument.contains("\r\n");

					for (Patch patch : patches) {

						if (useCRLF) {
							patch = patch.convertToCRLF(newDocument);
						}

						for (Diff diff : patch.getDiffs()) {

							// Throw errors if we are trying to insert between
							// \r and \n
							if (diff.getStartIndex() > 0 && diff.getStartIndex() < document.get().length()
									&& document.get().charAt(diff.getStartIndex() - 1) == '\r'
									&& document.get().charAt(diff.getStartIndex()) == '\n') {
								throw new IllegalArgumentException("Tried to insert between \\r and \\n");
							}

							// If patching an active file, add it to the patch
							// list to ignore.
							if (currFile.equals(filePath)) {
								appliedDiffs.add(diff);
							}

							try {
								// Apply the change to the document
								if (diff.isInsertion()) {
									document.replace(diff.getStartIndex(), 0, diff.getChanges());
								} else {
									document.replace(diff.getStartIndex(), diff.getLength(), "");
								}
							} catch (BadLocationException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				} else {
					// If file is not open in an editor, enqueue the patch for
					// writing.
					PluginManager.getInstance().getDataManager().getFileContentWriter().enqueuePatchesForWriting(fileId, filePath, patches);
				}
			}
		});
	}
}
