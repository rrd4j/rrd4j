package org.rrd4j.data;

import org.rrd4j.core.Util;

import java.util.Calendar;
import java.util.StringTokenizer;

class RpnCalculator {
    private static final byte TKN_VAR = 0;
    private static final byte TKN_NUM = 1;

    // Arithmetics
    private static final byte TKN_PLUS = 2;
    private static final byte TKN_MINUS = 3;
    private static final byte TKN_MULT = 4;
    private static final byte TKN_DIV = 5;
    private static final byte TKN_MOD = 6;

    private static final byte TKN_SIN = 7;
    private static final byte TKN_COS = 8;
    private static final byte TKN_LOG = 9;
    private static final byte TKN_EXP = 10;
    private static final byte TKN_SQRT = 11;
    private static final byte TKN_ATAN = 12;
    private static final byte TKN_ATAN2 = 13;

    private static final byte TKN_FLOOR = 14;
    private static final byte TKN_CEIL = 15;

    private static final byte TKN_DEG2RAD = 16;
    private static final byte TKN_RAD2DEG = 17;
    private static final byte TKN_ROUND = 18;
    private static final byte TKN_POW = 19;
    private static final byte TKN_ABS = 20;
    private static final byte TKN_RANDOM = 21;

    // Boolean operators
    private static final byte TKN_LT = 22;
    private static final byte TKN_LE = 23;
    private static final byte TKN_GT = 24;
    private static final byte TKN_GE = 25;
    private static final byte TKN_EQ = 26;
    private static final byte TKN_NE = 27;
    private static final byte TKN_IF = 28;

    // Comparing values
    private static final byte TKN_MIN = 29;
    private static final byte TKN_MAX = 30;
    private static final byte TKN_LIMIT = 31;

    // Processing the stack directly
    private static final byte TKN_DUP = 32;
    private static final byte TKN_EXC = 33;
    private static final byte TKN_POP = 34;

    // Special values
    private static final byte TKN_UN = 35;
    private static final byte TKN_UNKN = 36;
    private static final byte TKN_NOW = 37;

    private static final byte TKN_TIME = 38;
    private static final byte TKN_PI = 39;
    private static final byte TKN_E = 40;

    private static final byte TKN_AND = 41;
    private static final byte TKN_OR = 42;
    private static final byte TKN_XOR = 43;

    private static final byte TKN_PREV = 44;
    private static final byte TKN_INF = 45;
    private static final byte TKN_NEGINF = 46;
    private static final byte TKN_STEP = 47;

    private static final byte TKN_YEAR = 48;
    private static final byte TKN_MONTH = 49;
    private static final byte TKN_DATE = 50;
    private static final byte TKN_HOUR = 51;
    private static final byte TKN_MINUTE = 52;
    private static final byte TKN_SECOND = 53;
    private static final byte TKN_WEEK = 54;

    private static final byte TKN_SIGN = 55;
    private static final byte TKN_RND = 56;

    private final String rpnExpression;
    private final String sourceName;
    private final DataProcessor dataProcessor;

    private final Token[] tokens;
    private final RpnStack stack = new RpnStack();
    private final double[] calculatedValues;
    private final long[] timestamps;
    private final double timeStep;

    RpnCalculator(String rpnExpression, String sourceName, DataProcessor dataProcessor) {
        this.rpnExpression = rpnExpression;
        this.sourceName = sourceName;
        this.dataProcessor = dataProcessor;
        this.timestamps = dataProcessor.getTimestamps();
        this.timeStep = this.timestamps[1] - this.timestamps[0];
        this.calculatedValues = new double[this.timestamps.length];

        StringTokenizer st = new StringTokenizer(rpnExpression, ", ");
        tokens = new Token[st.countTokens()];
        for (int i = 0; st.hasMoreTokens(); i++) {
            tokens[i] = createToken(st.nextToken());
        }
    }

    private Token createToken(String parsedText) {
        Token token = new Token();
        if (Util.isDouble(parsedText)) {
            token.id = TKN_NUM;
            token.number = Util.parseDouble(parsedText);
        }
        else if (parsedText.equals("+")) {
            token.id = TKN_PLUS;
        }
        else if (parsedText.equals("-")) {
            token.id = TKN_MINUS;
        }
        else if (parsedText.equals("*")) {
            token.id = TKN_MULT;
        }
        else if (parsedText.equals("/")) {
            token.id = TKN_DIV;
        }
        else if (parsedText.equals("%")) {
            token.id = TKN_MOD;
        }
        else if (parsedText.equals("SIN")) {
            token.id = TKN_SIN;
        }
        else if (parsedText.equals("COS")) {
            token.id = TKN_COS;
        }
        else if (parsedText.equals("LOG")) {
            token.id = TKN_LOG;
        }
        else if (parsedText.equals("EXP")) {
            token.id = TKN_EXP;
        }
        else if (parsedText.equals("ATAN")) {
            token.id = TKN_ATAN;
        }
        else if (parsedText.equals("ATAN2")) {
            token.id = TKN_ATAN2;
        }
        else if (parsedText.equals("FLOOR")) {
            token.id = TKN_FLOOR;
        }
        else if (parsedText.equals("CEIL")) {
            token.id = TKN_CEIL;
        }
        else if (parsedText.equals("DEG2RAD")) {
            token.id = TKN_DEG2RAD;
        }
        else if (parsedText.equals("RAD2DEG")) {
            token.id = TKN_RAD2DEG;
        }
        else if (parsedText.equals("ROUND")) {
            token.id = TKN_ROUND;
        }
        else if (parsedText.equals("POW")) {
            token.id = TKN_POW;
        }
        else if (parsedText.equals("ABS")) {
            token.id = TKN_ABS;
        }
        else if (parsedText.equals("SQRT")) {
            token.id = TKN_SQRT;
        }
        else if (parsedText.equals("RANDOM")) {
            token.id = TKN_RANDOM;
        }
        else if (parsedText.equals("LT")) {
            token.id = TKN_LT;
        }
        else if (parsedText.equals("LE")) {
            token.id = TKN_LE;
        }
        else if (parsedText.equals("GT")) {
            token.id = TKN_GT;
        }
        else if (parsedText.equals("GE")) {
            token.id = TKN_GE;
        }
        else if (parsedText.equals("EQ")) {
            token.id = TKN_EQ;
        }
        else if (parsedText.equals("NE")) {
            token.id = TKN_NE;
        }
        else if (parsedText.equals("IF")) {
            token.id = TKN_IF;
        }
        else if (parsedText.equals("MIN")) {
            token.id = TKN_MIN;
        }
        else if (parsedText.equals("MAX")) {
            token.id = TKN_MAX;
        }
        else if (parsedText.equals("LIMIT")) {
            token.id = TKN_LIMIT;
        }
        else if (parsedText.equals("DUP")) {
            token.id = TKN_DUP;
        }
        else if (parsedText.equals("EXC")) {
            token.id = TKN_EXC;
        }
        else if (parsedText.equals("POP")) {
            token.id = TKN_POP;
        }
        else if (parsedText.equals("UN")) {
            token.id = TKN_UN;
        }
        else if (parsedText.equals("UNKN")) {
            token.id = TKN_UNKN;
        }
        else if (parsedText.equals("NOW")) {
            token.id = TKN_NOW;
        }
        else if (parsedText.equals("TIME")) {
            token.id = TKN_TIME;
        }
        else if (parsedText.equals("PI")) {
            token.id = TKN_PI;
        }
        else if (parsedText.equals("E")) {
            token.id = TKN_E;
        }
        else if (parsedText.equals("AND")) {
            token.id = TKN_AND;
        }
        else if (parsedText.equals("OR")) {
            token.id = TKN_OR;
        }
        else if (parsedText.equals("XOR")) {
            token.id = TKN_XOR;
        }
        else if (parsedText.equals("PREV")) {
            token.id = TKN_PREV;
            token.variable = sourceName;
            token.values = calculatedValues;
        }
        else if (parsedText.startsWith("PREV(") && parsedText.endsWith(")")) {
            token.id = TKN_PREV;
            token.variable = parsedText.substring(5, parsedText.length() - 1);
            token.values = dataProcessor.getValues(token.variable);
        }
        else if (parsedText.equals("INF")) {
            token.id = TKN_INF;
        }
        else if (parsedText.equals("NEGINF")) {
            token.id = TKN_NEGINF;
        }
        else if (parsedText.equals("STEP")) {
            token.id = TKN_STEP;
        }
        else if (parsedText.equals("YEAR")) {
            token.id = TKN_YEAR;
        }
        else if (parsedText.equals("MONTH")) {
            token.id = TKN_MONTH;
        }
        else if (parsedText.equals("DATE")) {
            token.id = TKN_DATE;
        }
        else if (parsedText.equals("HOUR")) {
            token.id = TKN_HOUR;
        }
        else if (parsedText.equals("MINUTE")) {
            token.id = TKN_MINUTE;
        }
        else if (parsedText.equals("SECOND")) {
            token.id = TKN_SECOND;
        }
        else if (parsedText.equals("WEEK")) {
            token.id = TKN_WEEK;
        }
        else if (parsedText.equals("SIGN")) {
            token.id = TKN_SIGN;
        }
        else if (parsedText.equals("RND")) {
            token.id = TKN_RND;
        }
        else {
            token.id = TKN_VAR;
            token.variable = parsedText;
            token.values = dataProcessor.getValues(token.variable);
        }
        return token;
    }

    double[] calculateValues() {
        for (int slot = 0; slot < timestamps.length; slot++) {
            resetStack();
            for (Token token : tokens) {
                double x1, x2, x3;
                switch (token.id) {
                    case TKN_NUM:
                        push(token.number);
                        break;
                    case TKN_VAR:
                        push(token.values[slot]);
                        break;
                    case TKN_PLUS:
                        push(pop() + pop());
                        break;
                    case TKN_MINUS:
                        x2 = pop();
                        x1 = pop();
                        push(x1 - x2);
                        break;
                    case TKN_MULT:
                        push(pop() * pop());
                        break;
                    case TKN_DIV:
                        x2 = pop();
                        x1 = pop();
                        push(x1 / x2);
                        break;
                    case TKN_MOD:
                        x2 = pop();
                        x1 = pop();
                        push(x1 % x2);
                        break;
                    case TKN_SIN:
                        push(Math.sin(pop()));
                        break;
                    case TKN_COS:
                        push(Math.cos(pop()));
                        break;
                    case TKN_LOG:
                        push(Math.log(pop()));
                        break;
                    case TKN_EXP:
                        push(Math.exp(pop()));
                        break;
                    case TKN_ATAN:
                        push(Math.atan(pop()));
                        break;
                    case TKN_ATAN2:
                        x2 = pop();
                        x1 = pop();
                        push(Math.atan2(x1, x2));
                        break;
                    case TKN_FLOOR:
                        push(Math.floor(pop()));
                        break;
                    case TKN_CEIL:
                        push(Math.ceil(pop()));
                        break;
                    case TKN_DEG2RAD:
                        push(Math.toRadians(pop()));
                        break;
                    case TKN_RAD2DEG:
                        push(Math.toDegrees(pop()));
                        break;
                    case TKN_ROUND:
                        push(Math.round(pop()));
                        break;
                    case TKN_POW:
                        x2 = pop();
                        x1 = pop();
                        push(Math.pow(x1, x2));
                        break;
                    case TKN_ABS:
                        push(Math.abs(pop()));
                        break;
                    case TKN_SQRT:
                        push(Math.sqrt(pop()));
                        break;
                    case TKN_RANDOM:
                        push(Math.random());
                        break;
                    case TKN_LT:
                        x2 = pop();
                        x1 = pop();
                        push(x1 < x2 ? 1 : 0);
                        break;
                    case TKN_LE:
                        x2 = pop();
                        x1 = pop();
                        push(x1 <= x2 ? 1 : 0);
                        break;
                    case TKN_GT:
                        x2 = pop();
                        x1 = pop();
                        push(x1 > x2 ? 1 : 0);
                        break;
                    case TKN_GE:
                        x2 = pop();
                        x1 = pop();
                        push(x1 >= x2 ? 1 : 0);
                        break;
                    case TKN_EQ:
                        x2 = pop();
                        x1 = pop();
                        push(x1 == x2 ? 1 : 0);
                        break;
                    case TKN_NE:
                        x2 = pop();
                        x1 = pop();
                        push(x1 != x2 ? 1 : 0);
                        break;
                    case TKN_IF:
                        x3 = pop();
                        x2 = pop();
                        x1 = pop();
                        push(x1 != 0 ? x2 : x3);
                        break;
                    case TKN_MIN:
                        push(Math.min(pop(), pop()));
                        break;
                    case TKN_MAX:
                        push(Math.max(pop(), pop()));
                        break;
                    case TKN_LIMIT:
                        x3 = pop();
                        x2 = pop();
                        x1 = pop();
                        push(x1 < x2 || x1 > x3 ? Double.NaN : x1);
                        break;
                    case TKN_DUP:
                        push(peek());
                        break;
                    case TKN_EXC:
                        x2 = pop();
                        x1 = pop();
                        push(x2);
                        push(x1);
                        break;
                    case TKN_POP:
                        pop();
                        break;
                    case TKN_UN:
                        push(Double.isNaN(pop()) ? 1 : 0);
                        break;
                    case TKN_UNKN:
                        push(Double.NaN);
                        break;
                    case TKN_NOW:
                        push(Util.getTime());
                        break;
                    case TKN_TIME:
                        push((long) Math.round(timestamps[slot]));
                        break;
                    case TKN_PI:
                        push(Math.PI);
                        break;
                    case TKN_E:
                        push(Math.E);
                        break;
                    case TKN_AND:
                        x2 = pop();
                        x1 = pop();
                        push((x1 != 0 && x2 != 0) ? 1 : 0);
                        break;
                    case TKN_OR:
                        x2 = pop();
                        x1 = pop();
                        push((x1 != 0 || x2 != 0) ? 1 : 0);
                        break;
                    case TKN_XOR:
                        x2 = pop();
                        x1 = pop();
                        push(((x1 != 0 && x2 == 0) || (x1 == 0 && x2 != 0)) ? 1 : 0);
                        break;
                    case TKN_PREV:
                        push((slot == 0) ? Double.NaN : token.values[slot - 1]);
                        break;
                    case TKN_INF:
                        push(Double.POSITIVE_INFINITY);
                        break;
                    case TKN_NEGINF:
                        push(Double.NEGATIVE_INFINITY);
                        break;
                    case TKN_STEP:
                        push(timeStep);
                        break;
                    case TKN_YEAR:
                        push(getCalendarField(pop(), Calendar.YEAR));
                        break;
                    case TKN_MONTH:
                        push(getCalendarField(pop(), Calendar.MONTH));
                        break;
                    case TKN_DATE:
                        push(getCalendarField(pop(), Calendar.DAY_OF_MONTH));
                        break;
                    case TKN_HOUR:
                        push(getCalendarField(pop(), Calendar.HOUR_OF_DAY));
                        break;
                    case TKN_MINUTE:
                        push(getCalendarField(pop(), Calendar.MINUTE));
                        break;
                    case TKN_SECOND:
                        push(getCalendarField(pop(), Calendar.SECOND));
                        break;
                    case TKN_WEEK:
                        push(getCalendarField(pop(), Calendar.WEEK_OF_YEAR));
                        break;
                    case TKN_SIGN:
                        x1 = pop();
                        push(Double.isNaN(x1) ? Double.NaN : x1 > 0 ? +1 : x1 < 0 ? -1 : 0);
                        break;
                    case TKN_RND:
                        push(Math.floor(pop() * Math.random()));
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected RPN token encountered, token.id=" + token.id);
                }
            }
            calculatedValues[slot] = pop();
            // check if stack is empty only on the first try
            if (slot == 0 && !isStackEmpty()) {
                throw new IllegalArgumentException("Stack not empty at the end of calculation. " +
                        "Probably bad RPN expression [" + rpnExpression + "]");
            }
        }
        return calculatedValues;
    }

    private double getCalendarField(double timestamp, int field) {
        Calendar calendar = Util.getCalendar((long) (timestamp * 1000));
        return calendar.get(field);
    }

    private void push(double x) {
        stack.push(x);
    }

    private double pop() {
        return stack.pop();
    }

    private double peek() {
        return stack.peek();
    }

    private void resetStack() {
        stack.reset();
    }

    private boolean isStackEmpty() {
        return stack.isEmpty();
    }

    private static final class RpnStack {
        private static final int MAX_STACK_SIZE = 1000;
        private double[] stack = new double[MAX_STACK_SIZE];
        private int pos = 0;

        void push(double x) {
            if (pos >= MAX_STACK_SIZE) {
                throw new IllegalArgumentException("PUSH failed, RPN stack full [" + MAX_STACK_SIZE + "]");
            }
            stack[pos++] = x;
        }

        double pop() {
            if (pos <= 0) {
                throw new IllegalArgumentException("POP failed, RPN stack is empty");
            }
            return stack[--pos];
        }

        double peek() {
            if (pos <= 0) {
                throw new IllegalArgumentException("PEEK failed, RPN stack is empty");
            }
            return stack[pos - 1];
        }

        void reset() {
            pos = 0;
        }

        boolean isEmpty() {
            return pos <= 0;
        }
    }

    private static final class Token {
        byte id = -1;
        double number = Double.NaN;
        String variable = null;
        double[] values = null;
    }
}
