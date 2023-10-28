# Spark CEF Data Source

A custom Spark data source supporting the [Common Event Format](https://support.citrix.com/article/CTX136146) V25
standard for logging events.

[![Spark library CI](https://github.com/elastacloud/spark-cef-reader/actions/workflows/main.yml/badge.svg)](https://github.com/elastacloud/spark-cef-reader/actions/workflows/main.yml)

## Fork

This is a fork taken from the original source at [https://github.com/bp/spark-cef-reader](https://github.com/bp/spark-cef-reader)
which was created by the same authors as this fork.

This fork has the following changes applied to it at the time of the fork. Subsequence changes can be viewed
in the history of the source code and in the release notes.

* Updated to include support for Spark 3.4 and 3.5
* Rewrite of the options class to meet support for Spark 3.4
* Renamed the package to not violate any trademarks and to ensure that this is seen as a derivative work

This repository contains all history from the original source and has the same license applied.

## Supported Features

* Schema inference. Uses data types for known extensions.
* Plain text, bzip2, and gzip files supported and tested
* Field pivoting, for turning `<key>Label` fields into the field names
* Scanning depth, allowing you to define how many records to scan to infer the schema
* Built using Spark DataSource v2 APIs for Spark 3
* Usage as a source in Spark SQL statements

## Usage

```scala
import org.apache.spark.sql.SparkSession

val spark = SparkSession.builder().getOrCreate()

// Read using provided data frame reader
val df = spark.read
  .option("maxRecords", "10000") // Optional, default 10,000
  .option("pivotFields", "true") // Optional, default is false
  .cef("/path/to/file.log")

// Writing the data back out
df.write
  .mode("overwrite")
  .option("nullValue", "NA") // Optional
  .option("dateFormat", "millis") // Optional
  .cef("/path/to/output/file.log")

// Or using the format method (required for use in PySpark)

// Using the short format name
val dfShort = spark.read.format("cef").load("/path/to/file.log")

// Using the fully qualified name
val dfFull = spark.read.format("com.elastacloud.spark.cef").load("/path/to/file.log")

// The path to the file may be an absolute path name, multiple path names, or a glob pattern.
val dfGlob = spark.read.cef("/landing/events/year=2020/month=*/day=*/*.log.gz")
```

Available for use in Spark SQL as well

```sql
-- Note the use of backticks around the path

SELECT
    *
FROM
    cef.`/path/to/file.log`
```

## Options

The following options are available to pass to the data source, where they are not defined then the default value
will be used.

| Option                  | Type      | Default                        | Supported Actions | Purpose                                                                                                                                                                                                                                                                                                                                                                                                                                                                            |
|-------------------------|-----------|--------------------------------|-------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| maxRecords              | Integer   | 10,000                         | Read              | The number of records to scan when inferring the schema. The data source will keep scanning until either the maximum number of records have been reached or there are no more files to scan.                                                                                                                                                                                                                                                                                       |
| pivotFields             | Boolean   | false                          | Read              | Scans for field pairs in the format of `key=value keyLabel=OtherKey` and pivots the data to `OtherKey=value`.                                                                                                                                                                                                                                                                                                                                                                      |
| defensiveMode           | Boolean   | false                          | Read              | Used if a feed is known to violate the CEF spec. Adds overhead to the parsing so only use when there are known violations.                                                                                                                                                                                                                                                                                                                                                         |
| nullValue               | String    | `-`                            | Read/Write        | A value used in the CEF records which should be parsed as a `null` value.                                                                                                                                                                                                                                                                                                                                                                                                          |
| mode                    | ParseMode | Permissive                     | Read              | Permitted values are `permissive`, `dropmalformed` and `failfast`. When used in `FailFast` mode the parser will throw an error on the first record exception found. When used in `Permissive` mode it will attempt to parse as much of the record as possible, with `null` values used for all other values. Using `dropmalformed` will simply drop any malformed records from the result. `Permissive` mode may be used in combination with the `corruptRecordColumnName` option. |
| corruptRecordColumnName | String    | `null`                         | Read              | When used with `Permissive` mode the full record is stored in a column with the name provided. If null is provided then the full record is discarded. By providing a name the data source will append a column to the inferred schema.                                                                                                                                                                                                                                             |
| dateFormat              | String    | `MMM dd yyyy HH:mm:ss.SSS zzz` | Write             | When writing data this option defines the format time use for timestamp values. The data source will check against CEF valid formats. Alternatively use `millis` to output using milliseconds from the epoch                                                                                                                                                                                                                                                                       |

### CEF supported date formats

The following defines the date formats supported by the CEF standards. Also supported are epoch milliseconds which can 
be requested using the `millis` format for `dateFormat` when writing. When reading the data source will also evaluate
ISO 8601 formats for where non-standard date formats may have been used.

    MMM dd yyyy HH:mm:ss
    MMM dd yyyy HH:mm:ss.SSS zzz
    MMM dd yyyy HH:mm:ss.SSS
    MMM dd yyyy HH:mm:ss zzz
    MMM dd HH:mm:ss
    MMM dd HH:mm:ss.SSS zzz
    MMM dd HH:mm:ss.SSS
    MMM dd HH:mm:ss zzz

When reading, if the timezone identifier is omitted, then UTC will be assumed. If the year is omitted then it will
default to 1970.
