package org.rrd4j.graph;

import org.rrd4j.ConsolFun;
import org.rrd4j.core.Util;
import org.rrd4j.data.DataProcessor;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PrintText extends CommentText {
    static final String UNIT_MARKER = "([^%]?)%(s|S)";
    static final Pattern UNIT_PATTERN = Pattern.compile(UNIT_MARKER);

    private final String srcName;
    private ConsolFun consolFun;
    private final boolean includedInGraph;

    PrintText(String srcName, ConsolFun consolFun, String text, boolean includedInGraph) {
        super(text);
        this.srcName = srcName;
        this.consolFun = consolFun;
        this.includedInGraph = includedInGraph;
    }

    boolean isPrint() {
        return !includedInGraph;
    }

    void resolveText(Locale l, DataProcessor dproc, ValueScaler valueScaler) {
        super.resolveText(l, dproc, valueScaler);
        if (resolvedText != null) {
            double value = dproc.getAggregate(srcName, consolFun);
            Matcher matcher = UNIT_PATTERN.matcher(resolvedText);
            if (matcher.find()) {
                // unit specified
                ValueScaler.Scaled scaled = valueScaler.scale(value, matcher.group(2).equals("s"));
                resolvedText = resolvedText.substring(0, matcher.start()) +
                        matcher.group(1) + scaled.unit + resolvedText.substring(matcher.end());
                value = scaled.value;
            }
            resolvedText = Util.sprintf(l, resolvedText, value);
            trimIfGlue();
        }
    }
}
