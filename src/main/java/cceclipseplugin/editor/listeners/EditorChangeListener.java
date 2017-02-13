package cceclipseplugin.editor.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.resources.IFile;
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

/**
 * Listens to changes in eclipse parts, notifies DocumentManager when new
 * documents/editors are opened or closed
 * 
 * @author Benedict
 */
public class EditorChangeListener extends AbstractEditorChangeListener {

	private final Logger logger = LogManager.getLogger("editorChangeListener");
	
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

			// editor is not a text editor, we don't support realtime changes for it
			if (!(editor.getEditorInput() instanceof IFileEditorInput)) {
				return;
			}

			String filePath = ((IFileEditorInput) editor.getEditorInput()).getFile().getLocation().toString();

			this.documentMgr.openedEditor(filePath, editor);
			logger.debug(String.format("Opened document %s", editor.getTitle()));
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

			// editor is not an IFileEditorInput editor, we don't support realtime changes for it
			if (!(editor.getEditorInput() instanceof IFileEditorInput)) {
				return;
			}

			IFile f = ((IFileEditorInput) editor.getEditorInput()).getFile();
			if (!f.exists()) {
				return;
			}
			String filePath = f.getLocation().toString();
			this.documentMgr.closedDocument(filePath);
			logger.debug(String.format("Closed document %s", editor.getTitle()));
		}

	}

	/**
	 * Notify DocumentManager of active document, set new listener.
	 */
	@Override
	public void partActivated(IWorkbenchPartReference ref) {
		if (ref.getPart(false) instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) ref.getPart(false);

			System.err.println("Part activated for editor" + editor.getTitle());
			
			AbstractDocument document = (AbstractDocument) editor.getDocumentProvider()
					.getDocument(editor.getEditorInput());

			// editor is not a text editor, we don't support realtime changes for it
			if (!(editor.getEditorInput() instanceof IFileEditorInput)) {
				return;
			}

			String filePath = ((IFileEditorInput) editor.getEditorInput()).getFile().getLocation().toString();

			if(!filePath.equals(this.documentMgr.getCurrFile())){					
				currListener = new DocumentChangeListener();
	
				System.err.println("Added DocumentChangeListener" + currListener.toString());
				
				this.documentMgr.setCurrFile(filePath);
				document.addDocumentListener(currListener);
			}
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

			System.err.println("Part deactivated for editor" + editor.getTitle());
			
			IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());

			if (currListener != null) {
				document.removeDocumentListener(currListener);
			}
		}
	}
}
