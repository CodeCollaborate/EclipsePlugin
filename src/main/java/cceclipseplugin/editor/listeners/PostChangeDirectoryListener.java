package cceclipseplugin.editor.listeners;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.contentmergeviewer.TokenComparator;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

import cceclipseplugin.core.EclipseRequestManager;
import cceclipseplugin.core.PluginManager;
import dataMgmt.MetadataManager;
import dataMgmt.models.FileMetadata;
import dataMgmt.models.ProjectMetadata;
import patching.Diff;
import patching.Patch;
import requestMgmt.RequestManager;

public class PostChangeDirectoryListener extends AbstractDirectoryListener {

	private PreChangeDirectoryListener preChangeListener;
	
	public PostChangeDirectoryListener(PreChangeDirectoryListener p) {
		this.preChangeListener = p;
	}
	
	/**
	 * Handles a resource delta in the case that the resource is and IProject.
	 * 
	 * @param delta
	 * @return 
	 * 		true if the recursion down the delta's children stops after this handler
	 */
	@Override
	protected boolean handleProject(IResourceDelta delta) {
		IProject p = (IProject) delta.getResource();
		ProjectMetadata projectMeta = PluginManager.getInstance().getMetadataManager().getProjectMetadata(p.getFullPath().toString());
		RequestManager rm = PluginManager.getInstance().getRequestManager();
		
		if (delta.getKind() == IResourceDelta.REMOVED) {
			System.out.println("flag: " + delta.getFlags());
			
			// Project was renamed
			if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
				String newName = delta.getMovedToPath().lastSegment();
				String newPath = delta.getMovedToPath().toString();
				
				rm.renameProject(projectMeta.getProjectID(), newName, newPath);
				System.out.println("sent project rename request: renamed to \"" + newName + "\"; path changed to : " + newPath);
				return false;
			}
			
			// Project was deleted from disk
			System.out.println("unsubscribed from project due to removal from disk");
			PluginManager.getInstance().getRequestManager().unsubscribeFromProject(projectMeta.getProjectID());
			return true;
			
		} else if (delta.getKind() == IResourceDelta.ADDED) {
			return true;
		}
		
		return false;
	}
	
	@Override
	protected void handleFile(IResourceDelta delta) {
		IFile f = (IFile) delta.getResource();
		MetadataManager mm = PluginManager.getInstance().getMetadataManager();
		FileMetadata fileMeta = mm.getFileMetadata(f.getFullPath().removeLastSegments(1).toString());

		if ((delta.getFlags() & IResourceDelta.MOVED_TO) != 0) {
			
			IPath relativeMovedToPath = delta.getMovedToPath().removeFirstSegments(1);
			IPath fullMovedToPath = f.getProject().getFullPath().append(relativeMovedToPath);
			
			if (!relativeMovedToPath.toString().equals(f.getProjectRelativePath().toString())) {
				
				RequestManager rm = PluginManager.getInstance().getRequestManager();
				
				if (f.getName().equals(relativeMovedToPath.lastSegment())) {
					
					try {							
						// process file changes
						IFile movedFile = f.getProject().getFile(relativeMovedToPath);
						
						List<Diff> diffs = generateFileDiffs(f, movedFile);
						// send File.Move request
						rm.moveFile(fileMeta.getFileID(), 
								fullMovedToPath.removeLastSegments(1).toString(), 
								relativeMovedToPath.removeLastSegments(1).toString());
						System.out.println("sent file move request; moving from " +
							f.getProjectRelativePath().toString() + " to " + relativeMovedToPath);
						// send File.Change request
						// TODO: ask about base file version for Patch constructor and sendFileChanges
						rm.sendFileChanges(fileMeta.getFileID(), new String[] { new Patch(0, diffs).toString() }, 0);
						
					} catch (IOException | CoreException e1) {
						System.out.println("Could not generate diffs. Move request and refactored changes were not sent.");
						e1.printStackTrace();
					}
					
				} else {
					
					// send File.Rename request
					String newName = relativeMovedToPath.lastSegment();
					PluginManager.getInstance().getRequestManager().renameFile(fileMeta.getFileID(), newName);
					System.out.println("sent file rename request; changing to " + newName);
					
					// process file changes and send as File.Change 
					IFile movedFile = f.getProject().getFile(relativeMovedToPath);
					try {
						List<Diff> diffs = generateFileDiffs(f, movedFile);
						
						rm.sendFileChanges(fileMeta.getFileID(), new String[] { new Patch(0, diffs).toString() }, 0);
					} catch (IOException | CoreException e1) {
						System.out.println("Could not generate diffs.");
						e1.printStackTrace();
					}
					
				}
				
			}
			
		} else if (delta.getKind() == IResourceDelta.REMOVED) {
			// File was deleted from disk
			if (fileMeta != null) {
				PluginManager.getInstance().getRequestManager().deleteFile(fileMeta.getFileID());
				System.out.println("sent file delete request");
			}
		}
//			else if (delta.getKind() == IResourceDelta.CHANGED) {
//				if ((delta.getFlags() & (IResourceDelta.CONTENT)) != 0) {
//					System.out.println("content");
//				} else if ((delta.getFlags() & IResourceDelta.LOCAL_CHANGED) != 0) {
//					System.out.println("local changed");
//				} else if ((delta.getFlags() & IResourceDelta.DERIVED_CHANGED) != 0) {
//					System.out.println("derived changed");
//				}
//			}
//		}
	}
	
	private List<Diff> generateFileDiffs(IFile oldFile, IFile newFile) throws IOException, CoreException {
//		if (!oldFile.exists()) {
//			throw new IOException("Old file does not exist.");
//		}
		if (!newFile.exists()) {
			throw new IOException("New file does not exist");
		}
		
		String oldContents = new String(this.preChangeListener.getOldFileBytes());
		String newContents = new String(EclipseRequestManager.inputStreamToByteArray(newFile.getContents()));

		return generateStringDiffs(oldContents, newContents);
	}
	
	private List<Diff> generateStringDiffs(String oldContents, String newContents) {
		IRangeComparator oldFile = new TokenComparator(oldContents);
		IRangeComparator newFile = new TokenComparator(newContents);
		RangeDifference[] rangeDiffs = RangeDifferencer.findDifferences(oldFile, newFile);
		
		List<Diff> ccDiffs = new ArrayList<>();
		
		for (RangeDifference r : rangeDiffs) {
			Diff d = null;
			
			// insertion
			if (r.kind() == RangeDifference.RIGHT) {
				d = new Diff(true, r.rightStart(), 
						newContents.substring(r.rightStart(), r.rightStart() + r.rightLength()));
			} 
			// deletion
			else if (r.kind() == RangeDifference.LEFT) {
				d = new Diff(false, r.leftStart(),
						oldContents.substring(r.leftStart(), r.rightStart() + r.rightLength()));
			}
			
			if (d != null) {
				ccDiffs.add(d);
			}
		}
		
		return ccDiffs;
	}
	
}
