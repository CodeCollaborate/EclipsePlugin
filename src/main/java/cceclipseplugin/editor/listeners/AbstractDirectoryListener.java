package cceclipseplugin.editor.listeners;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;

import cceclipseplugin.core.EclipseRequestManager;
import cceclipseplugin.core.PluginManager;
import dataMgmt.MetadataManager;

public abstract class AbstractDirectoryListener implements IResourceChangeListener {

	protected Set<String> ignoredFiles = new HashSet<>();
	
	@Override
	public void resourceChanged(IResourceChangeEvent event) {
		IResourceDelta rootDelta = event.getDelta();
		if (rootDelta == null) {
			return;
		}
		
		System.out.println("resource change detected for " + rootDelta.getResource().getName());
		recursivelyHandleChange(rootDelta);
	}
	
	public void loadCCIgnore(IProject p) {
		IFile f = p.getFile(Paths.get(".ccignore").normalize().toString());
		if (f.exists()) {
			try {
				ignoredFiles.clear();
				InputStream in = f.getContents();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line;
				while ((line = reader.readLine()) != null) {
					ignoredFiles.add(line);
				}
				in.close();
			} catch (CoreException | IOException e) {
				e.printStackTrace();
			}
		} else {
			EclipseRequestManager rm = PluginManager.getInstance().getRequestManager();
			rm.createCCIgnoreFile(p);
		}
	}
	
	/**
	 * Iterates through the children of the given IResourceDelta and, if a registered CodeCollaborate
	 * project with metadata, sends the resource to its corresponding handler. 
	 * 
	 * @param delta
	 */
	private void recursivelyHandleChange(IResourceDelta delta) {
		IResource res = delta.getResource();
		
		if (res instanceof IFile) {
			System.out.println("type: file; kind: " + delta.getKind());
			if (ignoredFiles.contains(res.getName())) {
				System.out.println(String.format("File %s was ignored", res.getName()));
			} else {
				handleFile(delta);				
			}
		} else if (res instanceof IProject) {
			// stop handling if the project doesn't have CodeCollaborate metadata
			System.out.println("type: project; kind: " + delta.getKind());
			MetadataManager meta = PluginManager.getInstance().getMetadataManager();
			
			if (meta.getProjectMetadata(res.getFullPath().toString()) == null) {
				System.out.println("no project metadata found for project path \"" + res.getFullPath().toString() + "\"");
				return;
			}
			
			loadCCIgnore((IProject) res);
			
			
			boolean stopRecursion = handleProject(delta);
			
			if (stopRecursion) {
				return;
			}
		}
		
		for (IResourceDelta childDelta : delta.getAffectedChildren()) {
			recursivelyHandleChange(childDelta);
		}
	}
	
	protected abstract void handleFile(IResourceDelta delta);

	protected abstract boolean handleProject(IResourceDelta delta);
	
}
