/*
 * Copyright (C) 2001 Ciaran Treanor <ciaran@codeloop.com>
 *
 * Distributable under GPL license.
 * See terms of license at gnu.org.
 *
 * $Id: DataSourceType.java,v 1.1 2006/02/03 08:27:16 sasam Exp $
 */
package org.rrd4j.core.jrrd;

/**
 * Class DataSourceType
 *
 * @author <a href="mailto:ciaran@codeloop.com">Ciaran Treanor</a>
 * @version $Revision: 1.1 $
 */
public class DataSourceType {

    private static final int _COUNTER = 0;
    private static final String STR_COUNTER = "COUNTER";

    /**
     * Field COUNTER
     */
    public static final DataSourceType COUNTER =
            new DataSourceType(_COUNTER);
    private static final int _ABSOLUTE = 1;
    private static final String STR_ABSOLUTE = "ABSOLUTE";

    /**
     * Field ABSOLUTE
     */
    public static final DataSourceType ABSOLUTE =
            new DataSourceType(_ABSOLUTE);
    private static final int _GAUGE = 2;
    private static final String STR_GAUGE = "GAUGE";

    /**
     * Field GAUGE
     */
    public static final DataSourceType GAUGE = new DataSourceType(_GAUGE);
    private static final int _DERIVE = 3;
    private static final String STR_DERIVE = "DERIVE";

    /**
     * Field DERIVE
     */
    public static final DataSourceType DERIVE = new DataSourceType(_DERIVE);
    private int type;

    private DataSourceType(int type) {
        this.type = type;
    }

    /**
     * Returns a <code>DataSourceType</code> with the given name.
     *
     * @param s name of the <code>DataSourceType</code> required.
     * @return a <code>DataSourceType</code> with the given name.
     */
    public static DataSourceType get(String s) {

        if (s.equalsIgnoreCase(STR_COUNTER)) {
            return COUNTER;
        }
        else if (s.equalsIgnoreCase(STR_ABSOLUTE)) {
            return ABSOLUTE;
        }
        else if (s.equalsIgnoreCase(STR_GAUGE)) {
            return GAUGE;
        }
        else if (s.equalsIgnoreCase(STR_DERIVE)) {
            return DERIVE;
        }
        else {
            throw new IllegalArgumentException("Invalid DataSourceType");
        }
    }

    /**
     * Compares this object against the specified object.
     *
     * @return <code>true</code> if the objects are the same,
     *         <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {

        if (!(obj instanceof DataSourceType)) {
            throw new IllegalArgumentException("Not a DataSourceType");
        }

        return (((DataSourceType) obj).type == type)
                ? true
                : false;
    }

    /**
     * Returns a string representation of this object.
     *
     * @return a string representation of this object.
     */
    public String toString() {

        String strType;

        switch (type) {

            case _COUNTER:
                strType = STR_COUNTER;
                break;

            case _ABSOLUTE:
                strType = STR_ABSOLUTE;
                break;

            case _GAUGE:
                strType = STR_GAUGE;
                break;

            case _DERIVE:
                strType = STR_DERIVE;
                break;

            default:
                // Don't you just hate it when you see a line like this?
                throw new RuntimeException("This should never happen");
        }

        return strType;
    }
}
