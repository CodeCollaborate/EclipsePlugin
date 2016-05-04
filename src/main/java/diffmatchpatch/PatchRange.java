package diffmatchpatch;


public class PatchRange implements Comparable<PatchRange> {
	private final int startPos;
	private final int endPos;

	public PatchRange(int startPos, int endPos) {
		this.startPos = startPos;
		this.endPos = endPos;
	}

	/**
	 * @return the startPos
	 */
	public int getStartPos() {
		return startPos;
	}

	/**
	 * @return the endPos
	 */
	public int getEndPos() {
		return endPos;
	}

	@Override
	public String toString() {
		return startPos + ":" + endPos;
	}

	public boolean equals(PatchRange other) {
		return this.startPos == other.startPos && this.endPos == other.endPos;
	}

	@Override
	public int compareTo(PatchRange other) {
		if (this.startPos == other.startPos) {
			return Integer.compare(this.endPos, other.endPos);
		}
		return Integer.compare(this.startPos, other.startPos);
	}

}