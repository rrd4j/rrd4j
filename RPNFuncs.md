This is a list of RPN operators known in RRD4. One can have more informations at the [rrdtool help page](http://oss.oetiker.ch/rrdtool/doc/rrdgraph_rpn.en.html).
# Boolean operators #
  * LE
  * LT
  * GE
  * GT
  * EQ
  * NE
  * IF
  * UN: Return true if the popped element is NaN

# Logical operators #
The logical operators use the value of the stack as a logical value (0 being false). They return 0 as false and 1 as true.
  * AND
  * OR
  * XOR

# Comparing values #
  * MAX
  * MIN
  * LIMIT

# Arithmetics #
  * +
  * -
  * /
  * `*`
  * %
  * SIN
  * COS
  * LOG
  * SQRT
  * ABS
  * SIGN
  * EXP
  * POW
  * ATAN
  * ATAN2
  * DEG2RAD
  * RAD2DEG
Some rounding functions
  * CEIL
  * FLOOR
  * ROUND

# Constant #
  * PI: π
  * E: _e_
  * UNKN: Pushes an unknown value on the stack.

# Special values #
  * INF
  * NEGINF
  * PREV
  * RANDOM: Return an random number between 0 and 1
  * RND: Pop one element (count) from the stack. Return a random number between 0 and element – 1.
  * STEP: return the time stop from the datasource.

# Time #
  * NOW: Pushes the current time on the stack.
  * TIME: Pushes the time the currently processed value was taken at onto the stack.
The following functions pop one elements from the stack, it set a date using it as an epoch timein seconds and return the corresponding calendar field.
  * YEAR
  * MONTH, in the range [1,12]
  * DATE: the day of month
  * HOUR
  * MINUTE
  * SECOND
  * WEEK: it should be used carefully as the returned value  depends on the current time zone and current local

# Processing the stack directly #
  * DUP
  * POP
  * EXC

# TODO #

While most RPN functions are supported, the following are still left for use to implement.

  * ADDNAN
  * AVG
  * ISINF
  * LTIME
  * PREDICT
  * PREDICTSIGMA
  * REV
  * SORT
  * TREDNAN
  * TREND