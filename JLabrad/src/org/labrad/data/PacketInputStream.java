package org.labrad.data;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.labrad.types.Type;

public class PacketInputStream extends FilterInputStream {

    public PacketInputStream(InputStream in) {
        super(in);
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
        return new Packet(new Context(ctxHigh, ctxLow), source, request, records);
    }
    
    /**
     * Reads the specified number of bytes from the input stream.
     * @param n
     * @return
     * @throws IOException
     */
    private byte[] readBytes(int n) throws IOException {
        byte[] data = new byte[n];
        int totalRead = 0;

        while (totalRead < n) {
            int bytesRead = in.read(data, totalRead, n - totalRead);
            if (bytesRead < 0) {
                throw new IOException("Socket closed.");
            }
            totalRead += bytesRead;
        }
        return data;
    }
}
