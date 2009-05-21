package org.labrad.annotations;

import org.labrad.data.Data;

public class Ideas {
	public void doSomething(@Accepts("blah") Data a) {
	}
	
	public void doSomething(String a) {
	}
	
	public void doSomething(boolean a) {
	}
	
	public void doSomething(@Units("m/s") double speed) {
	}
	
	public @Returns("sss") Data doSomething() {
		return null;
	}
}
