# LevelDB JNI

## Description

LevelDB JNI gives you a Java interface to the 
[LevelDB](https://github.com/google/leveldb) C++ library
which is a fast key-value storage library written at Google 
that provides an ordered mapping from string keys to string values.. 


#  Notice this repo important

## this jni is based on leveldb 1.23 and snappy 1.8.

## how to build
    > note: The following examples work on mac, linux, other platforms are not verified yet.
    
### Prerequisites
*  gcc-c++
*  java(1.8+)，make sure the current session $JAVA_HOME is set.
*  [cmake](https://cmake.org/download/)
*  maven
*  automake
*  autoconf
*  pkg-configls(linux maybe required)


### 1. build snappy
    git clone https://github.com/halibobor/snappy.git
    cd snappy
    git checkout leveldbjni/v1.1.8
    git submodule update --init
    mkdir build
    cd build && cmake ../ && make
    make DESTDIR=/tmp install
### 2.build leveldb
    git clone https://github.com/halibobor/leveldb.git
    cd leveldb
    git checkout leveldbjni/v1.23
    git submodule update --init
    mkdir build
    export LIBRARY_PATH=/tmp/usr/local/lib OR export LIBRARY_PATH=/tmp/usr/local/lib64
    export CPLUS_INCLUDE_PATH=/tmp/usr/local/include
    cd build && cmake -DCMAKE_BUILD_TYPE=Release .. && cmake --build .
    make DESTDIR=/tmp install
### 3.build leveldbjni
    git clone https://github.com/halibobor/leveldbjni.git
    cd leveldbjni
    export SNAPPY_HOME=/tmp/usr/local
    export LEVELDB_HOME=/tmp/usr/local
    mvn clean install -P ${platform}

Replace ${platform} with one of the following platform identifiers (depending on the platform your building on):

* osx
* linux32
* linux64
* win32
* win64
* freebsd64

# Getting the JAR

Just add the following jar to your java project:
[leveldbjni-all-1.8.jar](http://repo2.maven.org/maven2/org/fusesource/leveldbjni/leveldbjni-all/1.8/leveldbjni-all-1.8.jar)

## Using as a Maven Dependency

You just need to add the following dependency to your Maven POM:

    <dependencies>
      <dependency>
        <groupId>org.fusesource.leveldbjni</groupId>
        <artifactId>leveldbjni-all</artifactId>
        <version>1.8</version>
      </dependency>
    </dependencies>

By using the `leveldbjni-all` dependency, you get the OS specific native drivers for all supported platforms.

If you want to use only one or some but not all native drivers, then directly use the OS specific dependency instead of `leveldbjni-all`. For example to use Linux 64 bit, use this dependency:

    <dependencies>
      <dependency>
        <groupId>org.fusesource.leveldbjni</groupId>
        <artifactId>leveldbjni-linux64</artifactId>
        <version>1.8</version>
      </dependency>
    </dependencies>

If you have the leveljni native driver DLL/SO library already separately installed e.g. by a package manager (see [issue 90](https://github.com/fusesource/leveldbjni/issues/90)), then you could depend on the Java "launcher" without the JAR containing the OS specific native driver like this:

      <dependency>
        <groupId>org.fusesource.leveldbjni</groupId>
        <artifactId>leveldbjni</artifactId>
        <version>1.8</version>
      </dependency>

Lastly, another project unrelated to this project separately provides a (less mature) pure Java implementation of LevelDB, see [dain/leveldb](https://github.com/dain/leveldb).  Note that both that and this project share the same Maven artefact for the Level DB API interface (org.iq80.leveldb:leveldb-api).


## API Usage:

Recommended Package imports:

    import org.iq80.leveldb.*;
    import static org.fusesource.leveldbjni.JniDBFactory.*;
    import java.io.*;

Opening and closing the database.

    Options options = new Options();
    options.createIfMissing(true);
    DB db = factory.open(new File("example"), options);
    try {
      // Use the db in here....
    } finally {
      // Make sure you close the db to shutdown the 
      // database and avoid resource leaks.
      db.close();
    }

Putting, Getting, and Deleting key/values.

    db.put(bytes("Tampa"), bytes("rocks"));
    String value = asString(db.get(bytes("Tampa")));
    db.delete(bytes("Tampa"));

Performing Batch/Bulk/Atomic Updates.

    WriteBatch batch = db.createWriteBatch();
    try {
      batch.delete(bytes("Denver"));
      batch.put(bytes("Tampa"), bytes("green"));
      batch.put(bytes("London"), bytes("red"));

      db.write(batch);
    } finally {
      // Make sure you close the batch to avoid resource leaks.
      batch.close();
    }

Iterating key/values.

    DBIterator iterator = db.iterator();
    try {
      for(iterator.seekToFirst(); iterator.hasNext(); iterator.next()) {
        String key = asString(iterator.peekNext().getKey());
        String value = asString(iterator.peekNext().getValue());
        System.out.println(key+" = "+value);
      }
    } finally {
      // Make sure you close the iterator to avoid resource leaks.
      iterator.close();
    }

Working against a Snapshot view of the Database.

    ReadOptions ro = new ReadOptions();
    ro.snapshot(db.getSnapshot());
    try {
      
      // All read operations will now use the same 
      // consistent view of the data.
      ... = db.iterator(ro);
      ... = db.get(bytes("Tampa"), ro);

    } finally {
      // Make sure you close the snapshot to avoid resource leaks.
      ro.snapshot().close();
    }

Using a custom Comparator.

    DBComparator comparator = new DBComparator(){
        public int compare(byte[] key1, byte[] key2) {
            return new String(key1).compareTo(new String(key2));
        }
        public String name() {
            return "simple";
        }
        public byte[] findShortestSeparator(byte[] start, byte[] limit) {
            return start;
        }
        public byte[] findShortSuccessor(byte[] key) {
            return key;
        }
    };
    Options options = new Options();
    options.comparator(comparator);
    DB db = factory.open(new File("example"), options);
    
Disabling Compression

    Options options = new Options();
    options.compressionType(CompressionType.NONE);
    DB db = factory.open(new File("example"), options);

Configuring the Cache
    
    Options options = new Options();
    options.cacheSize(100 * 1048576); // 100MB cache
    DB db = factory.open(new File("example"), options);

Getting approximate sizes.

    long[] sizes = db.getApproximateSizes(new Range(bytes("a"), bytes("k")), new Range(bytes("k"), bytes("z")));
    System.out.println("Size: "+sizes[0]+", "+sizes[1]);
    
Getting database status.

    String stats = db.getProperty("leveldb.stats");
    System.out.println(stats);

Getting informational log messages.

    Logger logger = new Logger() {
      public void log(String message) {
        System.out.println(message);
      }
    };
    Options options = new Options();
    options.logger(logger);
    DB db = factory.open(new File("example"), options);

Destroying a database.
    
    Options options = new Options();
    factory.destroy(new File("example"), options);

Repairing a database.
    
    Options options = new Options();
    factory.repair(new File("example"), options);

Using a memory pool to make native memory allocations more efficient:

    JniDBFactory.pushMemoryPool(1024 * 512);
    try {
        // .. work with the DB in here, 
    } finally {
        JniDBFactory.popMemoryPool();
    }
    
## Building

See also [releasing.md](releasing.md):

### Prerequisites 

* GNU compiler toolchain
* [Maven 3](http://maven.apache.org/download.html)

### Supported Platforms

The following worked for me on:

 * OS X Lion with X Code 4
 * CentOS 5.6 (32 and 64 bit)
 * Ubuntu 12.04 (32 and 64 bit)
 * apt-get install autoconf libtool

### Build Procedure

Then download the snappy, leveldb, and leveldbjni project source code:

    wget http://snappy.googlecode.com/files/snappy-1.0.5.tar.gz
    tar -zxvf snappy-1.0.5.tar.gz
    git clone git://github.com/chirino/leveldb.git
    git clone git://github.com/fusesource/leveldbjni.git
    export SNAPPY_HOME=`cd snappy-1.0.5; pwd`
    export LEVELDB_HOME=`cd leveldb; pwd`
    export LEVELDBJNI_HOME=`cd leveldbjni; pwd`

<!-- In cygwin that would be
    export SNAPPY_HOME=$(cygpath -w `cd snappy-1.0.5; pwd`)
    export LEVELDB_HOME=$(cygpath -w `cd leveldb; pwd`)
    export LEVELDBJNI_HOME=$(cygpath -w `cd leveldbjni; pwd`)
-->

Compile the snappy project.  This produces a static library.

    cd ${SNAPPY_HOME}
    ./configure --disable-shared --with-pic
    make
    
Patch and Compile the leveldb project.  This produces a static library. 
    
    cd ${LEVELDB_HOME}
    export LIBRARY_PATH=${SNAPPY_HOME}
    export C_INCLUDE_PATH=${LIBRARY_PATH}
    export CPLUS_INCLUDE_PATH=${LIBRARY_PATH}
    git apply ../leveldbjni/leveldb.patch
    make libleveldb.a

Now use maven to build the leveldbjni project. 
    
    cd ${LEVELDBJNI_HOME}
    mvn clean install -P download -P ${platform}

Replace ${platform} with one of the following platform identifiers (depending on the platform your building on):

* osx
* linux32
* linux64
* win32
* win64
* freebsd64

If your platform does not have the right auto-tools levels available
just copy the `leveldbjni-${version}-SNAPSHOT-native-src.zip` artifact
from a platform the does have the tools available then add the
following argument to your maven build:

    -Dnative-src-url=file:leveldbjni-${verision}-SNAPSHOT-native-src.zip

### Build Results

* `leveldbjni/target/leveldbjni-${version}.jar` : The java class file to the library.
* `leveldbjni/target/leveldbjni-${version}-native-src.zip` : A GNU style source project which you can use to build the native library on other systems.
* `leveldbjni-${platform}/target/leveldbjni-${platform}-${version}.jar` : A jar file containing the built native library using your currently platform.
    
