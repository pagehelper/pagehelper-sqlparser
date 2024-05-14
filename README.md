# 兼容 JSqlParser 多个版本的扩展实现

为了兼容 jsqlparser 4.5 和 4.7（支持4.8,4.9,5.0），以及后续可能存在的其他版本，新建了一个 pagehelper-sqlparser 项目，目前提供了
4.5 和 4.7
两个实现，
使用时从 pagehelper 排除 jsqlparser，然后选择一个 jsqlparser 实现即可，当前版本默认使用的 4.7 版本的代码，
因此如果想换 4.5 的实现，可以按照下面方式进行配置：

```xml
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper</artifactId>
    <version>6.1.0</version>
    <exclusions>
        <exclusion>
            <groupId>com.github.jsqlparser</groupId>
            <artifactId>jsqlparser</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>sqlparser4.5</artifactId>
    <version>6.1.0</version>
</dependency>
```

SPI 替换默认值的优先级低于 `sqlServerSqlParser`,`orderBySqlParser`,`countSqlParser` 参数指定的实现，不指定时如果存在SPI实现，即可生效，
SPI 可以参考 pagehelper-sqlsource 模块代码。

JSqlParser 默认解析 SQL 会使用临时创建的 `Executors.newSingleThreadExecutor()`，这里通过 API 跳过了线程池：

```java
CCJSqlParser parser = CCJSqlParserUtil.newParser(sql);
parser.

withSquareBracketQuotation(true);
```

JSqlParser 使用线程池的目的是为了防止解析超时，因此如果你遇到过超时的情况，可以引入下面的依赖（通过SPI覆盖了默认实现，超时时间10秒）：

```xml
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>sqlparser-timeout</artifactId>
    <version>6.1.0</version>
</dependency>
```

---------------------

To ensure compatibility with jsqlparser 4.5, 4.7, and possible future versions,
a new project called pagehelper-sqlparser has been created.
Currently, it provides two implementations: 4.5 and 4.7.
To use it, exclude jsqlparser from pagehelper and select one jsqlparser implementation.
The current version defaults to using the code from version 4.7.
If you want to switch to the 4.5 implementation, follow the configuration steps below:

```xml

<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper</artifactId>
    <version>6.1.0</version>
    <exclusions>
        <exclusion>
            <groupId>com.github.jsqlparser</groupId>
            <artifactId>jsqlparser</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
<groupId>com.github.pagehelper</groupId>
<artifactId>sqlparser4.5</artifactId>
<version>6.1.0</version>
</dependency>
```

The priority of replacing default values with SPI is lower than the implementations specified by the sqlServerSqlParser,
orderBySqlParser, and countSqlParser parameters.
If no specific implementation is specified, the SPI implementation will take effect if available.
You can refer to the code in the pagehelper-sqlsource module for SPI implementation examples.

By default, JSqlParser uses a temporarily created `Executors.newSingleThreadExecutor()` for parsing SQL.
Here, the thread pool is bypassed through the API:

```java
CCJSqlParser parser = CCJSqlParserUtil.newParser(sql);
parser.

withSquareBracketQuotation(true);
```

The purpose of using a thread pool in JSqlParser is to prevent parsing timeouts. Therefore, if you have encountered
timeout situations,
you can introduce the following dependency (which overrides the default implementation through SPI with a timeout of 10
seconds):

```xml
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>sqlparser-timeout</artifactId>
    <version>6.1.0</version>
</dependency>
```

现在，我们也提供了对 jsqlparser 4.9 版本的支持。要使用 jsqlparser 4.9 版本的实现，可以按照下面的方式进行配置：

```xml
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper</artifactId>
    <version>6.1.0</version>
    <exclusions>
        <exclusion>
            <groupId>com.github.jsqlparser</groupId>
            <artifactId>jsqlparser</artifactId>
        </exclusion>
    </exclusions>
</dependency>
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>sqlparser4.9</artifactId>
    <version>6.1.0</version>
</dependency>
```

This addition ensures that developers have the flexibility to choose the version of jsqlparser that best suits their needs, including the latest 4.9 version.
