package cceclipseplugin.editor;

import java.io.ByteArrayInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Scanner;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cceclipseplugin.core.PluginManager;
import constants.CoreStringConstants;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import patching.Diff;
import patching.Patch;
import websocket.INotificationHandler;
import websocket.models.Notification;
import websocket.models.notifications.FileChangeNotification;
import websocket.models.requests.FileChangeRequest;

/**
 * Manages documents, finds editors as needed.
 * 
 * @author Benedict
 *
 */
public class DocumentManager implements INotificationHandler {

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
		if (filePath == null) {
			this.currFile = null;
			return;
		}
		this.currFile = Paths.get(filePath).toString();
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
		this.openEditors.put(Paths.get(filePath).toString(), editor);
	}

	/**
	 * Call when a document is closed.
	 * 
	 * @param filePath
	 *            filePath of file that was closed.
	 */
	public void closedDocument(String filePath) {
		if (filePath == null || filePath.equals(this.currFile)) {
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
		if (editor == null) {
			return null;
		}

		IDocumentProvider provider = editor.getDocumentProvider();
		IEditorInput input = editor.getEditorInput();
		if (provider != null && input != null) {
			return (AbstractDocument) editor.getDocumentProvider().getDocument(editor.getEditorInput());
		} else {
			System.out.println("Error getting document for editor");
			return null;
		}
	}

	/**
	 * Notification handler for document manager. Parses generic notification to
	 * FileChangeNotification.
	 * 
	 * @param n
	 *            Notification of file changes.
	 */
	public void handleNotification(Notification n) {
		// Convert to correct notification types
		FileChangeNotification changeNotif = (FileChangeNotification) n.getData();

		// Parse list of patches.
		List<Patch> patches = new ArrayList<>();
		for (String patchStr : changeNotif.changes) {
			patches.add(new Patch(patchStr));
		}

		// Get file path to write to.
		FileMetadata fileMeta = PluginManager.getInstance().getMetadataManager().getFileMetadata(n.getResourceID());
		Long projectID = PluginManager.getInstance().getMetadataManager().getProjectIDForFileID(fileMeta.getFileID());
		if (projectID == null) {
			// Early out if no such projectID found.
			return;
		}

		String projectRootPath = PluginManager.getInstance().getMetadataManager().getProjectLocation(projectID);
		String filepath = Paths.get(projectRootPath, fileMeta.getFilePath()).normalize().toString();

		// TODO(wongb): Build patch buffer, making sure that they are applied in
		// order.
		// This is a temporary fix.
		if (changeNotif.fileVersion <= fileMeta.getVersion()) {
			try {
				System.out.printf(
						"ChangeNotification version was less than or equal to current version: %d <= %d; Notification: ",
						changeNotif.fileVersion, fileMeta.getVersion(), new ObjectMapper().writeValueAsString(n));
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}

		this.applyPatch(n.getResourceID(), filepath, patches);

		ProjectMetadata projMeta = PluginManager.getInstance().getMetadataManager()
				.getProjectMetadata(ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() + "/");

		synchronized (fileMeta) {
			fileMeta.setVersion(changeNotif.fileVersion);
		}
		PluginManager.getInstance().getMetadataManager().writeProjectMetadataToFile(projMeta, projectRootPath,
				CoreStringConstants.CONFIG_FILE_NAME);

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
		String currFile = this.currFile;

		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				ITextEditor editor = getEditor(filePath);
				// Get reference to open document
				AbstractDocument document = getDocumentForEditor(editor);
				if (editor != null && document != null) {

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
								PluginManager.getInstance().putFileInWarnList(filePath, FileChangeRequest.class);
								editor.doSave(new NullProgressMonitor());
							} catch (BadLocationException e) {
								System.out.printf("Bad Location; Patch: %s, Len: %d, Text: %s\n", diff.toString(),
										document.get().length(), document.get());
								e.printStackTrace();
							}
						}
					}
				} else {
					// If file is not open in an editor, enqueue the patch for
					// writing.

					IWorkspace workspace = ResourcesPlugin.getWorkspace();
					IPath ipath = Path.fromOSString(filePath);
					IFile file = workspace.getRoot().getFileForLocation(ipath);
					if (!file.exists()) {
						System.out.println("Cannot apply patches to non-existent file: " + filePath);
						return;
					}			
					
					String contents = null;
					try (Scanner s = new Scanner(file.getContents())) {
						contents = s.useDelimiter("\\A").hasNext() ? s.next() : "";
					} catch (CoreException e) {
						System.out.println("Cannot read file");
						return;
					}
					PluginManager m = PluginManager.getInstance();
					String newContents = m.getDataManager().getPatchManager().applyPatch(contents, patches);
					m.putFileInWarnList(file.getProjectRelativePath().toString(), FileChangeRequest.class);
					try {
						file.setContents(new ByteArrayInputStream(newContents.getBytes()), true, true, null);
					} catch (CoreException e) {
						System.out.println("Fail to update files on disk");
					}

					// PluginManager.getInstance().getDataManager().getFileContentWriter().enqueuePatchesForWriting(fileId,
					// filePath, patches);
				}
			}
		});
	}
}
