package org.labrad.data;

public class Complex {
    private final double real, imag;

    public Complex(double real, double imag) {
        this.real = real;
        this.imag = imag;
    }
    
    public double getReal() {
    	return real;
    }
    
    public double getImag() {
    	return imag;
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
		if (!(obj instanceof Complex))
			return false;
		Complex other = (Complex) obj;
		if (Double.doubleToLongBits(imag) != Double
				.doubleToLongBits(other.imag))
			return false;
		if (Double.doubleToLongBits(real) != Double
				.doubleToLongBits(other.real))
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
		long temp;
		temp = Double.doubleToLongBits(imag);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(real);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}
}
