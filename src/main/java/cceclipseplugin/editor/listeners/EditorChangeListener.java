package cceclipseplugin.editor.listeners;

import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import cceclipseplugin.editor.DocumentManager;

public class EditorChangeListener extends AbstractEditorChangeListener {

	private static EditorChangeListener instance = null;
	private DocumentChangeListener currListener = null;
	private DocumentManager documentMgr = DocumentManager.getInstance();

	public static EditorChangeListener getInstance() {
		if (instance == null) {
			synchronized (EditorChangeListener.class) {
				if (instance == null) {
					instance = new EditorChangeListener();
				}
			}
		}
		return instance;
	}

	private EditorChangeListener() {
		IEditorReference[] editorRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
				.getEditorReferences();
		for (IEditorReference ref : editorRefs) {
			IEditorPart editor = ref.getEditor(false);

			if (editor instanceof ITextEditor) {
				IDocument document = ((ITextEditor) editor).getDocumentProvider().getDocument(editor.getEditorInput());
				String filePath = ((IFileEditorInput) editor.getEditorInput()).getFile().getRawLocation().toString();
				this.documentMgr.openedEditor(filePath, (ITextEditor) editor);
			}
		}
	}

	@Override
	public void partOpened(IWorkbenchPartReference ref) {
		if (ref.getPart(false) instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) ref.getPart(false);
			String filePath = ((IFileEditorInput) editor.getEditorInput()).getFile().getRawLocation().toString();

			this.documentMgr.openedEditor(filePath, editor);
			System.out.println("Opened document " + editor.getTitle());
		}
	}

	@Override
	public void partClosed(IWorkbenchPartReference ref) {
		if (ref.getPart(false) instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) ref.getPart(false);

			String filePath = ((IFileEditorInput) editor.getEditorInput()).getFile().getRawLocation().toString();

			this.documentMgr.closedDocument(filePath);
			System.out.println("Closed document " + editor.getTitle());
		}

	}

	@Override
	public void partActivated(IWorkbenchPartReference ref) {
		if (ref.getPart(false) instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) ref.getPart(false);
			AbstractDocument document = (AbstractDocument) editor.getDocumentProvider()
					.getDocument(editor.getEditorInput());
			String filePath = ((IFileEditorInput) editor.getEditorInput()).getFile().getRawLocation().toString();

			currListener = new DocumentChangeListener();
			this.documentMgr.setCurrFile(filePath);
			document.addDocumentListener(currListener);
			documentMgr.currEditor = editor;
		}
	}

	@Override
	public void partDeactivated(IWorkbenchPartReference ref) {
		// TODO Auto-generated method stub
		if (ref.getPart(false) instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) ref.getPart(false);

			IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());

			if (currListener != null) {
				document.removeDocumentListener(currListener);
			}
		}
	}
}
