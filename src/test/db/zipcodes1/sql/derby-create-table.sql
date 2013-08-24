-- From http://federalgovernmentzipcodes.us/
-- Adapted to Derby syntax
CREATE TABLE zip_code (
  zip_code varchar(5) NOT NULL,
  lat float NOT NULL,
  lon float NOT NULL,
  city varchar(100) NOT NULL,
  state_prefix varchar(100) NOT NULL,
  county varchar(100) NOT NULL,
  z_type varchar(100) NOT NULL,
  xaxis float NOT NULL,
  yaxis float NOT NULL,
  zaxis float NOT NULL,
  z_primary varchar(100) NOT NULL,
  worldregion varchar(100) NOT NULL,
  country varchar(100) NOT NULL,
  locationtext varchar(255) NOT NULL,
  location varchar(255) NOT NULL,
  population varchar(255) NOT NULL,
  housingunits integer NOT NULL,
  income integer NOT NULL,
  landarea varchar(255) NOT NULL,
  waterarea varchar(255) NOT NULL,
  decommisioned varchar(100) NOT NULL,
  militaryrestrictioncodes varchar(255) NOT NULL,
  decommisionedplace varchar(255) NOT NULL
);
