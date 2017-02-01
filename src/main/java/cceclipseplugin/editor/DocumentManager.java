package cceclipseplugin.editor;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.internal.filebuffers.SynchronizableDocument;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.ui.UIRequestErrorHandler;
import cceclipseplugin.ui.dialogs.MessageDialog;
import constants.CoreStringConstants;
import dataMgmt.DataManager;
import dataMgmt.MetadataManager;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import patching.Diff;
import patching.Patch;
import websocket.IFileChangeNotificationHandler;
import websocket.INotificationHandler;
import websocket.models.File;
import websocket.models.Notification;
import websocket.models.Request;
import websocket.models.notifications.FileChangeNotification;
import websocket.models.requests.FileChangeRequest;
import websocket.models.requests.FilePullRequest;
import websocket.models.responses.FileChangeResponse;
import websocket.models.responses.FileCreateResponse;
import websocket.models.responses.FilePullResponse;

/**
 * Manages documents, finds editors as needed.
 *
 * @author Benedict
 *
 */
public class DocumentManager implements IFileChangeNotificationHandler {

	private final Logger logger = LogManager.getLogger("documentManager");
	
	private String currFile = null;
	private HashMap<String, ITextEditor> openEditors = new HashMap<>();
	private HashMap<String, LinkedList<Diff>> appliedDiffs = new HashMap<>();

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
	 * @param absolutePath
	 *            File path of active file
	 */
	public void setCurrFile(String absolutePath) {
		if (absolutePath == null) {
			this.currFile = null;
			return;
		}
		this.currFile = absolutePath;
	}

	/**
	 * FilePath of editor that was just opened
	 *
	 * @param absolutePath
	 *            File path of opened editor
	 * @param editor
	 *            Editor that is opened for the given file
	 */
	public void openedEditor(String absolutePath, ITextEditor editor) {
		this.openEditors.put(absolutePath, editor);

		IFile file = editor.getEditorInput().getAdapter(IFile.class);
		String fullPath = file.getFullPath().toString();
		FileMetadata fileMeta = DataManager.getInstance().getMetadataManager().getFileMetadata(fullPath);
		if (fileMeta == null) {
			// TODO: Remove these debug statements
			if (fileMeta == null) {
				System.out.println("file metadata was null");
			}
			return;
		}

		DataManager.getInstance().getPatchManager().setModificationStamp(fileMeta.getFileID(),
				((SynchronizableDocument) editor.getDocumentProvider().getDocument(editor.getEditorInput()))
						.getModificationStamp());
	}

	/**
	 * Call when a document is closed.
	 *
	 * @param absolutePath
	 *            filePath of file that was closed.
	 */
	public void closedDocument(String absolutePath) {
		if (absolutePath == null || absolutePath.equals(this.currFile)) {
			setCurrFile(null);
		}
		this.openEditors.remove(absolutePath);
		this.sendFilePullRequest(absolutePath);
	}

	private void sendFilePullRequest(String absolutePath) {
		PluginManager pm = PluginManager.getInstance();
		MetadataManager mm = pm.getMetadataManager();
		
		String workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString();
		java.io.File rootFile = new java.io.File(workspaceRoot);
		java.io.File absoluteFile = new java.io.File(absolutePath);
		String workspaceRelativePathString = "/" + rootFile.toURI().relativize(absoluteFile.toURI()).toString();
		try {
			workspaceRelativePathString = java.net.URLDecoder.decode(workspaceRelativePathString, "UTF-8");
		} catch (UnsupportedEncodingException e1) {
			logger.error("Error parsing file relative path", e1);
		}
		IPath relativePath = new Path(workspaceRelativePathString);
		FileMetadata file = mm.getFileMetadata(workspaceRelativePathString);
		if (file == null) {
			logger.warn(String.format("Closed an untracked file: %s", workspaceRelativePathString));
			return;
		}
		long projId = mm.getProjectIDForFileID(file.getFileID());
		Set<Long> subProjIds = pm.getDataManager().getSessionStorage().getSubscribedIds();
		if (!subProjIds.contains(projId)) {
			logger.debug("Closed a file in an unsubscribed project");
			return;
		}
		Request req = (new FilePullRequest(file.getFileID())).getRequest(response -> {
			if (response.getStatus() == 200) {
				byte[] fileBytes = ((FilePullResponse) response.getData()).getFileBytes();
				String fileContents = new String(fileBytes);
				List<Patch> patches = new ArrayList<>();
				for (String stringPatch : ((FilePullResponse) response.getData()).getChanges()) {
					patches.add(new Patch(stringPatch));
				}
				IFile newFile = ResourcesPlugin.getWorkspace().getRoot().getFile(relativePath);
				newFile = newFile.getProject().getFile(file.getFilePath());
				fileContents = pm.getDataManager().getPatchManager().applyPatch(fileContents, patches);

				NullProgressMonitor progressMonitor = new NullProgressMonitor();
				try {
					if (newFile.exists()) {
						pm.putFileInWarnList(relativePath.toString(), FileChangeResponse.class);
						ByteArrayInputStream in = new ByteArrayInputStream(fileContents.getBytes());
						newFile.setContents(in, false, false, progressMonitor);
						in.close();
					} else {
						// warn directory watching before creating the file
						pm.putFileInWarnList(relativePath.toString(), FileCreateResponse.class);
						ByteArrayInputStream in = new ByteArrayInputStream(fileContents.getBytes());
						newFile.create(in, false, progressMonitor);
						in.close();
					}
				} catch (Exception e) {
					e.printStackTrace();
					Display.getDefault().asyncExec(() -> MessageDialog
							.createDialog("Failed to pull file on resource close. Please resubscribe to the project."));
				}
			}
		}, new UIRequestErrorHandler("Couldn't send file pull request after file close: " + absolutePath));
		pm.getWSManager().sendAuthenticatedRequest(req);
	}

	/**
	 * Gets the editor for the given file
	 *
	 * @param absolutePath
	 *            path of file that is open
	 * @return ITextEditor instance for given filePath
	 */
	public ITextEditor getEditor(String absolutePath) {
		return this.openEditors.get(absolutePath);
	}

	/**
	 * Get the queue of diffs that were just applied for the given filepath.
	 *
	 * @return The queue of diffs that was applied for the given filepath.
	 */
	public LinkedList<Diff> getAppliedDiffs(String filepath) {
		if (!appliedDiffs.containsKey(filepath)) {
			appliedDiffs.put(filepath, new LinkedList<>());
		}
		return appliedDiffs.get(filepath);
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
			logger.error("Error getting document for editor");
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
	public Long handleNotification(Notification n, long expectedModificationStamp) {
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
			return -1l;
		}

		IPath projectRootPath = new Path(
				PluginManager.getInstance().getMetadataManager().getProjectLocation(projectID));
		String absolutePath = projectRootPath.append(fileMeta.getFilePath()).toString();

		// TODO(wongb): Build patch reorder buffer, making sure that they are
		// applied in
		// order.
		// This is a temporary fix.
		// if (changeNotif.fileVersion <= fileMeta.getVersion()) {
		// try {
		// System.out.printf(
		// "ChangeNotification version was less than or equal to current
		// version: %d <= %d; Notification: ",
		// changeNotif.fileVersion, fileMeta.getVersion(), new
		// ObjectMapper().writeValueAsString(n));
		// } catch (JsonProcessingException e) {
		// e.printStackTrace();
		// }
		// return;
		// }

		ProjectMetadata projMeta = PluginManager.getInstance().getMetadataManager()
				.getProjectMetadata(projectRootPath.toString());

		IPath fileRelativePathWithName = new Path(fileMeta.getFilePath());
		IPath projectRelativePath = new Path(projMeta.getName());
		String workspaceRelativePath = projectRelativePath.append(fileRelativePathWithName).toString();

		Long result = this.applyPatch(n.getResourceID(), expectedModificationStamp, absolutePath, workspaceRelativePath.toString(), patches);

		if (result != null) {
			synchronized (fileMeta) {
				fileMeta.setVersion(changeNotif.fileVersion);
			}
			PluginManager.getInstance().getMetadataManager().writeProjectMetadataToFile(projMeta,
					projectRootPath.toString(), CoreStringConstants.CONFIG_FILE_NAME);
		}
		return result;

	}

	/**
	 * If the document is open, patch it in memory. Otherwise, send it back to
	 * client core for file patching.
	 *
	 * @param fileId
	 *            fileId to patch; this is mainly used for passing to clientCore
	 * @param absolutePath
	 *            absolute file path; used as key in editorMap, and patches.
	 * @param workspaceRelativePath
	 *            workspace relative file path that includes the filename
	 * @param patches
	 *            the list of patches to apply, in order.
	 */
	public Long applyPatch(long fileId, long expectedModificationStamp, String absolutePath, String workspaceRelativePath,
			List<Patch> patches) {
		String currFile = this.currFile;
		ITextEditor editor = getEditor(absolutePath);

		// Get reference to open document
		AbstractDocument document = getDocumentForEditor(editor);
		
		Long[] result = new Long[1];
		
		if (editor != null && document != null) {
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					synchronized (((SynchronizableDocument) document).getLockObject()) {
						// If modification stamp does not match,
						if (expectedModificationStamp != -1
								&& document.getModificationStamp() != expectedModificationStamp) {
							System.out.println(
									"Document changed between notification arrival and attempt to append. Retrying");
							System.out.println("Got modification stamp " + document.getModificationStamp() + "; wanted "
									+ expectedModificationStamp);
							result[0] = null;
							return;
						}

						// Get text in document.
						String newDocument = document.get();

						// If CRLFs are found, apply patches in CRLF mode.
						boolean useCRLF = newDocument.contains("\r\n");


						for (Patch patch : patches) {

							if (useCRLF) {
								patch = patch.convertToCRLF(newDocument);
							}

							for (Diff diff : patch.getDiffs()) {

								// Throw errors if we are trying to insert
								// between
								// \r and \n
								if (diff.getStartIndex() > 0 && diff.getStartIndex() < document.get().length()
										&& document.get().charAt(diff.getStartIndex() - 1) == '\r'
										&& document.get().charAt(diff.getStartIndex()) == '\n') {
									throw new IllegalArgumentException("Tried to insert between \\r and \\n");
								}

								// Apply the change to the document
								try {
									// If patching an active file, add it to the
									// patch
									// list to ignore.
									if (currFile.equals(absolutePath)) {
										if (!appliedDiffs.containsKey(absolutePath)) {
											appliedDiffs.put(absolutePath, new LinkedList<>());
										}
										appliedDiffs.get(absolutePath).add(diff);
									}

									System.out.println("Applying diff: " + diff);

									if (diff.isInsertion()) {
										document.replace(diff.getStartIndex(), 0, diff.getChanges());
									} else {
										document.replace(diff.getStartIndex(), diff.getLength(), "");
									}
								} catch (BadLocationException e) {
									logger.error( 
											String.format("Bad Location; Patch: %s, Len: %d, Text: %s\n", 
													diff.toString(), 
													document.get().length(), 
													document.get()), 
											e);
								}
							}
						}
						result[0] = document.getModificationStamp();
					}
				}
			});
			
			Display.getDefault().asyncExec(() -> {
				PluginManager.getInstance().putFileInWarnList(workspaceRelativePath,
						FileChangeRequest.class);
				editor.doSave(new NullProgressMonitor());
			});
			
			return result[0];
		} else {
			// If file is not open in an editor, enqueue the patch for
			// writing.

			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IPath ipath = new Path(absolutePath);
			IFile file = workspace.getRoot().getFileForLocation(ipath);
			if (!file.exists()) {
				logger.warn(String.format("Cannot apply patches to non-existent file: %s", absolutePath.toString()));
				return -1l;
			}

			String contents = null;
			try (Scanner s = new Scanner(file.getContents())) {
				contents = s.useDelimiter("\\A").hasNext() ? s.next() : "";
			} catch (CoreException e) {
				logger.error("Cannot read file", e);
				return -1l;
			}
			PluginManager m = PluginManager.getInstance();
			String newContents = m.getDataManager().getPatchManager().applyPatch(contents, patches);
			m.putFileInWarnList(workspaceRelativePath, FileChangeRequest.class);
			try {
				file.setContents(new ByteArrayInputStream(newContents.getBytes()), true, true, null);
			} catch (CoreException e) {
				logger.error("Fail to update files on disk", e);
				return -1l;
			}

			return -1l;
		}
	}
}
