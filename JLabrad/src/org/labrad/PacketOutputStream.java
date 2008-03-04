package org.labrad;

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
        writePacket(packet.context, packet.target, packet.request, packet.getRecords());
    }

    public void writePacket(Context context, long target, int request,
            Record... records) throws IOException {

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        for (Record rec : records) {
            Data recData = new Data(Type.RECORD_TYPE);
            recData.setWord(rec.ID, 0);
            recData.setStr(rec.data.getTag(), 1);
            recData.setBytes(rec.data.flatten(), 2);
            os.write(recData.flatten());
        }

        Data packetData = new Data(Type.PACKET_TYPE);
        packetData.setWord(context.high, 0);
        packetData.setWord(context.low, 1);
        packetData.setInt(request, 2);
        packetData.setWord(target, 3);
        packetData.setBytes(os.toByteArray(), 4);

        byte[] output = packetData.flatten();
        out.write(output);
        out.flush();
    }
}
