package org.labrad;

import org.labrad.data.Request;

public interface LookupService {
	boolean doLookupsFromCache(Request request);
	boolean doLookups(Request request);
}
