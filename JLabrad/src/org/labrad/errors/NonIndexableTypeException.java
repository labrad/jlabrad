package org.labrad.errors;

import org.labrad.types.Type;

public class NonIndexableTypeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public NonIndexableTypeException(Type type) {
		super(type.getCode() + " cannot be indexed.");
	}
}
