CREATE ROLE configurator WITH ENCRYPTED PASSWORD 'configurator';
ALTER ROLE configurator WITH LOGIN;

DROP DATABASE IF EXISTS configuration;

CREATE DATABASE configuration OWNER configurator;

GRANT CONNECT, TEMPORARY ON DATABASE configuration TO configurator;
GRANT ALL ON DATABASE configuration TO configurator;
--GRANT ALL ON DATABASE gamificationdb TO gameuser;
