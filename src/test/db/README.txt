This directory contains third-party, publicly available relational databases 
whose content is used to automate logic2j testing:
- functional and non-regression testing of the default inference and unification engines
- benchmarking and performance assessment of the relational database clause providers
- stress-loading and multi-threading of the engine

We use the Derby database in embedded mode. The Maven "test" target takes care of unzipping
the voluminous binary database and the derby.jar driver.

For every database,
- the "sql" directory contains credits, licensing information, and possibly scripts adapted to Derby,
  or, when not possible, instructions on how to load these data into Derby.
  
- the derby-vNNNN directory contains a binary distribution of the database, ready to use by the test cases.
