/*
 * Copyright (C) 2001 Ciaran Treanor <ciaran@codeloop.com>
 *
 * Distributable under GPL license.
 * See terms of license at gnu.org.
 *
 * $Id: ConsolidationFunctionType.java,v 1.1 2006/02/03 08:27:16 sasam Exp $
 */
package org.rrd4j.core.jrrd;

/**
 * Class ConsolidationFunctionType
 *
 * @author <a href="mailto:ciaran@codeloop.com">Ciaran Treanor</a>
 * @version $Revision: 1.1 $
 */
public class ConsolidationFunctionType {

    private static final int _AVERAGE = 0;
    private static final String STR_AVERAGE = "AVERAGE";

    /**
     * Field AVERAGE
     */
    public static final ConsolidationFunctionType AVERAGE =
            new ConsolidationFunctionType(_AVERAGE);
    private static final int _MIN = 1;
    private static final String STR_MIN = "MIN";

    /**
     * Field MIN
     */
    public static final ConsolidationFunctionType MIN =
            new ConsolidationFunctionType(_MIN);
    private static final int _MAX = 2;
    private static final String STR_MAX = "MAX";

    /**
     * Field MAX
     */
    public static final ConsolidationFunctionType MAX =
            new ConsolidationFunctionType(_MAX);
    private static final int _LAST = 3;
    private static final String STR_LAST = "LAST";

    /**
     * Field LAST
     */
    public static final ConsolidationFunctionType LAST =
            new ConsolidationFunctionType(_LAST);
    private int type;

    private ConsolidationFunctionType(int type) {
        this.type = type;
    }

    /**
     * Returns a <code>ConsolidationFunctionType</code> with the given name.
     *
     * @param s name of the <code>ConsolidationFunctionType</code> required.
     * @return a <code>ConsolidationFunctionType</code> with the given name.
     */
    public static ConsolidationFunctionType get(String s) {

        if (s.equalsIgnoreCase(STR_AVERAGE)) {
            return AVERAGE;
        }
        else if (s.equalsIgnoreCase(STR_MIN)) {
            return MIN;
        }
        else if (s.equalsIgnoreCase(STR_MAX)) {
            return MAX;
        }
        else if (s.equalsIgnoreCase(STR_LAST)) {
            return LAST;
        }
        else {
            throw new IllegalArgumentException("Invalid ConsolidationFunctionType");
        }
    }

    /**
     * Compares this object against the specified object.
     *
     * @return <code>true</code> if the objects are the same,
     *         <code>false</code> otherwise.
     */
    public boolean equals(Object o) {

        if (!(o instanceof ConsolidationFunctionType)) {
            throw new IllegalArgumentException("Not a ConsolidationFunctionType");
        }

        return (((ConsolidationFunctionType) o).type == type)
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

            case _AVERAGE:
                strType = STR_AVERAGE;
                break;

            case _MIN:
                strType = STR_MIN;
                break;

            case _MAX:
                strType = STR_MAX;
                break;

            case _LAST:
                strType = STR_LAST;
                break;

            default:
                throw new RuntimeException("This should never happen");
        }

        return strType;
    }
}
