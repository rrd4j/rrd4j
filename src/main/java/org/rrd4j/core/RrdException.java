package org.rrd4j.core;

import java.io.IOException;

/**
 * A general purpose RRD4J exception. 
 */
public class RrdException extends IOException {

   private static final long serialVersionUID = 1L;

   public RrdException(String message) {
      super(message);
   }

   public RrdException(String message, Throwable cause) {
       super(message, cause);
    }

}
