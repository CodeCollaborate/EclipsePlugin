package cceclipseplugin.core;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Semaphore;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import cceclipseplugin.ui.UIRequestErrorHandler;
import cceclipseplugin.ui.dialogs.MessageDialog;
import websocket.models.File;
import websocket.models.Project;
import websocket.models.Request;
import websocket.models.requests.FilePullRequest;
import websocket.models.responses.FilePullResponse;

public class ProjectManager {
	public void createEclipseProject(Project p, File[] files, byte[][] fileBytes) throws CoreException {
		IProgressMonitor progressMonitor = new NullProgressMonitor();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(p.getName());
		project.create(progressMonitor);
		project.open(progressMonitor);
		
		for (int i = 0; i < files.length; i++) {
			// maybe create folders?
			IFile newFile = project.getFile(new Path(files[i].getRelativePath()));
			newFile.create(new ByteArrayInputStream(fileBytes[i]), true, progressMonitor);
		}
	}
	
	public void deleteEclipseProject(Project p) {
		IProgressMonitor progressMonitor = new NullProgressMonitor();
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IProject project = root.getProject(p.getName());
		try {
			project.delete(true, true, progressMonitor);
		} catch (CoreException e) {
			e.printStackTrace();
		}
	}
	
	public void updateEclipseProjectFiles(File[] files, byte[][] fileBytes) {
		
	}
	
	public byte[][] pullFiles(File[] files) {
		byte[][] fileBytes = new byte[files.length][];
		Semaphore waiter = new Semaphore(0);
		for (int i = 0; i < files.length; i++) {
			final int index = i; // have to assign index to final ref to use in response
			Request req = (new FilePullRequest(files[i].getFileID())).getRequest(response -> {
				if (response.getStatus() == 200) {
					fileBytes[index] = ((FilePullResponse) response.getData()).getFileBytes();
					if (index == files.length - 1)
						waiter.release();
				} else {
					MessageDialog.createDialog("Failed to pull file" + files[index].getFilename() + " with status code " + response.getStatus()).open();
				}
			}, new UIRequestErrorHandler("Couldn't send file pull request."));
			
			PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
		}
		
		while (!waiter.tryAcquire(1)) {
//			 wait... (eventually notify UI)
		}
		return fileBytes;
	}
}
