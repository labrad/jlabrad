package org.labrad.data;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

public class PacketEncoder extends OneToOneEncoder {

  @Override
  protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg)
      throws Exception {
    if (!(msg instanceof Data)) {
      // Ignore what this encoder can't encode.
      return msg;
    }
    
    // Convert to a Data first.
    Data data = (Data) msg;
    
    // Convert the number into a byte array.
    byte[] bytes = data.toBytes();
    
    // Construct a message.
    ChannelBuffer buf = ChannelBuffers.dynamicBuffer();
    buf.writeBytes(bytes);
    
    // Return the constructed message.
    return buf;
  }

}
