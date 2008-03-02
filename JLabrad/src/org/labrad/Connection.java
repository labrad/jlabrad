package org.labrad;

import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Connection {
	
	private static final String encoding = "ISO-8859-1";
	private static String password = "martinisgroup";
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			Socket s = new Socket("localhost", 7682);
			PacketInputStream is = new PacketInputStream(s.getInputStream());
			PacketOutputStream os = new PacketOutputStream(s.getOutputStream());
			
			int request = 1;
			long target = 1;
			Record[] records = {};
			Packet packet = new Packet(new Context(1234,5678), target, request, records);
			
			System.out.println("sending: " + packet.toString());
			os.writePacket(packet);
		
			packet = is.readPacket();
			System.out.println("got packet: " + packet.toString());
			
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] challenge = packet.getRecord(0).data.getBytes();
			md.update(challenge);
	    	md.update(password.getBytes(encoding));
	    	
	    	Data data = new Data("s").setBytes(md.digest());
	    	os.writePacket(new Context(0,1), target, request, new Record(0, data));
	    	
	    	packet = is.readPacket();
			System.out.println("got packet: " + packet.toString());
			
			data = new Data("ws").setWord(1, 0).setStr("Java Client", 1);
			os.writePacket(new Context(1,1), target, request, new Record(0, data));
			
			packet = is.readPacket();
			System.out.println("got packet: " + packet.toString());
	    	
			long ID = packet.getRecord(0).data.getWord();
			System.out.println("My ID is: " + ID);
			
			// lookup hydrant server
			data = new Data("s").setStr("Hydrant Server");
			os.writePacket(new Packet(new Context(0, 1), 1, request, new Record(3, data)));
			packet = is.readPacket();
			long hydrantID = packet.getRecord(0).data.getWord();
			
			long start, end;
			
			System.out.println("getting random data, with printing...");
			start = System.currentTimeMillis();
			for (int i = 0; i < 1000; i++) {
				os.writePacket(new Context(0, 1), hydrantID, request, new Record(0));
				packet = is.readPacket();
				System.out.println("got packet: " + packet.toString());
			}
			end = System.currentTimeMillis();
			System.out.println("done.  elapsed: " + (end-start) + " ms.");
			
			System.out.println("getting random data, make pretty, but don't print...");
			start = System.currentTimeMillis();
			for (int i = 0; i < 1000; i++) {
				os.writePacket(new Context(0, 1), hydrantID, request, new Record(0));
				packet = is.readPacket();
				packet.toString();
			}
			end = System.currentTimeMillis();
			System.out.println("done.  elapsed: " + (end-start) + " ms.");
			
			System.out.println("getting random data, no printing...");
			start = System.currentTimeMillis();
			for (int i = 0; i < 1000; i++) {
				os.writePacket(new Context(0, 1), hydrantID, request, new Record(0));
				packet = is.readPacket();
				//System.out.println("got packet: " + packet.toString());
			}
			end = System.currentTimeMillis();
			System.out.println("done.  elapsed: " + (end-start) + " ms.");
			
			System.out.println("pinging manager 10000 times...");
			start = System.currentTimeMillis();
			for (int i = 0; i < 10000; i++) {
				os.writePacket(new Context(0, 1), 1, request);
				packet = is.readPacket();
				//System.out.println("got packet: " + packet.toString());
			}
			end = System.currentTimeMillis();
			System.out.println("done.  elapsed: " + (end-start) + " ms.");
			
		}  catch (NoSuchAlgorithmException e) {
			throw new RuntimeException("MD5 digest not supported.");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unsupported string encoding.");
		} catch (IOException e) {
			System.out.println("IOException.");
			System.exit(1);
		}
	}
}
