CREATE USER "${dbUsername}" WITH PASSWORD '${dbPassword}';
CREATE SCHEMA IF NOT EXISTS sample
    AUTHORIZATION "${dbUsername}";

GRANT
    CONNECT,
    TEMPORARY
ON DATABASE "my_sample_db"
TO "${dbUsername}";
