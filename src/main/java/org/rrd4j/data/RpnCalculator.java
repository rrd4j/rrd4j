package org.rrd4j.data;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rrd4j.core.Util;

class RpnCalculator {
    private enum Token_Symbol {
        TKN_VAR("") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(token.values[slot]);
            }
        },
        TKN_NUM("") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(token.number);
            }            
        },

        // Arithmetics
        TKN_PLUS("+") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(c.pop() + c.pop());
            }
        },
        TKN_ADDNAN("ADDNAN") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x1 = c.pop();
                double x2 = c.pop();
                c.push(Double.isNaN(x1) ? x2 : (Double.isNaN(x2) ? x1 : x1 + x2));
            }
        },
        TKN_MINUS("-") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(x1 - x2);
            }
        },
        TKN_MULT("*") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(c.pop() * c.pop());
            }
        },
        TKN_DIV("/") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(x1 / x2);
            }
        },
        TKN_MOD("%") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(x1 % x2);
            }
        },

        TKN_SIN("SIN") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.sin(c.pop()));
            }
        },
        TKN_COS("COS") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.cos(c.pop()));
            }
        },
        TKN_LOG("LOG") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.log(c.pop()));
            }
        },
        TKN_EXP("EXP") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.exp(c.pop()));
            }
        },
        TKN_SQRT("SQRT") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.sqrt(c.pop()));
            }
        },
        TKN_ATAN("ATAN") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.atan(c.pop()));
            }
        },
        TKN_ATAN2("ATAN2") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(Math.atan2(x1, x2));
            }
        },

        TKN_FLOOR("FLOOR") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.floor(c.pop()));
            }
        },
        TKN_CEIL("CEIL") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.ceil(c.pop()));
            }
        },

        TKN_DEG2RAD("DEG2RAD") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.toRadians(c.pop()));
            }
        },
        TKN_RAD2DEG("RAD2DEG") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.toDegrees(c.pop()));
            }
        },
        TKN_ROUND("ROUND") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.round(c.pop()));
            }
        },
        TKN_POW("POW") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(Math.pow(x1, x2));
            }
        },
        TKN_ABS("ABS") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.abs(c.pop()));
            }
        },
        TKN_RANDOM("RANDOM") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.random());
            }
        },
        TKN_RND("RND") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.floor(c.pop() * Math.random()));
            }
        },

        // Boolean operators
        TKN_LT("LT") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(x1 < x2 ? 1 : 0);
            }
        },
        TKN_LE("LE") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(x1 <= x2 ? 1 : 0);
            }
        },
        TKN_GT("GT") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(x1 > x2 ? 1 : 0);
            }
        },
        TKN_GE("GE") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(x1 >= x2 ? 1 : 0);
            }
        },
        TKN_EQ("EQ") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(x1 == x2 ? 1 : 0);
            }
        },
        TKN_NE("NE") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(x1 != x2 ? 1 : 0);
            }
        },
        TKN_IF("IF") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x3 = c.pop();
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(x1 != 0 ? x2 : x3);
            }
        },

        // Comparing values
        TKN_MIN("MIN") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.min(c.pop(), c.pop()));
            }
        },
        TKN_MAX("MAX") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.max(c.pop(), c.pop()));                
            }
        },
        TKN_LIMIT("LIMIT") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x3 = c.pop();
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(x1 < x2 || x1 > x3 ? Double.NaN : x1);
            }
        },

        // Processing the stack directly
        TKN_DUP("DUP") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(c.peek());
            }
        },
        TKN_EXC("EXC") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(x2);
                c.push(x1);
            }
        },
        TKN_POP("POP") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.pop();
            }
        },

        // Special values
        TKN_UN("UN") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Double.isNaN(c.pop()) ? 1 : 0);
            }
        },
        TKN_UNKN("UNKN") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Double.NaN);
            }
        },
        TKN_NOW("NOW") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Util.getTime());
            }
        },

        TKN_PI("PI") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.PI);
            }
        },
        TKN_E("E") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Math.E);
            }
        },
        TKN_INF("INF") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Double.POSITIVE_INFINITY);
            }
        },
        TKN_NEGINF("NEGINF") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(Double.NEGATIVE_INFINITY);
            }
        },

        // Logical operator
        TKN_AND("AND") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push((x1 != 0 && x2 != 0) ? 1 : 0);
            }
        },
        TKN_OR("OR") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push((x1 != 0 || x2 != 0) ? 1 : 0);
            }
        },
        TKN_XOR("XOR") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x2 = c.pop();
                double x1 = c.pop();
                c.push(((x1 != 0 && x2 == 0) || (x1 == 0 && x2 != 0)) ? 1 : 0);
            }
        },

        TKN_PREV("PREV") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push((slot == 0) ? Double.NaN : token.values[slot - 1]);
            }
        },
        TKN_STEP("STEP") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(c.timeStep);
            }
        },
        TKN_TIME("TIME") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(c.timestamps[slot]);
            }
        },
        TKN_YEAR("YEAR") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(c.getCalendarField(c.pop(), Calendar.YEAR));
            }
        },
        TKN_MONTH("MONTH") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(c.getCalendarField(c.pop(), Calendar.MONTH));
            }
        },
        TKN_DATE("DATE") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(c.getCalendarField(c.pop(), Calendar.DAY_OF_MONTH));
            }
        },
        TKN_HOUR("HOUR") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(c.getCalendarField(c.pop(), Calendar.HOUR_OF_DAY));
            }
        },
        TKN_MINUTE("MINUTE") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(c.getCalendarField(c.pop(), Calendar.MINUTE));
            }
        },
        TKN_SECOND("SECOND") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(c.getCalendarField(c.pop(), Calendar.SECOND));
            }
        },
        TKN_WEEK("WEEK") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                c.push(c.getCalendarField(c.pop(), Calendar.WEEK_OF_YEAR));
            }
        },

        TKN_SIGN("SIGN") {
            @Override
            void do_method(RpnCalculator c, Token token, int slot) {
                double x1 = c.pop();
                c.push(Double.isNaN(x1) ? Double.NaN : x1 > 0 ? +1 : x1 < 0 ? -1 : 0);
            }
        };

        public final String token_string;
        Token_Symbol(String token_string) {
            this.token_string = token_string;
        }
        abstract void do_method(RpnCalculator c, Token token, int slot);
    }

    static private final Map<String, Token_Symbol> symbols = new HashMap<String, Token_Symbol>(Token_Symbol.values().length);
    {
        for(Token_Symbol s: Token_Symbol.values()) {
            if(! s.token_string.isEmpty())
                symbols.put(s.token_string, s);
        }
    }
    private final String rpnExpression;
    private final String sourceName;
    private final DataProcessor dataProcessor;

    private final Token[] tokens;
    private final RpnStack stack = new RpnStack();
    private final double[] calculatedValues;
    private final long[] timestamps;
    private final double timeStep;
    private final List<String> sourcesNames;

    RpnCalculator(String rpnExpression, String sourceName, DataProcessor dataProcessor) {
        this.rpnExpression = rpnExpression;
        this.sourceName = sourceName;
        this.dataProcessor = dataProcessor;
        this.timestamps = dataProcessor.getTimestamps();
        this.timeStep = this.timestamps[1] - this.timestamps[0];
        this.calculatedValues = new double[this.timestamps.length];
        this.sourcesNames = Arrays.asList(dataProcessor.getSourceNames());
        String[] tokensString = rpnExpression.split(" *, *");
        tokens = new Token[tokensString.length];
        for (int i = 0; i < tokensString.length; i++) {
            tokens[i] = createToken(tokensString[i].trim());
        }
    }

    private Token createToken(String parsedText) {
        Token token = new Token();
        if (symbols.containsKey(parsedText)){
            token.id = symbols.get(parsedText);
        }
        else if (parsedText.equals("PREV")) {
            token.id = Token_Symbol.TKN_PREV;
            token.variable = sourceName;
            token.values = calculatedValues;
        }
        else if (parsedText.startsWith("PREV(") && parsedText.endsWith(")")) {
            token.id = Token_Symbol.TKN_PREV;
            token.variable = parsedText.substring(5, parsedText.length() - 1);
            token.values = dataProcessor.getValues(token.variable);
        }
        else if (Util.isDouble(parsedText)) {
            token.id = Token_Symbol.TKN_NUM;
            token.number = Util.parseDouble(parsedText);
        }
        else if (sourcesNames.contains(parsedText)){
            token.id = Token_Symbol.TKN_VAR;
            token.variable = parsedText;
            token.values = dataProcessor.getValues(token.variable);
        }
        else {
            throw new IllegalArgumentException("Unexpected RPN token encountered: " +  parsedText);
        }
        return token;
    }

    double[] calculateValues() {
        for (int slot = 0; slot < timestamps.length; slot++) {
            resetStack();
            for (Token token: tokens) {
                token.id.do_method(this, token, slot);
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
        Token_Symbol id;
        double number = Double.NaN;
        String variable = "";
        double[] values = null;
    }
}
