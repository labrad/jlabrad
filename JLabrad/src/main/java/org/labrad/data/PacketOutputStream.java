/*
 * Copyright 2008 Matthew Neeley
 * 
 * This file is part of JLabrad.
 *
 * JLabrad is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JLabrad is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JLabrad.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.labrad.data;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.labrad.types.Type;

/**
 * Output stream that writes LabRAD packets.
 * @author maffoo
 *
 */
public class PacketOutputStream extends BufferedOutputStream {

  public PacketOutputStream(OutputStream out) {
    super(out);
  }

  /**
   * Writes a packet to the output stream.
   * @param packet
   * @throws IOException
   */
  public void writePacket(Packet packet) throws IOException {
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    // flatten records
    for (Record rec : packet.getRecords()) {
      Data recData = new Data(Type.RECORD_TYPE);
      recData.setWord(rec.getID(), 0);
      recData.setString(rec.getData().getTag(), 1);
      recData.setBytes(rec.getData().toBytes(), 2);
      os.write(recData.toBytes());
    }

    // flatten packet header and append records
    Data packetData = new Data(Type.PACKET_TYPE);
    packetData.setWord(packet.getContext().getHigh(), 0);
    packetData.setWord(packet.getContext().getLow(), 1);
    packetData.setInt(packet.getRequest(), 2);
    packetData.setWord(packet.getTarget(), 3);
    packetData.setBytes(os.toByteArray(), 4);

    out.write(packetData.toBytes());
    out.flush();
  }
}
