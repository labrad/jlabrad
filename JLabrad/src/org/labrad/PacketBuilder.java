package org.labrad;

import java.io.IOException;
import java.util.Vector;

import org.labrad.data.Data;
import org.labrad.data.Record;

public class PacketBuilder {
	private SimpleClient sc;
	private ServerProxy server;
	private Vector<Record> records;
	
	public PacketBuilder(SimpleClient sc, ServerProxy server) {
		this.sc = sc;
		this.server = server;
		records = new Vector<Record>();
	}
	
	public Data[] send() throws IOException {
		Record[] request = new Record[records.size()];
		for (int i = 0; i < records.size(); i++) {
			request[i] = records.get(i);
		}
		Record[] response = sc.sendRequest(server.getID(), request);
		Data[] answer = new Data[response.length];
		for (int i = 0; i < response.length; i++) {
			answer[i] = response[i].getData();
		}
		return answer;
	}
	
	public PacketBuilder add(long setting) {
		records.add(new Record(setting));
		return this;
	}
	
	public PacketBuilder add(long setting, Data data) {
		records.add(new Record(setting, data));
		return this;
	}
	
	public PacketBuilder add(String setting) {
		records.add(new Record(setting));
		return this;
	}
	
	public PacketBuilder add(String setting, Data data) {
		records.add(new Record(setting, data));
		return this;
	}
	
	public int addIndexed(long setting) {
		records.add(new Record(setting));
		return records.size()-1;
	}
	
	public int addIndexed(long setting, Data data) {
		records.add(new Record(setting, data));
		return records.size()-1;
	}
	
	public int addIndexed(String setting) {
		records.add(new Record(setting));
		return records.size()-1;
	}
	
	public int addIndexed(String setting, Data data) {
		records.add(new Record(setting, data));
		return records.size()-1;
	}
}
