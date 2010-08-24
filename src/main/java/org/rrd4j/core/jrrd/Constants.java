/*
 * Copyright (C) 2001 Ciaran Treanor <ciaran@codeloop.com>
 *
 * Distributable under GPL license.
 * See terms of license at gnu.org.
 *
 * $Id: Constants.java,v 1.1 2006/02/03 08:27:16 sasam Exp $
 */
package org.rrd4j.core.jrrd;

interface Constants {

    int DS_NAM_SIZE = 20;
    int DST_SIZE = 20;
    int CF_NAM_SIZE = 20;
    int LAST_DS_LEN = 30;
    static String COOKIE = "RRD";
    static String VERSION = "0001";
    double FLOAT_COOKIE = 8.642135E130;
    static byte[] FLOAT_COOKIE_BIG_ENDIAN = {0x5B, 0x1F, 0x2B, 0x43,
            (byte) 0xC7, (byte) 0xC0, 0x25,
            0x2F};
    static byte[] FLOAT_COOKIE_LITTLE_ENDIAN = {0x2F, 0x25, (byte) 0xC0,
            (byte) 0xC7, 0x43, 0x2B, 0x1F,
            0x5B};
}
