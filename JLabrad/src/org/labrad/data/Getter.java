package org.labrad.data;

import org.labrad.types.Type;

public interface Getter<T> {
	/**
	 * The LabRAD type this getter operates on.
	 * @return
	 */
	Type getType();
	
	/**
	 * Get a T object from the given Data object.
	 * @param data
	 * @return
	 */
	T get(Data data);
}
