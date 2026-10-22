package org.rrd4j.core;

import java.io.IOException;
import java.io.Serial;

/**
 * A general purpose RRD4J exception.
 * 
 * @since 3.4 
 */
public class RrdException extends IOException {

    @Serial
    private static final long serialVersionUID = 1L;

   public RrdException(String message) {
      super(message);
   }

   public RrdException(String message, Throwable cause) {
       super(message, cause);
    }

}
