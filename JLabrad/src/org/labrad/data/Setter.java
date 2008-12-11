package org.labrad.data;

import org.labrad.types.Type;

public interface Setter<T> {
	/**
	 * Get the LabRAD type that this setter sets.
	 * @return
	 */
	Type getType();
	
	/**
	 * Set a Data object with a value of the correct type.
	 * @param data
	 * @param value
	 */
	void set(Data data, T value);
}