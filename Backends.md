# Introduction #

RRD4J uses backend objects to store actual bytes for RRD database.

# Details #

Each Round Robin Database object (`RrdDb` object) is backed with a single `RrdBackend` object which performs actual I/O operations on the underlying storage. The `RrdBackend` object is itself not created directly but through factories, that are object instanciated from `RrdBackendFactory` derivated classes.

RRD backend are registered and managed through the static methods `RrdBackendFactory.registerFactory(RrdBackendFactory factory)` and `RrdBackendFactory.registerAndSetAsDefaultFactory(RrdBackendFactory factory)`.

When a RRD database is created without the factory argument or through the `RrrDbPool`, it use the default factory specified with `RrdBackendFactory.registerAndSetAsDefaultFactory(RrdBackendFactory factory)` or `RrdBackendFactory.setDefaultFactory(java.lang.String factoryName)`. For more details, see the [javadoc](http://rrd4j.googlecode.com/svn/trunk/javadoc/reference/org/rrd4j/core/RrdBackendFactory.html).

Factory classes are used to create concrete `RrdBackend` implementations. Each factory creates unlimited number of specific backend objects. Rrd4j supports six different backend types (backend factories) out of the box:

  * **`FILE`**: objects of this class are created from the `RrdFileBackendFactory` class. This was the default backend used in all Rrd4j releases before 1.4.0 release. It uses `java.io.*` package and `RandomAccessFile` class to store RRD data in files on the disk.
  * **`SAFE`**: objects of this class are created from the `RrdSafeFileBackendFactory` class. It uses `java.io.*` package and `RandomAccessFile` class to store RRD data in files on the disk. This backend is _safe_: it locks the underlying RRD file during update/fetch operations, and caches only static parts of a RRD file in memory. Therefore, this backend is safe to be used when RRD files should be shared between several JVMs at the same time.
  * **`NIO`**: objects of this class are created from the `RrdNioBackendFactory` class. The backend uses `java.io.*` and `java.nio.*` classes (mapped `ByteBuffer`) to store RRD data in files on the disk. This is the default backend since 1.4.0 release. It can be quite heavy on memory, because of the memory mapping of files.
  * **`MEMORY`**: objects of this class are created from the `RrdMemoryBackendFactory` class. This backend stores all data in memory. Once JVM exits, all data gets lost. The backend is extremely fast and memory hungry.
  * **`BERKELEY`**: objects of this class are created from the `RrdBerkeleyDbBackendFactory` class. It stores RRD data to ordinary disk files using [Oracle Berkeley DB](http://www.oracle.com/technetwork/database/berkeleydb/overview/index-093405.html) Java Edition.
  * **`MONGODB`**: objects of this class are created from the `RrdMongoDBBackendFactory` class. It stores data in a `DBCollection` from [MongoDB](http://www.mongodb.org/).

Each backend factory is identified by its name. Constructors are provided in the `RrdDb` class to create `RrdDb` objects (RRD databases) backed with a specific backend.
See javadoc for `RrdBackend` to find out how to create your custom backends.