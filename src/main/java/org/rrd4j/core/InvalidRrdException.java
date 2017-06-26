package org.rrd4j.core;

/**
 * An exception indicating a corrupted RRD header.
 */
public class InvalidRrdException extends RrdException {

   private static final long serialVersionUID = 1L;

   public InvalidRrdException(String message) {
       super(message);
   }
   
}
