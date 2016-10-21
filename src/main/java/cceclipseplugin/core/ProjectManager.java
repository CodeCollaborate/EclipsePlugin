package cceclipseplugin.core;

import java.io.ByteArrayInputStream;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

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
			String path = files[i].getRelativePath();
			path = path.replace("/", "\\");
			if (path.contains("\\")) {
				path = path.substring(path.indexOf("\\") + 1, path.length()); // get rid of project name from dir
			} else {
				path = "";
			}
			path = path + files[i].getFilename();
			IFile newFile = project.getFile(new Path(path));
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
		final int limit = files.length;
		for (int i = 0; i < limit; i++) {
			final int index = i; // have to assign index to final ref to use in response
			Request req = (new FilePullRequest(files[i].getFileID())).getRequest(response -> {
				System.out.println("Got some stuff");
//				if (response.getStatus() == 200) {
//					fileBytes[index] = ((FilePullResponse) response.getData()).getFileBytes();
//					System.out.println("Index: "+index+" limit: "+files.length);
//					if (index == (limit - 1)) {
//						waiter.release();
//					}
//				} else {
//					MessageDialog.createDialog("Failed to pull file" + files[index].getFilename() + " with status code " + response.getStatus()).open();
//				}
			}, new UIRequestErrorHandler("Couldn't send file pull request."));
			
			PluginManager.getInstance().getWSManager().sendAuthenticatedRequest(req);
		}
		
		try {
			if (!waiter.tryAcquire(1, 30, TimeUnit.SECONDS)) {
				System.out.println("Couldn't get all dem files");
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return fileBytes;
	}
}
