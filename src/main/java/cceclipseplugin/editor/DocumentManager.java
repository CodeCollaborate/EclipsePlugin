package cceclipseplugin.editor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import cceclipseplugin.editor.listeners.EditorChangeListener;
import patcher.Patch;

public class DocumentManager {

	private static DocumentManager instance = null;

	private String currFile = null;
	public ITextEditor currEditor = null;
	private HashMap<String, ITextEditor> openEditors = new HashMap<>();
	private Queue<Patch> appliedPatches = new LinkedList<>();

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

	public Queue<Patch> getAppliedPatches() {
		return appliedPatches;
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

	public void applyPatch(String fileName, Patch patch) throws BadLocationException {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {

				// CAUSING GLITCHES BECAUSE OF PUTTING ITEMS BETWEEN \r\n ->
				// \ra\n
				ITextEditor editor = currEditor;
				AbstractDocument document = getDocumentForEditor(editor);
				ITextSelection currSelection = getSelectionForEditor(editor);

				if (document.get().length() >= patch.getStartIndex() + 2
						&& document.get().substring(patch.getStartIndex(), patch.getStartIndex() + 2).equals("\r\n")) {
					throw new IllegalArgumentException("Inserted between \\r and \\n");
				}
				if (currFile.equals(fileName)) {
					appliedPatches.add(patch);
				}

				try {
					if (patch.getRemovals() > 0) {
						document.replace(patch.getStartIndex(), patch.getRemovals(), "");
						// if (currSelection.getOffset() >=
						// patch.getStartIndex() + patch.getRemovals()) {
						// editor.selectAndReveal(currSelection.getOffset() -
						// patch.getRemovals(), 0);
						// } else if (currSelection.getOffset() >=
						// patch.getStartIndex()) {
						// editor.selectAndReveal(patch.getStartIndex(), 0);
						// }
					} else if (!patch.getInsertions().isEmpty()) {
						document.replace(patch.getStartIndex(), 0, patch.getInsertions());
						// if (currSelection.getOffset() >=
						// patch.getStartIndex()) {
						// editor.selectAndReveal(currSelection.getOffset() +
						// patch.getInsertions().length(), 0);
						// }
					} else {
						throw new IllegalArgumentException("Invalid patch; no operation described");
					}
				} catch (BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		});
	}
}
