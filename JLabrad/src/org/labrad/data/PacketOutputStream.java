package org.labrad.data;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.labrad.types.Type;

public class PacketOutputStream extends BufferedOutputStream {

    public PacketOutputStream(OutputStream out) {
        super(out);
    }

    public void writePacket(Packet packet) throws IOException {
        writePacket(packet.getContext(), packet.getTarget(),
        		    packet.getRequest(), packet.getRecords());
    }

    public void writePacket(Context context, long target, int request,
    		Record... records) throws IOException {
    	writePacket(context.high, context.low, target, request, records);
    }
    
    public void writePacket(long high, long low, long target, int request,
            Record... records) throws IOException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        // flatten records
        for (Record rec : records) {
            Data recData = new Data(Type.RECORD_TYPE);
            recData.setWord(rec.getID(), 0);
            recData.setStr(rec.getData().getTag(), 1);
            recData.setBytes(rec.getData().flatten(), 2);
            os.write(recData.flatten());
        }

        // flatten packet header and append records
        Data packetData = new Data(Type.PACKET_TYPE);
        packetData.setWord(high, 0);
        packetData.setWord(low, 1);
        packetData.setInt(request, 2);
        packetData.setWord(target, 3);
        packetData.setBytes(os.toByteArray(), 4);

        out.write(packetData.flatten());
        out.flush();
    }
}
