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
    Data hdrdata = Data.fromBytes(hdr, Type.HEADER_TYPE);
    ctxHigh = hdrdata.get(0).getWord();
    ctxLow = hdrdata.get(1).getWord();
    request = hdrdata.get(2).getInt();
    source = hdrdata.get(3).getWord();
    dataLen = hdrdata.get(4).getWord();

    byte[] recbuf = readBytes((int)dataLen);
    ByteArrayInputStream is = new ByteArrayInputStream(recbuf);
    List<Record> records = new ArrayList<Record>();
    while (is.available() > 0) {
      Data recdata = Data.fromBytes(is, Type.RECORD_TYPE);
      long ID = recdata.get(0).getWord();
      String tag = recdata.get(1).getString();
      byte[] data = recdata.get(2).getBytes();
      records.add(new Record(ID, Data.fromBytes(data, Type.fromTag(tag))));
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
