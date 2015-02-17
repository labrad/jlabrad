package org.labrad;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.labrad.data.PacketDecoder;

public class TestClientNetty {
  public static void main(String[] args) throws Exception {
    String host = "localhost"; //args[0];
    int port = 7667; //Integer.parseInt(args[1]);

    ChannelFactory factory =
        new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());

    ClientBootstrap bootstrap = new ClientBootstrap(factory);

    PacketDecoder handler = new PacketDecoder();
    bootstrap.getPipeline().addLast("handler", handler);
    
    bootstrap.setOption("tcpNoDelay", true);
    bootstrap.setOption("keepAlive", true);

    bootstrap.connect(new InetSocketAddress(host, port));
  }
}
