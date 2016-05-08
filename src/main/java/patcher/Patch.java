package patcher;

import java.util.ArrayList;
import java.util.List;

public class Patch {

	private final int baseVersion;
	private final List<Diff> diffs;

	public Patch(int baseVersion, List<Diff> diffs) {
		this.baseVersion = baseVersion;
		this.diffs = diffs;
	}

	public Patch(String str) {
		String[] parts = str.split(":\n");
		this.baseVersion = Integer.parseInt(parts[0].substring(1));

		String[] diffStrs = parts[1].split(",\n");
		diffs = new ArrayList<>();

		for (String diffStr : diffStrs) {
			diffs.add(new Diff(diffStr));
		}
	}

	public List<Diff> getDiffs() {
		return diffs;
	}

	public int getBaseVersion() {
		return baseVersion;
	}

	public Patch convertToCRLF(String base) {
		List<Diff> CRLFDiffs = new ArrayList<Diff>();
		for (Diff diff : diffs) {
			CRLFDiffs.add(diff.convertToCRLF(base));
		}
		return new Patch(baseVersion, CRLFDiffs);
	}

	public Patch convertToLF(String base) {
		List<Diff> LFDiffs = new ArrayList<Diff>();
		for (Diff diff : diffs) {
			LFDiffs.add(diff.convertToLF(base));
		}
		return new Patch(baseVersion, LFDiffs);
	}

	public Patch getUndo() {
		List<Diff> undoDiffs = new ArrayList<Diff>();
		for (Diff diff : diffs) {
			undoDiffs.add(diff.getUndo());
		}
		return new Patch(baseVersion, undoDiffs);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("v");
		sb.append(baseVersion);

		sb.append(":\n");
		for (Diff diff : diffs) {
			sb.append(diff);
			sb.append(",\n");
		}

		return sb.substring(0, sb.length() - 2);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + baseVersion;
		result = prime * result + ((diffs == null) ? 0 : diffs.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Patch other = (Patch) obj;
		if (baseVersion != other.baseVersion)
			return false;
		if (diffs == null) {
			if (other.diffs != null)
				return false;
		} else if (!diffs.equals(other.diffs))
			return false;
		return true;
	}

	public Patch transform(List<Patch> patches) {
		return transform(patches.toArray(new Patch[patches.size()]));
	}

	public Patch transform(Patch... patches) {
		List<Diff> intermediateDiffs = diffs;
		int maxVersionSeen = baseVersion;

		for (Patch patch : patches) {
			// Must be able to transform backwards as well?
			List<Diff> newIntermediateDiffs = new ArrayList<>();

			for (Diff diff : intermediateDiffs) {
				newIntermediateDiffs.addAll(diff.transform(patch.diffs));
			}

			intermediateDiffs = newIntermediateDiffs;
			maxVersionSeen = patch.baseVersion;
		}

		return new Patch(maxVersionSeen, intermediateDiffs);
	}
}
