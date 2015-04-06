RRD4J supports all standard operations on Round Robin Database (RRD) files: CREATE, UPDATE, FETCH, LAST, DUMP, XPORT and GRAPH. RRD4J's API is made for those who are familiar with RRDTool's concepts and logic, but prefer to work with pure java.
If you provide the same data to RRDTool and RRD4J, you will get exactly the same results and graphs. RRD4J is made from the scratch and it uses very limited portions of RRDTool's original source code.

RRD4J does not use native functions and libraries, has no Runtime.exec() calls and does not require RRDTool to be present. RRD4J is distributed as a software library (jar files) and comes with full java source code (Apache licence).

You will not understand a single thing here if you are not already familiar with RRDTool. Basic concepts and terms (such as: datasource, archive, datasource type, consolidation functions, archive steps/rows, heartbeat, RRD step, RPN, graph DEFs and CDEFs) are not explained here because they have exactly the same meaning in RRD4J and RRDTool. If you are a novice RRDTool/RRD4J user, this annotated RRDTool tutorial is a good place to start.

## Why RRD4J? ##

Nothing can be compared with RRDTool. Period. Tobi Oetiker did such an excellent work that any attempt to write something similar to RRDTool will probably look pathetic.

Several years ago I used RRDTool for Internet traffic monitoring in a complex, commercial project. Front-end application (data sampling and validation) was written in Java, but RRDTool did all the hard work. No doubt that RRDTool is a wonderful and very useful tool especially for those inclined to pure C/C++ or scripting languages like Perl or PHP, but I am a Java addict since I discovered this superior programming language. Java does not prohibit usage of external tools, scripts and native libraries but such practice breaks the basic Java motto ("code once, run everywhere") and should be avoided whenever possible. Having finished some swift researches I was astonished that nobody tried to implement the same concept in pure Java. Even OpenNMS, excellent Java application for network monitoring used its own JNI library as a wrapper around RRDTool. And something that uses JNI will never become something that is easily portable - we are talking about different world here.

RRDTool raises some issues if you try to use it with Java. RRDTool is written in good old C, and at the present moment there is no complete, official, and bullet-proof Java interface to RRDTool functions. You have only several options:
  * To spawn RRDTool commands as external processes, through Runtime.exec() calls. I don't like it, because - it's not pure Java, and it's slow. Your application will have to carry the source code of RRDTool everywhere around and it has to be recompiled for different platforms. I used this approach in several of my applications and managed to crash Sun's JVM from time to time, usually under heavy loads ("an error has happened outside of JVM", that's all you'll get from dying JVM)

  * To use some native Java library as a wrapper for RRDTool functions. This approach is much faster than ordinary Runtime.exec() call.You could try my RRDJTool library or the library bundled with OpenNMS, but it's still not pure Java. And you will have a growing headache whenever your native library has to be moved from one platform to another.

To make things even worse, Runtime.exec() is probably the weakest and the most complicated part of the entire J2SE. It just looks simple, but to use it properly you'll have to read javadoc very carefully. It's surprisingly easy to write java code with Runtime.exec() which works well on Windows or Solaris, but crashes or blocks JVM on Linux. There are several excellent articles on the Web to help you with this issue, and this one is probably mandatory. But, sooner or later you'll end with a conclusion that support for external processes in Java is unnecessarily complicated and somewhat unnatural.

I choose deliberately to look pathetic, but I could not resist to create a complete, pure Java replacement for RRDTool. So, RRD4J is here. I'll try to make my point here:

  * RRD4J is a free software library (API) for RRD files management and graphing. RRD4J is not a set of command line utilities like RRDTool.

  * RRD4J guarantees the following: If you perform the same sequence of RRD create, update and fetch operations using RRDTool and RRD4J API, you will get exactly the same results. Without this feature, RRD4J would be pointless. However, we deliberately introduced some minor differences in data processing between RRD4J and RRDTool. We believe that in some rare special cases RRD4J should have more processing power than RRDTool.

  * RRD4J supports exactly the same data source types (COUNTER, ABSOLUTE, DERIVE, GAUGE) and consolidation functions (AVERAGE, MIN, MAX, LAST) as RRDTool does.

  * RRD4J API is written to be fully compatible with the syntax and logic of key RRDTool commands (update, fetch, graph). If you are familiar with RRDTool and Java, you will have no problem to use RRD4J API to manipulate RRD files.

  * RRD4J is made from the scratch. RRD4J is not a port of RRDTool's C source code to Java. In fact, RRDTool source code is used in RRD4J in very small doses (for example, for rollover detection with COUNTER data types, but even Tobi borrowed that part of the code from someone else ;)

  * RRD4J files have fixed sizes, as RRDTool files. However, RRD4J uses its own binary file format: you cannot use RRD4J API to manage RRDTool files and vice versa.

  * RRD4J RRD files are portable, RRDTool files are not. Try to copy a RRDTool file from Linux to Windows platform and fetch data from it. It does not work! But with RRD4J you are free to create your RRD files on Solaris and transfer them to Windows or Linux platform for further processing. It works! That is why I had to define my own file format which is different from the format used in RRDTool - there is no point in creating portable Java application backed by non-portable data files.

  * RRD4J uses the same XML format for RRD dump as RRDTool. You can dump your RRD4J file to an XML file which can be imported by RRDTool. And vice versa.

  * RRDTool is such a great tool because of its 'scripting' capabilities: graph and database definitions could be easily isolated from the source code. When you want to change the look of your RRDTool graphs, you don't have to recompile the whole source. At the present moment it is possible to create RRD4J RRD files and graphs starting from external XML template files. You change the template, not the source code when you want to change creation parameters of your RRD files and graphs.

  * RRD4J is a standalone Java library which supports DEF, CDEF, GPRINT, COMMENT and other important graph directives found in RRDTool's graph command (even RPN extensions). Thanks to Arne, RRD4J graphs now have almost the same look&feel like RRDTool graphs. To be honest, RRDTool is still a little more flexible, but just a little :) We plan to add more functionality and power to the graph.