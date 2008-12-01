package org.labrad.data;

class ByteArrayView {
	private final byte[] bytes;
	private final int offset;
	
	public ByteArrayView(byte[] bytes, int offset) {
		this.bytes = bytes;
		this.offset = offset;
	}
	
	public byte[] getBytes() {
		return bytes;
	}
	
	public int getOffset() {
		return offset;
	}
}
