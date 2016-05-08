package cceclipseplugin.editor;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.ITextEditor;

import patcher.Diff;
import patcher.Patch;

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

	public void applyPatch(String fileName, List<Patch> patches) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				ITextEditor editor = getEditor(fileName);
				if (editor != null) {

					AbstractDocument document = getDocumentForEditor(editor);
					// ITextSelection currSelection =
					// getSelectionForEditor(editor);

					String newDocument = document.get();

					// If more CRLFs are found, re-analyze, using the new start
					// index
					for (Patch patch : patches) {
						patch.convertToCRLF(newDocument);

						for (Diff diff : patch.getDiffs()) {

							// Throw errors if we are trying to insert between
							// \r
							// and \n
							if (diff.getStartIndex() > 0 && diff.getStartIndex() < document.get().length()
									&& document.get().charAt(diff.getStartIndex() - 1) == '\r'
									&& document.get().charAt(diff.getStartIndex()) == '\n') {
								// diff = diff.getOffsetDiff(-1);
								// System.out.println("Inserted between \\r and
								// \\n");
								throw new IllegalArgumentException("Tried to insert between \\r and \\n");
							}

							if (currFile.equals(fileName)) {
								appliedDiffs.add(diff);
							}

							try {
								if (diff.isInsertion()) {
									document.replace(diff.getStartIndex(), 0, diff.getChanges());
									// if (currSelection.getOffset() >=
									// patch.getStartIndex()) {
									// editor.selectAndReveal(currSelection.getOffset()
									// +
									// patch.getInsertions().length(),
									// currSelection.getLength());
									// }
								} else {
									document.replace(diff.getStartIndex(), diff.getLength(), "");
									// if (currSelection.getOffset() >=
									// patch.getStartIndex() +
									// patch.getRemovals())
									// {
									// editor.selectAndReveal(currSelection.getOffset()
									// -
									// patch.getRemovals(),
									// currSelection.getLength());
									// } else if (currSelection.getOffset() >=
									// patch.getStartIndex()) {
									// editor.selectAndReveal(patch.getStartIndex(),
									// currSelection.getLength());
									// }
								}
							} catch (BadLocationException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}
			}
		});
	}
}
