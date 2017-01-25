package cceclipseplugin.editor.listeners;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.IStatus;

import cceclipseplugin.core.CCIgnore;
import cceclipseplugin.core.PluginManager;
import dataMgmt.MetadataManager;
import dataMgmt.SessionStorage;
import dataMgmt.models.ProjectMetadata;

public abstract class AbstractDirectoryListener implements IResourceChangeListener {
	
	private final Logger logger = LogManager.getLogger("directoryListener");
	
	protected CCIgnore ignoreFile;
	
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta rootDelta = event.getDelta();
		if (rootDelta == null) {
			return;
		}
		
		ignoreFile = new CCIgnore();
		logger.debug(String.format("resource change detected for %s", rootDelta.getResource().getName()));
		recursivelyHandleChange(rootDelta);
	}
	
	/**
	 * Iterates through the children of the given IResourceDelta and, if a registered CodeCollaborate
	 * project with metadata, sends the resource to its corresponding handler. 
	 * 
	 * @param delta
	 */
	private void recursivelyHandleChange(IResourceDelta delta) {
		IResource res = delta.getResource();
		
		if (res instanceof IProject) {
			// stop handling if the project doesn't have CodeCollaborate metadata
			logger.debug(String.format("type: project; kind: %d", delta.getKind()));
			MetadataManager meta = PluginManager.getInstance().getMetadataManager();
			ProjectMetadata pMeta = meta.getProjectMetadata(res.getLocation().toString());
			if (pMeta == null) {
				logger.debug(String.format("No project metadata found for project path \"%s\"", res.getLocation().toString()));
				return;
			}
			// stop handling if not subscribed
			SessionStorage ss = PluginManager.getInstance().getDataManager().getSessionStorage();
			if (!ss.getSubscribedIds().contains(pMeta.getProjectID())) {
				logger.debug(String.format("Not subscribed, ignoring resource change for project \"%s\"", res.getLocation().toString()));
				return;
			}
			
			ignoreFile.loadCCIgnore((IProject) res);
			
			boolean stopRecursion = handleProject(delta);
			
			if (stopRecursion) {
				return;
			}
		} else if (res instanceof IFolder) {
			String path = res.getProjectRelativePath().toString();
			if (ignoreFile.containsEntry(path)) {
				logger.debug(String.format("Folder %s was ignored", path));
				return;
			}
		} if(res instanceof IFile) {
			String path = res.getProjectRelativePath().toString();
			if (ignoreFile.containsEntry(path)) {
				logger.debug(String.format("File %s was ignored", path));
				return;
			}
			logger.debug(String.format("type: file; kind: %d", delta.getKind()));
			handleFile(delta);
			
		} 
		
		for (IResourceDelta childDelta : delta.getAffectedChildren()) {
			recursivelyHandleChange(childDelta);
		}
	}
	
	protected abstract void handleFile(IResourceDelta delta);

	protected abstract boolean handleProject(IResourceDelta delta);
}
