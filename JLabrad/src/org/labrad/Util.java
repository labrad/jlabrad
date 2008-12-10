package org.labrad;

import java.util.Map;

public class Util {
	/**
     * Converts a byte array into a hex string.
     * @param bytes
     * @return
     */
    public static String dumpBytes(byte[] bytes) {
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
    
    /**
	 * Get an environment variable, or fall back on the given default if not found.
	 * @param key
	 * @param defaultVal
	 * @return
	 */
	public static String getEnv(String key, String defaultVal) {
		Map<String, String> env = System.getenv();
		if (env.containsKey(key)) {
			return env.get(key);
		} else {
			return defaultVal;
		}
	}
}
