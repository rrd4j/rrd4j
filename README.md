rrd4j <a href='http://jrds.fr/jenkins/job/rrd4j/'><img src='http://jrds.fr/jenkins/job/rrd4j/badge/icon?file=.png' /></a>
=====

RRD4J is a high performance data logging and graphing system for time series data, implementing [RRDTool's](http://oss.oetiker.ch/rrdtool/)
functionality in Java. It follows much of the same logic and uses the same data sources, archive types and definitions as RRDTool does.

RRD4J supports all standard operations on Round Robin Database (RRD) files: `CREATE`, `UPDATE`, `FETCH`, `LAST`, `DUMP`, `EXPORT` and `GRAPH`.
RRD4J's API is made for those who are familiar with [RRDTool's](http://oss.oetiker.ch/rrdtool/) concepts and logic, but prefer to
work with pure Java (no native functions or libraries, no Runtime.exec(), RRDTool does not have to be present). We help out our
users [here](https://groups.google.com/forum/#!forum/rrd4j-discuss).

### Latest Version (requires Java 7+)

RRD4J 3.1 (released 2017-01-01) - [Download](https://github.com/rrd4j/rrd4j/releases) - [Changelog](https://raw.githubusercontent.com/rrd4j/rrd4j/master/changelog.txt)

### Building (optional)

RRD4J is built using Maven. The generated site is available [here](http://rrd4j.org/site). Automated builds are uploaded
to [Sonatype's repository](https://oss.sonatype.org/content/repositories/snapshots/org/rrd4j/rrd4j).

### Using with Maven

Add this dependency to your project's POM file:

```xml
<dependency>
	<groupId>org.rrd4j</groupId>
	<artifactId>rrd4j</artifactId>
	<version>3.1</version>
</dependency>
```

### Why RRD4J?

  * Portable files, RRDTool files are not
  * Simple API
  * Supports the same data source types as RRDTool (`COUNTER`, `ABSOLUTE`, `DERIVE`, `GAUGE`)
  * Supports the same consolidation functions as RRDTool (`AVERAGE`, `MIN`, `MAX`, `LAST`) and adds `TOTAL`, `FIRST`
  * Supports almost all RRDTool RPN functions (wiki/see [RPNFuncs](RPNFuncs))
  * Multiple backends, e.g. use MongoDB as data store

### Usage Example

```java
import org.rrd4j.core.*;
import static org.rrd4j.DsType.*;
import static org.rrd4j.ConsolFun.*;

String rrdPath = "my.rrd";

// first, define the RRD
RrdDef rrdDef = new RrdDef(rrdPath, 300);
rrdDef.addArchive(AVERAGE, 0.5, 1, 600); // 1 step, 600 rows
rrdDef.addArchive(AVERAGE, 0.5, 6, 700); // 6 steps, 700 rows
rrdDef.addArchive(MAX, 0.5, 1, 600);

// then, create a RrdDb from the definition and start adding data
RrdDb rrdDb = new RrdDb(rrdDef);
Sample sample = rrdDb.createSample();
while (...) {
    double inbytes = ...
    double outbytes = ...
    sample.setValue("inbytes", inbytes);
    sample.setValue("outbytes", outbytes);
    sample.update();
}
rrdDb.close();

// then create a graph definition
RrdGraphDef gDef = new RrdGraphDef();
gDef.setWidth(500);
gDef.setHeight(300);
gDef.setFilename("inbytes.png");
gDef.setTitle("My Title");
gDef.setVerticalLabel("bytes");
gDef.datasource("inbytes-average", rrdPath, "inbytes", AVERAGE);
gDef.line("inbytes-average", Color.BLUE, "Bytes In")
gDef.hrule(2568, Color.GREEN, "hrule");
gDef.setImageFormat("png");

// then actually draw the graph
RrdGraph graph = new RrdGraph(gDef); // will create the graph in the path specified
```

Go through the source of [Demo](https://github.com/rrd4j/rrd4j/blob/master/src/main/java/org/rrd4j/demo/Demo.java) for more examples.

### Supported Backends

Next to memory and file storage, RRD4J supports the following backends (using byte array storage):

  * [MongoDB](http://www.mongodb.org/) - a scalable, high-performance, open source, document-oriented database.
  * [Oracle Berkeley DB](http://www.oracle.com/technetwork/database/berkeleydb/overview/index-093405.html) Java Edition - an open source, embeddable database providing developers with fast, reliable, local persistence with zero administration.

### Clojure

Thanks to the [rrd4clj](https://github.com/maoe/rrd4clj) project Clojure now has a RRD API (using RRD4J). Check out their [examples](https://github.com/maoe/rrd4clj/blob/master/src/clj/rrd4clj/examples.clj).

### Contributing

If you are interested in contributing to RRD4J, start by posting pull requests to issues that are important to you. Subscribe to the [discussion
group](https://groups.google.com/forum/#!forum/rrd4j-discuss) and introduce yourself.

If you can't contribute, please let us know about your RRD4J use case. Always good to hear your stories!

### Graph Examples (from the [JRDS](http://jrds.fr/) project)

![http://jrds.fr/_media/myssqlops.png](http://jrds.fr/_media/myssqlops.png)

![http://jrds.fr/_media/screenshots/meminforam.png](http://jrds.fr/_media/screenshots/meminforam.png)

### License

Licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
