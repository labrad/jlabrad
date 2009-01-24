package org.labrad;

import org.labrad.data.Data;

public interface Handler {
	public Data handle(Data data) throws Exception;
}
