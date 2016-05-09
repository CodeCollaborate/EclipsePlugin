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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import clientcore.models.FileChangeNotification;
import dataMgmt.FileContentWriter;
import dataMgmt.MetadataManager;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import patching.Diff;
import patching.Patch;
import websocket.models.Notification;

public class DocumentManager {

	private static DocumentManager instance = null;

	private String currFile = null;
	private HashMap<String, ITextEditor> openEditors = new HashMap<>();
	private Queue<Diff> appliedDiffs = new LinkedList<>();

	public static DocumentManager getInstance() {
		if (instance == null) {
			synchronized (DocumentManager.class) {
				if (instance == null) {
					instance = new DocumentManager();
				}
			}
		}
		return instance;
	}

	private DocumentManager() {

	}

	public String getCurrFile() {
		return currFile;
	}

	public void setCurrFile(String currFile) {
		this.currFile = currFile;
	}

	public void openedEditor(String name, ITextEditor editor) {
		this.openEditors.put(name, editor);
	}

	public void closedDocument(String name) {
		if (this.currFile.equals(name)) {
			setCurrFile(null);
		}
		this.openEditors.remove(name);
	}

	public ITextEditor getEditor(String fileName) {
		return this.openEditors.get(fileName);
	}

	public Queue<Diff> getAppliedDiffs() {
		return appliedDiffs;
	}

	private AbstractDocument getDocumentForEditor(ITextEditor editor) {
		return (AbstractDocument) editor.getDocumentProvider().getDocument(editor.getEditorInput());
	}

	private ITextSelection getSelectionForEditor(ITextEditor editor) {
		return (ITextSelection) editor.getSite().getSelectionProvider().getSelection();
	}

	private void setSelectionForEditor(ITextEditor editor, ITextSelection selection) {
		editor.getSite().getSelectionProvider().setSelection(selection);
	}

	public void handleNotification(Notification n) {
		JsonNode notificationJSON = n.getData();

		FileChangeNotification notification = new ObjectMapper().convertValue(notificationJSON,
				FileChangeNotification.class);

		List<Patch> patches = new ArrayList<>();
		for (String patchStr : notification.getChanges()) {
			patches.add(new Patch(patchStr));
		}

		// Get file path to write to.
		FileMetadata fileMetaData = MetadataManager.getInstance().getFileMetadata(notification.getFileID());
		String projectRootPath = MetadataManager.getInstance().getProjectPath(fileMetaData.getProjectId());
		String filepath = Paths.get(projectRootPath, fileMetaData.getFilePath()).toString();

		this.applyPatch(notification.getFileID(), filepath, patches);
	}

	public void applyPatch(long fileId, String filePath, List<Patch> patches) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				ITextEditor editor = getEditor(filePath);
				if (editor != null) {

					AbstractDocument document = getDocumentForEditor(editor);
					// ITextSelection currSelection =
					// getSelectionForEditor(editor);

					String newDocument = document.get();

					// If more CRLFs are found, re-analyze, using the new start
					// index
					boolean useCRLF = newDocument.contains("\r\n");

					for (Patch patch : patches) {

						if (useCRLF) {
							patch.convertToCRLF(newDocument);
						}

						for (Diff diff : patch.getDiffs()) {

							// Throw errors if we are trying to insert between
							// \r
							// and \n
							if (diff.getStartIndex() > 0 && diff.getStartIndex() < document.get().length()
									&& document.get().charAt(diff.getStartIndex() - 1) == '\r'
									&& document.get().charAt(diff.getStartIndex()) == '\n') {
								throw new IllegalArgumentException("Tried to insert between \\r and \\n");
							}

							if (currFile.equals(filePath)) {
								appliedDiffs.add(diff);
							}

							try {
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
					FileContentWriter.getInstance().enqueuePatchesForWriting(fileId, filePath, patches);
				}
			}
		});
	}
}
