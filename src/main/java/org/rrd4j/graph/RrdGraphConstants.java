package org.rrd4j.graph;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.Locale;

/**
 * Class to represent various constants used for graphing. No methods are specified.
 */
public interface RrdGraphConstants {
    /**
     * Default graph starting time
     */
    String DEFAULT_START = "end-1d";
    /**
     * Default graph ending time
     */
    String DEFAULT_END = "now";

    /**
     * Constant to represent second
     */
    int SECOND = Calendar.SECOND;
    /**
     * Constant to represent minute
     */
    int MINUTE = Calendar.MINUTE;
    /**
     * Constant to represent hour
     */
    int HOUR = Calendar.HOUR_OF_DAY;
    /**
     * Constant to represent day
     */
    int DAY = Calendar.DAY_OF_MONTH;
    /**
     * Constant to represent week
     */
    int WEEK = Calendar.WEEK_OF_YEAR;
    /**
     * Constant to represent month
     */
    int MONTH = Calendar.MONTH;
    /**
     * Constant to represent year
     */
    int YEAR = Calendar.YEAR;

    /**
     * Constant to represent Monday
     */
    int MONDAY = Calendar.MONDAY;
    /**
     * Constant to represent Tuesday
     */
    int TUESDAY = Calendar.TUESDAY;
    /**
     * Constant to represent Wednesday
     */
    int WEDNESDAY = Calendar.WEDNESDAY;
    /**
     * Constant to represent Thursday
     */
    int THURSDAY = Calendar.THURSDAY;
    /**
     * Constant to represent Friday
     */
    int FRIDAY = Calendar.FRIDAY;
    /**
     * Constant to represent Saturday
     */
    int SATURDAY = Calendar.SATURDAY;
    /**
     * Constant to represent Sunday
     */
    int SUNDAY = Calendar.SUNDAY;

    /**
     * Index of the canvas color. Used in {@link RrdGraphDef#setColor(int, java.awt.Paint)}
     */
    int COLOR_CANVAS = 0;
    /**
     * Index of the background color. Used in {@link RrdGraphDef#setColor(int, java.awt.Paint)}
     */
    int COLOR_BACK = 1;
    /**
     * Index of the top-left graph shade color. Used in {@link RrdGraphDef#setColor(int, java.awt.Paint)}
     */
    int COLOR_SHADEA = 2;
    /**
     * Index of the bottom-right graph shade color. Used in {@link RrdGraphDef#setColor(int, java.awt.Paint)}
     */
    int COLOR_SHADEB = 3;
    /**
     * Index of the minor grid color. Used in {@link RrdGraphDef#setColor(int, java.awt.Paint)}
     */
    int COLOR_GRID = 4;
    /**
     * Index of the major grid color. Used in {@link RrdGraphDef#setColor(int, java.awt.Paint)}
     */
    int COLOR_MGRID = 5;
    /**
     * Index of the font color. Used in {@link RrdGraphDef#setColor(int, java.awt.Paint)}
     */
    int COLOR_FONT = 6;
    /**
     * Index of the frame color. Used in {@link RrdGraphDef#setColor(int, java.awt.Paint)}
     */
    int COLOR_FRAME = 7;
    /**
     * Index of the arrow color. Used in {@link RrdGraphDef#setColor(int, java.awt.Paint)}
     */
    int COLOR_ARROW = 8;

    /**
     * Allowed color names which can be used in {@link RrdGraphDef#setColor(String, java.awt.Paint)} method
     */
    String[] COLOR_NAMES = {
            "canvas", "back", "shadea", "shadeb", "grid", "mgrid", "font", "frame", "arrow"
    };

    /**
     * Default first day of the week (obtained from the default locale)
     */
    int FIRST_DAY_OF_WEEK = Calendar.getInstance(Locale.getDefault()).getFirstDayOfWeek();

    /**
     * Default graph canvas color
     */
    Color DEFAULT_CANVAS_COLOR = Color.WHITE;
    /**
     * Default graph background color
     */
    Color DEFAULT_BACK_COLOR = new Color(245, 245, 245);
    /**
     * Default top-left graph shade color
     */
    Color DEFAULT_SHADEA_COLOR = new Color(200, 200, 200);
    /**
     * Default bottom-right graph shade color
     */
    Color DEFAULT_SHADEB_COLOR = new Color(150, 150, 150);
    /**
     * Default minor grid color
     */
    Color DEFAULT_GRID_COLOR = new Color(171, 171, 171, 95);
    /**
     * Default major grid color
     */
    Color DEFAULT_MGRID_COLOR = new Color(255, 91, 91, 95);
    /**
     * Default font color
     */
    Color DEFAULT_FONT_COLOR = Color.BLACK;
    /**
     * Default frame color
     */
    Color DEFAULT_FRAME_COLOR = Color.BLACK;
    /**
     * Default arrow color
     */
    Color DEFAULT_ARROW_COLOR = Color.RED;

    /**
     * Constant to represent left alignment marker
     */
    String ALIGN_LEFT_MARKER = "\\l";
    /**
     * Constant to represent left alignment marker, without new line
     */
    String ALIGN_LEFTNONL_MARKER = "\\L";
    /**
     * Constant to represent centered alignment marker
     */
    String ALIGN_CENTER_MARKER = "\\c";
    /**
     * Constant to represent right alignment marker
     */
    String ALIGN_RIGHT_MARKER = "\\r";
    /**
     * Constant to represent justified alignment marker
     */
    String ALIGN_JUSTIFIED_MARKER = "\\j";
    /**
     * Constant to represent "glue" marker
     */
    String GLUE_MARKER = "\\g";
    /**
     * Constant to represent vertical spacing marker
     */
    String VERTICAL_SPACING_MARKER = "\\s";
    /**
     * Constant to represent no justification markers
     */
    String NO_JUSTIFICATION_MARKER = "\\J";
    /**
     * Used internally
     */
    String[] MARKERS = {
            ALIGN_LEFT_MARKER, ALIGN_LEFTNONL_MARKER, ALIGN_CENTER_MARKER, ALIGN_RIGHT_MARKER,
            ALIGN_JUSTIFIED_MARKER, GLUE_MARKER, VERTICAL_SPACING_MARKER, NO_JUSTIFICATION_MARKER
    };

    /**
     * Constant to represent in-memory image name
     */
    String IN_MEMORY_IMAGE = "-";

    /**
     * Default units length
     */
    int DEFAULT_UNITS_LENGTH = 9;
    /**
     * Default graph width
     */
    int DEFAULT_WIDTH = 400;
    /**
     * Default graph height
     */
    int DEFAULT_HEIGHT = 100;
    /**
     * Default image format
     */
    String DEFAULT_IMAGE_FORMAT = "gif";
    /**
     * Default image quality, used only for jpeg graphs
     */
    float DEFAULT_IMAGE_QUALITY = 0.8F; // only for jpegs, not used for png/gif
    /**
     * Default value base
     */
    double DEFAULT_BASE = 1000;

    /**
     * Font constructor, to use embedded fonts
     */
    static class FontConstructor {
        static public Font getFont(int type, int size) {
            String fontPath;
            if (type == Font.BOLD)
                fontPath = "/DejaVuSansMono-Bold.ttf";
            else
                fontPath = "/DejaVuSansMono.ttf";

            InputStream fontstream = RrdGraphConstants.class.getResourceAsStream(fontPath);
            try {
                return Font.createFont(Font.TRUETYPE_FONT, fontstream).deriveFont(type, size);
            } catch (FontFormatException e) {
                throw new RuntimeException(e);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            finally {
                try {
                    if (fontstream != null) { 
                        fontstream.close();
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Default graph small font
     */
    static final Font DEFAULT_SMALL_FONT = FontConstructor.getFont(Font.PLAIN, 10);
    /**
     * Default graph large font
     */
    static final Font DEFAULT_LARGE_FONT = FontConstructor.getFont(Font.BOLD, 12);
    /**
     * Font for the Gator
     */
    static final Font GATOR_FONT = FontConstructor.getFont(Font.PLAIN, 9);
    /**
     * Used internally
     */
    double LEGEND_LEADING = 1.2; // chars
    /**
     * Used internally
     */
    double LEGEND_LEADING_SMALL = 0.7; // chars
    /**
     * Used internally
     */
    double LEGEND_BOX_SPACE = 1.2; // chars
    /**
     * Used internally
     */
    double LEGEND_BOX = 0.9; // chars
    /**
     * Used internally
     */
    int LEGEND_INTERSPACING = 2; // chars
    /**
     * Used internally
     */
    int PADDING_LEFT = 10; // pix
    /**
     * Used internally
     */
    int PADDING_TOP = 12; // pix
    /**
     * Used internally
     */
    int PADDING_TITLE = 6; // pix
    /**
     * Used internally
     */
    int PADDING_RIGHT = 16; // pix
    /**
     * Used internally
     */
    int PADDING_PLOT = 2; //chars
    /**
     * Used internally
     */
    int PADDING_LEGEND = 2; // chars
    /**
     * Used internally
     */
    int PADDING_BOTTOM = 6; // pix
    /**
     * Used internally
     */
    int PADDING_VLABEL = 7; // pix

    /**
     * Stroke used to draw grid
     */
    Stroke GRID_STROKE = new BasicStroke(1);

    /**
     * Stroke used to draw ticks
     */
    Stroke TICK_STROKE = new BasicStroke(1);
}
