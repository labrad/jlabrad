package org.labrad.errors;

import org.labrad.data.Data;

public class LabradException extends Exception {
    private static final long serialVersionUID = 1L;
    private int code;
    private Data payload;
    
    public LabradException(int code, String message) {
        this(code, message, Data.EMPTY);
    }
    
    public LabradException(int code, String message, Throwable cause) {
        this(code, message, Data.EMPTY, cause);
    }
    
    public LabradException(int code, String message, Data payload) {
        super(message);
        this.code = code;
        this.payload = payload;
    }
    
    public LabradException(int code, String message, Data payload, Throwable cause) {
        super(message, cause);
        this.code = code;
        this.payload = payload;
    }
    
    public int getCode() {
        return code;
    }
    
    public Data getPayload() {
        return payload;
    }
}
