alter table zip_code add column land double;

alter table zip_code add column water double;

alter table zip_code add column pop integer;

update zip_code set land = double(landarea) where length(landarea)>1;

update zip_code set water = double(waterarea) where length(waterarea)>1;

update zip_code set pop = integer(population) where length(population)>1;

alter table zip_code drop column landarea;

alter table zip_code drop column waterarea;

alter table zip_code drop column population;

rename column zip_code.land to landarea;

rename column zip_code.water to waterarea;

rename column zip_code.pop to population;

-- Predicate view used in test cases
create view pred_zip_code as
select zip_code as arg_0, city as arg_1 FROM zip_code;

-- Indexes
create index zip_ix1 on zip_code(zip_code);

create index zip_ix2 on zip_code(city);

-- Reclaim empty space
call syscs_util.syscs_compress_table('app', 'zip_code', 0);
