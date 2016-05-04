package patcher;

import cceclipseplugin.utils.Utils;

public class Patch {
	private int startIndex;
	private int removals;
	private String insertions;

	public Patch(int startIndex, int removals, String insertions) {
		this.startIndex = startIndex;
		this.removals = removals;
		this.insertions = insertions;
	}

	public Patch(String str) {
		String[] parts = str.split(":");
		
		if (parts.length != 2){
			throw new IllegalArgumentException("Illegal patch format; should be %d:-%d or %d:+%s");
		}
		
		try {
			this.startIndex = Integer.parseInt(parts[0]);
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid offset: " + parts[0], e);
		}
		switch (parts[1].charAt(0)) {
		case '+':
			this.insertions = Utils.urlDecode(parts[1].substring(1));
			break;
		case '-':
			try {
				this.removals = Integer.parseInt(parts[1].substring(1));
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Invalid removal count: " + parts[1].substring(1), e);
			}
			break;
		default:
			throw new IllegalArgumentException("Invalid operation: " + parts[1].charAt(0));
		}
	}

	public int getStartIndex() {
		return startIndex;
	}

	public void setStartIndex(int startIndex) {
		this.startIndex = startIndex;
	}

	public int getRemovals() {
		return removals;
	}

	public void setRemovals(int removals) {
		this.removals = removals;
	}

	public String getInsertions() {
		return insertions;
	}

	public void setInsertions(String insertions) {
		this.insertions = insertions;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		if (removals > 0) {
			sb.append(String.format("%d:-%d", startIndex, removals));
		}
		if (!insertions.isEmpty()) {
			// TODO: Do we need to change all line-breaks to CRLF?
			// or replace \v with \n?
			sb.append(String.format("%d:+%s", startIndex, Utils.urlEncode(insertions)));
		}

		return sb.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((insertions == null) ? 0 : insertions.hashCode());
		result = prime * result + removals;
		result = prime * result + startIndex;
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
		if (insertions == null) {
			if (other.insertions != null)
				return false;
		} else if (!insertions.equals(other.insertions))
			return false;
		if (removals != other.removals)
			return false;
		if (startIndex != other.startIndex)
			return false;
		return true;
	}

}
