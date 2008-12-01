package org.labrad.data;


public class Packet {
    private Context context;

    private long target;

    private int request;

    Record[] records;

    public Packet(Context context, long target, int request, Record... records) {
        this.context = context;
        this.target = target;
        this.request = request;
        this.records = records;
    }

    public String toString() {
        String recStr = "";
        if (records.length > 0) {
            for (Record rec : records) {
                recStr += ", " + rec.toString();
            }
            recStr = recStr.substring(2);
        }
        return "Packet(" + "context=" + context.toString()
                         + ", target=" + Long.toString(target)
                         + ", request=" + Integer.toString(request)
                         + ", records=[" + recStr + "]" + ")";
    }

    public int size() { return records.length; }
    public Record[] getRecords() { return records; }
    public Record getRecord(int index) { return records[index]; }
	public Context getContext() { return context; }
	public long getTarget() { return target; }
	public int getRequest() { return request; }
}
