# Introduction 

This DB2 enhancement contains an SQL file parser which is able to handle SQL files that normally work when run directly in DB2:

* Properly handle DB2's `--#SET TERMINATOR ` tag which can switch the line terminator in the middle of a script.
* Issues a `commit` if there is a commit missing before `TRUNCATE TABLE` to avoid the error 'The SQL statement is only allowed as the first statement in a unit of work. SQLCODE=-428, SQLSTATE=25001, ...'
* Re-writes `REORG TABLE` commands to an `ADMIN_CMD` so that a JDBC driver will properly execute it

# Usage

1. Add the dependency to liquibase:
```
      <plugin>
        <groupId>org.liquibase</groupId>
        <artifactId>liquibase-maven-plugin</artifactId>
        <dependencies>
          ...
          <dependency>
            <groupId>io.github.pdjohe</groupId>
            <artifactId>liquibase-db2-enhanced</artifactId>
            <version>1.0</version>
          </dependency>
          ...
        </dependencies>
      </plugin>
```

2. Include the XSD to your change log and use, for example:

```
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xmlns:db2="http://www.liquibase.org/xml/ns/dbchangelog-ext/db2-enhanced"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.6.xsd
                            http://www.liquibase.org/xml/ns/dbchangelog-ext/db2-enhanced http://www.liquibase.org/xml/ns/dbchangelog-ext/db2-enhanced-1.0.xsd">

    <changeSet id="test-01" author="test">
        <db2:db2SqlFile path="./test1.sql"/>
        <rollback>
            <db2:db2SqlFile path="./test1-rollback.sql"/>
        </rollback>
    </changeSet>


</databaseChangeLog>
```

# Building the plugin yourself

The plugin can be built with maven:

`mvn clean install`

In order to run all the tests, run the following to start DB2 locally using docker (and then execute the tests normally):

`docker-compose up db2`