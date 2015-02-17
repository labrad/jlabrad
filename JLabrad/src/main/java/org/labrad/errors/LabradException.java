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

package org.labrad.errors;

import org.labrad.data.Data;

public class LabradException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  private int code;
  private Data payload;

  public LabradException(Data error) {
    this(error.getErrorCode(), error.getErrorMessage(), error.getErrorPayload());
  }

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
