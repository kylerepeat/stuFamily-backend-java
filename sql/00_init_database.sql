-- Run this script on the postgres maintenance database:
-- psql -h <host> -p <port> -U <user> -d postgres -f sql/00_init_database.sql

SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;

-- Create database with UTF8 charset.
-- If database already exists, skip manually.
CREATE DATABASE stufamily
    WITH ENCODING = 'UTF8'
         TEMPLATE = template0;
