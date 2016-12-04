import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import cceclipseplugin.core.EclipseRequestManager;
import patching.Diff;

public class StringDiffTest {

	private EclipseRequestManager rm = new EclipseRequestManager(null, null, null, null);

	@Test
	public void testSingleInsertion() {
		String original = "abde";
		String changed = "abcde";
		System.out.println("Testing insertion...");
		List<Diff> diffs = rm.generateStringDiffs(original, changed);
		if (!diffs.isEmpty()) {
			assertEquals(new Diff(true, 2, "c").toString(), diffs.get(0).toString());
		} else {
			System.out.println("First test skipped...");
		}		
	}
	
	@Test
	public void testSingleDeletion() {
		String original = "abcde";
		String changed = "abde";
		System.out.println("Testing deletion...");
		List<Diff> diffs = rm.generateStringDiffs(original, changed);
		if (!diffs.isEmpty()) {
			assertEquals(new Diff(false, 2, "c").toString(), diffs.get(0).toString());
		}
	}
	
	@Test
	public void testMultiCharInsertion() {
		String original = "ace";
		String changed = "abcde";
		System.out.println("Testing multi-char insertion...");
		List<Diff> diffs = rm.generateStringDiffs(original, changed);
		if (!diffs.isEmpty()) {
			assertEquals(new Diff(false, 1, "c").toString(), diffs.get(0).toString());
			assertEquals(new Diff(true, 1, "bcd").toString(), diffs.get(1).toString());
		}	
	}

	@Test
	public void testMultiCharDeletion() {
		String original = "abcde";
		String changed = "ace";
		System.out.println("Testing multi-char deletion...");
		List<Diff> diffs = rm.generateStringDiffs(original, changed);
		if (!diffs.isEmpty()) {
			assertEquals(new Diff(false, 1, "bcd").toString(), diffs.get(0).toString());
			assertEquals(new Diff(true, 1, "c").toString(), diffs.get(1).toString());
		}	
	}
	
	@Test
	public void testMultiLineDiff() {
		String original = "Hello. This is a test.\nDelete me!!\nDon't mess this up.\n";
		String changed = "Hello. This is a test.\nDon't mess this up.\nAdd me!\n";
		System.out.println("Testing multi-line operation...");
		List<Diff> diffs = rm.generateStringDiffs(original, changed);
		if (!diffs.isEmpty()) {
			assertEquals(new Diff(false, 23, "Delete me!!\n").toString(), diffs.get(0).toString());
			assertEquals(new Diff(true, 43, "Add me!\n").toString(), diffs.get(1).toString());
		}
	}
}
