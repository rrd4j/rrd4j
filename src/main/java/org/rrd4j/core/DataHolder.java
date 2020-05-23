package org.rrd4j.core;

import java.util.TimeZone;

import org.rrd4j.ConsolFun;
import org.rrd4j.data.Plottable;
import org.rrd4j.data.Variable;

public interface DataHolder {

   /**
    * Constant that defines the default {@link RrdDbPool} usage policy. Defaults to <code>false</code>
    * (i.e. the pool will not be used to fetch data from RRD files)
    */
   public static final boolean DEFAULT_POOL_USAGE_POLICY = false;

    /**
     * Returns boolean value representing {@link org.rrd4j.core.RrdDbPool RrdDbPool} usage policy.
     *
     * @return true, if the pool will be used internally to fetch data from RRD files, false otherwise.
     */
    boolean isPoolUsed();

    /**
     * Sets the {@link org.rrd4j.core.RrdDbPool RrdDbPool} usage policy.
     *
     * @param poolUsed true, if the pool will be used to fetch data from RRD files, false otherwise.
     */
    void setPoolUsed(boolean poolUsed);

    RrdDbPool getPool();

    /**
     * Defines the {@link org.rrd4j.core.RrdDbPool RrdDbPool} to use. If not defined, but {{@link #setPoolUsed(boolean)}
     * set to true, the default {@link RrdDbPool#getInstance()} will be used.
     * @param pool an optional pool to use.
     */
    void setPool(RrdDbPool pool);

    /**
     * Set the time zone used for the legend.
     *
     * @param tz the time zone to set
     */
    void setTimeZone(TimeZone tz);

    TimeZone getTimeZone();

    /**
     * Sets the time when the graph should end. Time in seconds since epoch
     * (1970-01-01) is required. Negative numbers are relative to the current time.
     *
     * @param time Ending time for the graph in seconds since epoch
     */
    void setEndTime(long time);

    /**
     * Returns ending timestamp.
     *
     * @return Ending timestamp in seconds
     */
    long getEndTime();

    /**
     * Sets the time when the graph should start. Time in seconds since epoch
     * (1970-01-01) is required. Negative numbers are relative to the current time.
     *
     * @param time Starting time for the graph in seconds since epoch
     */
    void setStartTime(long time);

    /**
     * Returns starting timestamp.
     *
     * @return Starting timestamp in seconds
     */
    long getStartTime();

   /**
     * Sets starting and ending time for the for the graph. Timestamps in seconds since epoch are
     * required. Negative numbers are relative to the current time.
     *
     * @param startTime Starting time in seconds since epoch
     * @param endTime   Ending time in seconds since epoch
     */
    void setTimeSpan(long startTime, long endTime);

    /**
     * <p>Roughly corresponds to the --step option in RRDTool's graph/xport commands. Here is an explanation borrowed
     * from RRDTool:</p>
     * <p><i>"By default rrdgraph calculates the width of one pixel in the time
     * domain and tries to get data at that resolution from the RRD. With
     * this switch you can override this behavior. If you want rrdgraph to
     * get data at 1 hour resolution from the RRD, then you can set the
     * step to 3600 seconds. Note, that a step smaller than 1 pixel will
     * be silently ignored."</i></p>
     * <p>I think this option is not that useful, but it's here just for compatibility.</p>
     * @param step Time step at which data should be fetched from RRD files. If this method is not used,
     *             the step will be equal to the smallest RRD step of all processed RRD files. If no RRD file is processed,
     *             the step will be roughly equal to the with of one graph pixel (in seconds).
     */
    void setStep(long step);

    /**
     * Returns the time step used for data processing.
     *
     * @return Step used for data processing.
     */
    long getStep();

    /**
     * Defines virtual datasource. This datasource can then be used
     * in other methods like {@link #datasource(String, String)}.
     *
     * @param name      Source name
     * @param rrdPath   Path to RRD file
     * @param dsName    Datasource name in the specified RRD file
     * @param consolFun Consolidation function (AVERAGE, MIN, MAX, LAST)
     */
    void datasource(String name, String rrdPath, String dsName,
            ConsolFun consolFun);

    /**
     * Defines virtual datasource. This datasource can then be used
     * in other methods like {@link #datasource(String, String)}.
     *
     * @param name      Source name
     * @param rrdPath   Path to RRD file
     * @param dsName    Datasource name in the specified RRD file
     * @param consolFun Consolidation function (AVERAGE, MIN, MAX, LAST)
     * @param backend   Backend to be used while fetching data from a RRD file.
     */
    void datasource(String name, String rrdPath, String dsName,
            ConsolFun consolFun, RrdBackendFactory backend);

    /**
     * Create a new virtual datasource by evaluating a mathematical
     * expression, specified in Reverse Polish Notation (RPN).
     *
     * @param name          Source name
     * @param rpnExpression RPN expression.
     */
    void datasource(String name, String rpnExpression);

    /**
     * Creates a datasource that performs a variable calculation on an
     * another named datasource to yield a single combined timestamp/value.
     *
     * Requires that the other datasource has already been defined; otherwise, it'll
     * end up with no data
     *
     * @param name - the new virtual datasource name
     * @param defName - the datasource from which to extract the percentile. Must be a previously
     *                     defined virtual datasource
     * @param var - a new instance of a Variable used to do the calculation
     */
    void datasource(String name, String defName, Variable var);

    /**
     * Creates a new (plottable) datasource. Datasource values are obtained from the given plottable
     * object.
     *
     * @param name      Source name.
     * @param plottable Plottable object.
     */
    void datasource(String name, Plottable plottable);

    /**
     * Creates a new 'fetched' datasource. Datasource values are obtained from the
     * given {@link org.rrd4j.core.FetchData} object.
     *
     * @param name      Source name.
     * @param fetchData FetchData object.
     */
    void datasource(String name, FetchData fetchData);

    /**
     * Creates a new 'fetched' datasource. Datasource values are obtained from the
     * given {@link org.rrd4j.core.FetchData} object. 
     * Values will be extracted from the datasource dsName in the fetchData
     *
     * @param name      Source name.
     * @param dsName    Source name in fetchData.
     * @param fetchData FetchData object.
     */
    void datasource(String name, String dsName, FetchData fetchData);

}
