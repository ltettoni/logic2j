A free database of US ZIP codes from http://federalgovernmentzipcodes.us/

Version (here named "zipcodes1") as of 3/9/2011. 
One single table, 80K records, 23 columns.

Courtesy of http://federalgovernmentzipcodes.us/ webmaster, with thanks.


----------------------------------------------------------------------------
-- EVERYTHING INSIDE AND UNDER THIS DIRECTORY IS FREE AND WITHOUT LICENSE --
--          IT IS NOT SUBJECT TO THE VIRAL NATURE OF LGPLv2.1             --
----------------------------------------------------------------------------


The MySQL scripts downloaded from http://federalgovernmentzipcodes.us/zip_code_full.zip needed some adapting to Derby.


The scripts included here are:
(1) sql/derby-create-tables.sql   : 
       A version of the CREATE TABLE DDL in Derby's dialect : different column type spec, removed index, avoid column names quotes
(2) sql/derby-insertObject.zip          :
       Derby does not accept quoted column names - so same inserts but without quotes in the column specs. Varchar values remain quoted of course.
(3) sql/derby-after-load.sql      :
       Refactored pure numeric columns to double or integer: landarea, waterarea, and population. Will allow aggregate functions.
