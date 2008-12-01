package org.labrad.data;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.labrad.types.Type;

public class PacketInputStream extends FilterInputStream {

    // used for generic method call to convert List<Record> into Record[]
    private static final Record[] RECORD_ARRAY = {};
    
    public PacketInputStream(InputStream in) {
        super(in);
    }

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

    /**
     * Reads a single packet from the input stream.
     * @return
     * @throws IOException
     */
    public Packet readPacket() throws IOException {
        long ctxHigh, ctxLow, source, dataLen;
        int request;

        byte[] hdr = readBytes(20);
        Data hdrdata = Data.unflatten(hdr, Type.HEADER_TYPE);
        ctxHigh = hdrdata.getWord(0);
        ctxLow = hdrdata.getWord(1);
        request = hdrdata.getInt(2);
        source = hdrdata.getWord(3);
        dataLen = hdrdata.getWord(4);

        byte[] recbuf = readBytes((int)dataLen);
        ByteArrayInputStream is = new ByteArrayInputStream(recbuf);
        List<Record> records = new ArrayList<Record>();
        while (is.available() > 0) {
            Data recdata = Data.unflatten(is, Type.RECORD_TYPE);
            long ID = recdata.getWord(0);
            String tag = recdata.getString(1);
            byte[] data = recdata.getBytes(2);
            records.add(new Record(ID, Data.unflatten(data, tag)));
        }
        return new Packet(new Context(ctxHigh, ctxLow), source, request,
                          records.toArray(RECORD_ARRAY));
    }
    
    /**
     * Reads the specified number of bytes from the input stream.
     * @param n
     * @return
     * @throws IOException
     */
    private byte[] readBytes(int n) throws IOException {
        byte[] data = new byte[n];
        int bytesRead, totalRead = 0;

        while (totalRead < n) {
            bytesRead = in.read(data, totalRead, n - totalRead);
            if (bytesRead < 0) {
                throw new IOException("Socket closed.");
            }
            totalRead += bytesRead;
        }
        return data;
    }
}
