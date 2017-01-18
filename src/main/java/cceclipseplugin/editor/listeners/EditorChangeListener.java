package cceclipseplugin.editor.listeners;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.AbstractDocument;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

import cceclipseplugin.core.PluginManager;
import cceclipseplugin.editor.DocumentManager;
import cceclipseplugin.log.Logger;

/**
 * Listens to changes in eclipse parts, notifies DocumentManager when new
 * documents/editors are opened or closed
 * 
 * @author Benedict
 */
public class EditorChangeListener extends AbstractEditorChangeListener {

	private DocumentChangeListener currListener = null;
	private final DocumentManager documentMgr = PluginManager.getInstance().getDocumentManager();

	public EditorChangeListener() {
		IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorReference[] editorRefs = activePage.getEditorReferences();
		for (IEditorReference ref : editorRefs) {
			IEditorPart editor = ref.getEditor(false);

			if (editor instanceof ITextEditor) {
				String filePath = ((IFileEditorInput) editor.getEditorInput()).getFile().getLocation().toString();
				this.documentMgr.openedEditor(filePath, (ITextEditor) editor);

				if (editor == activePage.getActiveEditor()) {
					this.partActivated(activePage.getActivePartReference());
				}
			}
		}
	}

	/**
	 * Called when a part is opened. Notifies documentManager of opened document
	 */
	@Override
	public void partOpened(IWorkbenchPartReference ref) {
		if (ref.getPart(false) instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) ref.getPart(false);
			String filePath = ((IFileEditorInput) editor.getEditorInput()).getFile().getLocation().toString();

			this.documentMgr.openedEditor(filePath, editor);
			Logger.getInstance().log(IStatus.INFO, String.format("Opened document %s", editor.getTitle()));
		}
	}

	/**
	 * Called when a part is closed. Notifies documentManager of document
	 * closure.
	 */
	@Override
	public void partClosed(IWorkbenchPartReference ref) {
		if (ref.getPart(false) instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) ref.getPart(false);
			IFile f = ((IFileEditorInput) editor.getEditorInput()).getFile();
			if (!f.exists()) {
				return;
			}
			String filePath = f.getLocation().toString();
			this.documentMgr.closedDocument(filePath);
			Logger.getInstance().log(IStatus.INFO, String.format("Closed document %s", editor.getTitle()));
		}

	}

	/**
	 * Notify DocumentManager of active document, set new listener.
	 */
	@Override
	public void partActivated(IWorkbenchPartReference ref) {
		if (ref.getPart(false) instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) ref.getPart(false);
			AbstractDocument document = (AbstractDocument) editor.getDocumentProvider()
					.getDocument(editor.getEditorInput());
			String filePath = ((IFileEditorInput) editor.getEditorInput()).getFile().getLocation().toString();

			currListener = new DocumentChangeListener();
			this.documentMgr.setCurrFile(filePath);
			document.addDocumentListener(currListener);
		}
	}

	/**
	 * Notify DocumentManager of inactive document, removes listener
	 */
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
