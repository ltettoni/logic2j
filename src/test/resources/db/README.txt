This directory contains third-party, publicly available relational database(s) 
whose content is used to automate the testing of logic2j:
- functional and non-regression testing of the default inference and unification engines
- benchmarking and performance assessment of the relational databases clause providers
- stress-loading and multi-threading of the engine on real data

We use a H2 database engine in in-memory mode and provide a loading SQL script with DDL and DML.

The Maven "test" target takes care of unzipping the voluminous binary images.

The directory structure is, for a given DATABASE:

- "src/test/resources/db/DATABASE/README.txt"     Description, access information, credits, licensing information, of one database.
- "src/test/resources/db/DATABASE/sql"            SQL DDL and DML scripts adapted to H2.
