package org.labrad;

public class Util {
	/**
     * Converts a byte array into a hex string.
     * @param bytes
     * @return
     */
    public String dumpBytes(byte[] bytes) {
        int counter = 0;
        String dump = "";
        for (byte b : bytes) {
            int high = (b & 0xF0) >> 4;
            int low = (b & 0x0F);
            dump += "0123456789ABCDEF".substring(high, high + 1)
                    + "0123456789ABCDEF".substring(low, low + 1);
            counter++;
            if (counter == 4) {
                dump += " ";
                counter = 0;
            }
        }
        return dump;
    }
}
