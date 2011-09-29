This directory contains third-party, publicly available relational database(s) 
whose content is used to automate the testing of logic2j:
- functional and non-regression testing of the default inference and unification engines
- benchmarking and performance assessment of the relational databases clause providers
- stress-loading and multi-threading of the engine on real data

We use the Derby database engine in embedded mode and provide binary images
of those public databases (so you won't have to load heaps of SQL), whenever possible.

The Maven "test" target takes care of unzipping the voluminous binary images.

The directory structure is:

- "db/DATABASE"           contains description, access information, credits, licensing information, of one database.
- "db/DATABASE/sql"       contains DDL and DML scripts adapted to Derby or, when not possible, 
                          download site and instructions to adapt to Derby. 
  
- "db/DATABASE/derby-vN.zip"   A compressed image of the derby database
- "db/DATABASE/derby-vN/**"    An Maven auto-expanded image of the database, ready to connect to
