# File Layout #

The version 1 rrd file format uses a simple format. Each RRA is an array, using the sample time as an indices. But that's not really efficient for disk IO. An update will generate one disk IO for each RRA.
With the new version, all sample within the same archive for a given sample time is stored in the same row of a matrix. With this method each update generate a single IO. Reads generate more IO because the whole archive needs to the read within the needed range, even the useless datasources. But that's not a big problems because there is much more write than read. And also reads stays sequential so the caching mechanism from the file system helps  a lot.

# File backend #

There is two kind of file backend in rrd4j: NIO and FILE. NIO uses mapped IO, FILE uses random access files. Don't assume that one is better is inferior or superior that the other, because many parameters can have dramatics influence. For example, Linux uses big IO (about 128kB) with mmaped files. The RAM will be filled with useless data and it can destroy performances. So one should tests both before making a choice.