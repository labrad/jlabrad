package org.labrad.data;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;
import org.labrad.types.Type;

public class PacketDecoder extends FrameDecoder {

  @Override
  protected Object decode(ChannelHandlerContext ctx, Channel channel,
      ChannelBuffer buffer) throws Exception {
    // Wait until the header is available.
    if (buffer.readableBytes() < 20) {
      return null;
    }
    
    buffer.markReaderIndex();
    
    // Unpack the header.
    int ctxHigh = buffer.readInt();
    int ctxLow = buffer.readInt();
    int request = buffer.readInt();
    int source = buffer.readInt();
    int dataLen = buffer.readInt();
    
    // Wait until the whole data is available.
    if (buffer.readableBytes() < dataLen) {
      buffer.resetReaderIndex();
      return null;
    }
    
    // Unpack the received data into a list of records.
    byte[] decoded = new byte[dataLen];
    buffer.readBytes(decoded);
    ByteArrayInputStream is = new ByteArrayInputStream(decoded);
    
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
}
