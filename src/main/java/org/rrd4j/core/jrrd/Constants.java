package org.rrd4j.core.jrrd;

interface Constants {
    final int DS_NAM_SIZE = 20;
    final int DST_SIZE = 20;
    final int CF_NAM_SIZE = 20;
    final  int LAST_DS_LEN = 30;
    static final String COOKIE = "RRD";
    public static final int MAX_SUPPORTED_VERSION = 3;
    public static final String UNDEFINED_VERSION = "UNDEF";
    public static final int UNDEFINED_VERSION_AS_INT = -1;
    public static int VERSION_WITH_LAST_UPDATE_SEC = 3;
    final double FLOAT_COOKIE = 8.642135E130;
    static final byte[] FLOAT_COOKIE_BIG_ENDIAN = {0x5B, 0x1F, 0x2B, 0x43,
        (byte) 0xC7, (byte) 0xC0, 0x25,
        0x2F};
    static final byte[] FLOAT_COOKIE_LITTLE_ENDIAN = {0x2F, 0x25, (byte) 0xC0,
        (byte) 0xC7, 0x43, 0x2B, 0x1F,
        0x5B};
}
