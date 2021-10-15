DROP DATABASE IF EXISTS configuration;

CREATE DATABASE configuration;

GRANT CONNECT, TEMPORARY ON DATABASE configuration TO configurator;
GRANT ALL ON DATABASE configuration TO configurator;
--GRANT ALL ON DATABASE gamificationdb TO gameuser;
