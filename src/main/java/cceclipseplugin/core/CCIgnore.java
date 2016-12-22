package cceclipseplugin.core;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;

import cceclipseplugin.ui.dialogs.MessageDialog;
import dataMgmt.MetadataManager;
import dataMgmt.models.FileMetadata;
import websocket.models.responses.FileCreateResponse;

public class CCIgnore {
	
	public static String FILENAME = ".ccignore";
	public static String DEFAULT_CONTENTS = "CodeCollaborateConfig.json\nbin\n";
	
	private Set<String> ignoredFiles = new HashSet<>();
	
	/**
	 * Creates a new .ccignore file for the given project on disk. This 
	 * creation will be detected by the directory watching system and 
	 * sent to the server. Also loads the file into an instance of this
	 * class so that the contents can be checked with the provided handles.
	 * 
	 * @param p
	 * @return Returns a reference to the loaded 
	 */
	public static CCIgnore createForProject(IProject p) {
		IFile file = p.getFile(new Path(Paths.get(FILENAME).normalize().toString()));
		
		if (!file.exists()) {
			InputStream in = new ByteArrayInputStream(DEFAULT_CONTENTS.getBytes());
			
			try {
				// warn directory watching
				PluginManager.getInstance().putFileInWarnList(file.getProjectRelativePath().toString(), FileCreateResponse.class);
				
				file.create(in, true, new NullProgressMonitor());
				in.close();
			} catch (CoreException e) {
				MessageDialog.createDialog("Failed to generate .ccignore file.").open();
				e.printStackTrace();
			} catch (IOException e) {
				System.err.println("Failed to close input stream.");
				e.printStackTrace();
			}
		}
		
		CCIgnore ignoreFile = new CCIgnore();
		ignoreFile.loadCCIgnore(p);
		return ignoreFile;
	}
	
	/**
	 * Loads the contents of the .ccignore file for the given project into
	 * the Set encapsulated within this class.
	 * 
	 * @param p
	 */
	public void loadCCIgnore(IProject p) {
		IFile f = p.getFile(Paths.get(FILENAME).normalize().toString());
		if (f.exists()) {
			try {
				ignoredFiles.clear();
				InputStream in = f.getContents();
				BufferedReader reader = new BufferedReader(new InputStreamReader(in));
				String line;
				while ((line = reader.readLine()) != null) {
					String path = Paths.get(line).normalize().toString();
					ignoredFiles.add(path);
				}
				reader.close();
				(new Thread(() -> serverCleanUp(p))).start();
			} catch (CoreException | IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Iterates through the file metadata for the given project and sends 
	 * delete requests if the file is included in the ignore file.
	 * 
	 * @param p
	 * 
	 */
	private void serverCleanUp(IProject p) {
		EclipseRequestManager rm = PluginManager.getInstance().getRequestManager();
		MetadataManager mm = PluginManager.getInstance().getMetadataManager();
		List<FileMetadata> fileMetas = mm.getProjectMetadata(p.getLocation().toString().replace("\\", "/")).getFiles();
		
		if (fileMetas == null) {
			return;
		}
		for (FileMetadata fm : fileMetas) {
			String path =  Paths.get(fm.getRelativePath(), fm.getFilename()).normalize().toString();
			for (String ignored : ignoredFiles) {
				if (path.startsWith(ignored) || path.equals(ignored)) {
					// send delete request for fileID
					System.out.println(String.format("Cleaning up %s from server", path));
					rm.deleteFile(fm.getFileID());
				}
			}
		}
	}
	
	/**
	 * Checks if the given entry is contained within the encapsulated 
	 * set that has been loaded from the .ccignore file.
	 * 
	 * @param e
	 * @return Returns true if the encapsulated set contains the given entry.
	 */
	public boolean containsEntry(String e) {
		String path = Paths.get(e).normalize().toString();
		return ignoredFiles.contains(path);
	}

}
