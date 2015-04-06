<table cellpadding='0' border='0' cellspacing='0'>
<tr>
<td valign='top'>
RRD4J is a high performance data logging and graphing system for time series data, implementing <a href='http://oss.oetiker.ch/rrdtool/'>RRDTool's</a> functionality in Java. It follows much of the same logic and uses the same data sources, archive types and definitions as RRDTool does. Open Source under Apache 2.0 License.<br>
<br>
RRD4J supports all standard operations on Round Robin Database (RRD) files: CREATE, UPDATE, FETCH, LAST, DUMP, EXPORT and GRAPH. RRD4J's API is made for those who are familiar with <a href='http://oss.oetiker.ch/rrdtool/'>RRDTool's</a> concepts and logic, but prefer to work with pure Java (no native functions or libraries, no Runtime.exec(), RRDTool does not have to be present).<br>
<br>
<h3>Latest Version (requires Java 6+)</h3>
RRD4J 2.2 (released 2013-04-11) - <a href='http://code.google.com/p/rrd4j/downloads/list'>Download</a> - <a href='http://rrd4j.googlecode.com/git-history/2.2/changelog.txt'>Changelog</a> - <a href='http://rrd4j.googlecode.com/git-history/2.2/javadoc/reference/packages.html'>Javadoc</a>


<h3>Development</h3>
RRD4J can be build using gradle, maven or ant, but maven is the main build tool, the one that is used to generate continuous integration build.<br>
<br>
The Git repository can also be accessed at <a href='https://github.com/fbacchella/RRD4J'>https://github.com/fbacchella/RRD4J</a>. But for now, the reference site will stay on google, GitHub will be used for pull request.<br>
<br>
The maven generated site is <a href='http://rrd4j.org/site'>http://rrd4j.org/site</a>.<br>
<br>
Automated build are uploaded at sonatype's <a href='https://oss.sonatype.org/content/repositories/snapshots/org/rrd4j/rrd4j'>repository</a>.<br>
<br>
RRD4J is missing unit test, so please don't hesitate to look at the <a href='http://rrd4j.org/site/jacoco'>jacoco report</a> for missing code coverage and submit new one.<br>
<br>
<a href='http://jrds.fr/jenkins/job/rrd4j/'><img src='http://jrds.fr/jenkins/job/rrd4j/badge/icon?file=.png' /></a>

<wiki:gadget url="http://www.ohloh.net/p/13231/widgets/project_partner_badge.xml" height="53" border="0"/><br>
</td>
<td>
<table cellpadding='5' border='0'>
<tr>
<td>
<img src='http://rrd4j.googlecode.com/git/demo.png' />
</td>
</tr>
</table>
</td>
</tr>
</table>


---


## Highlights ##

  * **Portable files**, RRDTool files are not
  * **Simple API**
  * **RRD4J supports the same data source types** as RRDTool (COUNTER, ABSOLUTE, DERIVE, GAUGE)
  * **RRD4J supports the same consolidation functions** as RRDTool (AVERAGE, MIN, MAX, LAST) and adds TOTAL, FIRST
  * **RRD4J** supports almost all RRDTool RPN functions (see [RPNFuncs](RPNFuncs.md))
  * RRD4J has **multiple backends**, e.g. use MongoDB as data store

## Usage Example ##
```
import org.rrd4j.code.*;
import static org.rrd4j.DsType.*;
import static org.rrd4j.ConsolFun.*;
...
// first, define the RRD
RrdDef rrdDef = new RrdDef(rrdPath, 300);
rrdDef.addArchive(AVERAGE, 0.5, 1, 600); // 1 step, 600 rows
rrdDef.addArchive(AVERAGE, 0.5, 6, 700); // 6 steps, 700 rows
rrdDef.addArchive(MAX, 0.5, 1, 600);

// then, create a RrdDb from the definition and start adding data
RrdDb rrdDb = new RrdDb(rrdDef);
Sample sample = rrdDb.createSample();
while (...) {
    sample.setTime(t);
    sample.setValue("inbytes", ...);
    sample.setValue("outbytes", ...);
    sample.update();
}
rrdDb.close();

// then create a graph definition
RrdGraphDef gDef = new RrdGraphDef();
gDef.setWidth(500);
gDef.setHeight(300);
gDef.setFilename(imgPath);
gDef.setStartTime(start);
gDef.setEndTime(end);
gDef.setTitle("My Title");
gDef.setVerticalLabel("bytes");

gDef.datasource("bytes", "myrrdpath", "inbytes", AVERAGE);
gDef.hrule(2568, Color.GREEN, "hrule");
gDef.setImageFormat("png");

// then actually draw the graph
RrdGraph graph = new RrdGraph(gDef); // will create the graph in the path specified
```

Go through the source of [Demo](http://rrd4j.googlecode.com/svn/trunk/src/main/java/org/rrd4j/demo/Demo.java) for more examples.
## Supported Databases ##
Next to memory and file storage, RRD4J supports the following databases (using byte array storage):

  * [MongoDB](http://www.mongodb.org/) - a scalable, high-performance, open source, document-oriented database. See the [RrdMongoDBBackendFactory](http://code.google.com/p/rrd4j/source/browse/trunk/src/main/java/org/rrd4j/core/RrdMongoDBBackendFactory.java) class.
  * [Oracle Berkeley DB](http://www.oracle.com/technetwork/database/berkeleydb/overview/index-093405.html) Java Edition -  an open source, embeddable database providing developers with fast, reliable, local persistence with zero administration. See the [RrdBerkeleyDbBackendFactory](http://code.google.com/p/rrd4j/source/browse/trunk/src/main/java/org/rrd4j/core/RrdBerkeleyDbBackendFactory.java) class.

## Clojure ##

Thanks to the [rrd4clj](https://github.com/maoe/rrd4clj) project Clojure now has a RRD API (using RRD4J). Check out their [examples](https://github.com/maoe/rrd4clj/blob/master/src/clj/rrd4clj/examples.clj).

## Contributing to RRD4J ##

If you are interested in contributing to RRD4J, start by posting patches to issues that are important to you. Subscribe to the discussion group and introduce yourself.

If you can't contribute, please let us know about your RRD4J use case. Always good to hear your stories!

## Graph Examples (from the [JRDS](http://jrds.fr/) project) ##

![http://jrds.fr/_media/myssqlops.png](http://jrds.fr/_media/myssqlops.png)

![http://jrds.fr/_media/screenshots/meminforam.png](http://jrds.fr/_media/screenshots/meminforam.png)