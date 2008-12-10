package org.labrad.data;

public class Context {
    private final long high, low;

    public Context(long high, long low) {
        this.high = high;
        this.low = low;
    }

    public long getHigh() { return high; }
    public long getLow() { return low; }
    
    public String toString() {
        return "(" + high + "," + low + ")";
    }

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Context))
			return false;
		Context other = (Context) obj;
		if (high != other.high)
			return false;
		if (low != other.low)
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (high ^ (high >>> 32));
		result = prime * result + (int) (low ^ (low >>> 32));
		return result;
	}
}
