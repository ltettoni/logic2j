initialize :- rdb_config("org.apache.derby.jdbc.AutoloadedDriver", "jdbc:derby:src/test/db/zipcodes1/derby-v10.8.2.1", "APP", "APP", "", ["pred_zip_code"]).

% This would define a predicate and its arguments corresponding to a table (or view) and (some of) its columns
binding(zip_code(_, _), 'TBL_NAME'('COLUMN1_NAME', 'COLUMN2_NAME)).
