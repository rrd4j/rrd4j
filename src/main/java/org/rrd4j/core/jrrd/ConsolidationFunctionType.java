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
public enum ConsolidationFunctionType {
    AVERAGE, MIN, MAX, LAST, HWPREDICT, SEASONAL, DEVPREDICT, DEVSEASONAL, FAILURES, MHWPREDICT  
}
