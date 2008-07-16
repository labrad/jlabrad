package org.labrad;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.labrad.data.Context;
import org.labrad.data.Packet;
import org.labrad.data.PacketInputStream;
import org.labrad.data.PacketOutputStream;
import org.labrad.data.Record;

public class Client {
    Reader reader;

    Writer writer;

    Socket socket;

    String host;

    BlockingQueue<Packet> writeQueue;

    int port;

    int nextRequest = 1;

    class Reader extends Thread {
        PacketInputStream is;

        Reader(PacketInputStream is) {
            this.is = is;
        }

        public void run() {
            try {
                while (!Thread.interrupted()) {
                    //Packet p = is.readPacket();
                    is.readPacket();
                }
            } catch (IOException e) {
                System.out.println("IOException in Reader Thread.");
            }
        }
    }

    class Writer extends Thread {
        BlockingQueue<Packet> queue;
        PacketOutputStream os;

        Writer(BlockingQueue<Packet> queue, PacketOutputStream os) {
            this.queue = queue;
            this.os = os;
        }

        public void run() {
            try {
                while (true) {
                    Packet p = queue.take();
                    os.writePacket(p);
                }
            } catch (InterruptedException e) {
                System.out.println("Writer Thread interrupted.");
            } catch (IOException e) {
                System.out.println("IOException in Writer Thread.");
            }
        }
    }

    public void sendRequest(long server, Record... records) {
        Context context = new Context(0, 1);
        writeQueue.add(new Packet(context, server, nextRequest, records));
        nextRequest++;
        //return new Response();
    }

    Client(String host, int port) {
        this.host = host;
        this.port = port;

        try {
            socket = new Socket("localhost", 7682);
            PacketInputStream is = new PacketInputStream(socket.getInputStream());
            PacketOutputStream os = new PacketOutputStream(socket.getOutputStream());

            writeQueue = new LinkedBlockingQueue<Packet>();

            reader = new Reader(is);
            writer = new Writer(writeQueue, os);
            reader.start();
            writer.start();
        } catch (UnknownHostException e) {

        } catch (IOException e) {

        }
    }

}
