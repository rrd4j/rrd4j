package org.rrd4j.core;

import java.awt.*;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Stack;

/**
 * Extremely simple utility class used to create XML documents.
 */
public class XmlWriter {
    static final String INDENT_STR = "   ";

    private PrintWriter writer;
    private StringBuilder indent = new StringBuilder("");
    private Stack<String> openTags = new Stack<String>();

    /**
     * Creates XmlWriter with the specified output stream to send XML code to.
     *
     * @param stream Output stream which receives XML code
     */
    public XmlWriter(OutputStream stream) {
        writer = new PrintWriter(stream, true);
    }

    /**
     * Opens XML tag
     *
     * @param tag XML tag name
     */
    public void startTag(String tag) {
        writer.println(indent + "<" + tag + ">");
        openTags.push(tag);
        indent.append(INDENT_STR);
    }

    /**
     * Closes the corresponding XML tag
     */
    public void closeTag() {
        String tag = openTags.pop();
        indent.setLength(indent.length() - INDENT_STR.length());
        writer.println(indent + "</" + tag + ">");
    }

    /**
     * Writes &lt;tag&gt;value&lt;/tag&gt; to output stream
     *
     * @param tag   XML tag name
     * @param value value to be placed between <code>&lt;tag&gt</code> and <code>&lt;/tag&gt;</code>
     */
    public void writeTag(String tag, Object value) {
        if (value != null) {
            writer.println(indent + "<" + tag + ">" +
                    escape(value.toString()) + "</" + tag + ">");
        }
        else {
            writer.println(indent + "<" + tag + "></" + tag + ">");
        }
    }

    /**
     * Writes &lt;tag&gt;value&lt;/tag&gt; to output stream
     *
     * @param tag   XML tag name
     * @param value value to be placed between <code>&lt;tag&gt</code> and <code>&lt;/tag&gt;</code>
     */
    public void writeTag(String tag, int value) {
        writeTag(tag, "" + value);
    }

    /**
     * Writes &lt;tag&gt;value&lt;/tag&gt; to output stream
     *
     * @param tag   XML tag name
     * @param value value to be placed between <code>&lt;tag&gt</code> and <code>&lt;/tag&gt;</code>
     */
    public void writeTag(String tag, long value) {
        writeTag(tag, "" + value);
    }

    /**
     * Writes &lt;tag&gt;value&lt;/tag&gt; to output stream
     *
     * @param tag   XML tag name
     * @param value value to be placed between <code>&lt;tag&gt</code> and <code>&lt;/tag&gt;</code>
     */
    public void writeTag(String tag, double value, String nanString) {
        writeTag(tag, Util.formatDouble(value, nanString, true));
    }

    /**
     * Writes &lt;tag&gt;value&lt;/tag&gt; to output stream
     *
     * @param tag   XML tag name
     * @param value value to be placed between <code>&lt;tag&gt</code> and <code>&lt;/tag&gt;</code>
     */
    public void writeTag(String tag, double value) {
        writeTag(tag, Util.formatDouble(value, true));
    }

    /**
     * Writes &lt;tag&gt;value&lt;/tag&gt; to output stream
     *
     * @param tag   XML tag name
     * @param value value to be placed between <code>&lt;tag&gt</code> and <code>&lt;/tag&gt;</code>
     */
    public void writeTag(String tag, boolean value) {
        writeTag(tag, "" + value);
    }

    /**
     * Writes &lt;tag&gt;value&lt;/tag&gt; to output stream
     *
     * @param tag   XML tag name
     * @param value value to be placed between <code>&lt;tag&gt</code> and <code>&lt;/tag&gt;</code>
     */
    public void writeTag(String tag, Color value) {
        int rgb = value.getRGB() & 0xFFFFFF;
        writeTag(tag, "#" + Integer.toHexString(rgb).toUpperCase());
    }

    /**
     * Writes &lt;tag&gt;value&lt;/tag&gt; to output stream
     *
     * @param tag   XML tag name
     * @param value value to be placed between <code>&lt;tag&gt</code> and <code>&lt;/tag&gt;</code>
     */
    public void writeTag(String tag, Font value) {
        startTag(tag);
        writeTag("name", value.getName());
        int style = value.getStyle();
        if ((style & Font.BOLD) != 0 && (style & Font.ITALIC) != 0) {
            writeTag("style", "BOLDITALIC");
        }
        else if ((style & Font.BOLD) != 0) {
            writeTag("style", "BOLD");
        }
        else if ((style & Font.ITALIC) != 0) {
            writeTag("style", "ITALIC");
        }
        else {
            writeTag("style", "PLAIN");
        }
        writeTag("size", value.getSize());
        closeTag();
    }

    /**
     * Writes &lt;tag&gt;value&lt;/tag&gt; to output stream
     *
     * @param tag   XML tag name
     * @param value value to be placed between <code>&lt;tag&gt</code> and <code>&lt;/tag&gt;</code>
     */
    public void writeTag(String tag, File value) {
        writeTag(tag, value.getPath());
    }

    /**
     * Flushes the output stream
     */
    public void flush() {
        writer.flush();
    }

    protected void finalize() {
        writer.close();
    }

    /**
     * Writes XML comment to output stream
     *
     * @param comment comment string
     */
    public void writeComment(Object comment) {
        writer.println(indent + "<!-- " + escape(comment.toString()) + " -->");
	}

	private static String escape(String s) {
		return s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
	}
}
