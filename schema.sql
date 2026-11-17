-- Smart Traffic Management System - MySQL schema
-- The application creates these automatically on first connect, so running this file is optional.
-- Useful if you want to show the tables in MySQL Workbench / a viva.

CREATE DATABASE IF NOT EXISTS smart_traffic;
USE smart_traffic;

CREATE TABLE IF NOT EXISTS traffic_reading (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    ts             DATETIME NOT NULL,
    sim_time       DOUBLE,
    scenario       VARCHAR(40),
    control_mode   VARCHAR(20),
    direction      VARCHAR(10),
    waiting        INT,
    density        DOUBLE,
    level          VARCHAR(12),
    avg_speed_kmh  DOUBLE
);

CREATE TABLE IF NOT EXISTS traffic_event (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    ts           DATETIME NOT NULL,
    sim_time     DOUBLE,
    event_type   VARCHAR(20),
    description  VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS run_summary (
    id                       BIGINT AUTO_INCREMENT PRIMARY KEY,
    ts                       DATETIME NOT NULL,
    scenario                 VARCHAR(40),
    control_mode             VARCHAR(20),
    duration_sec             DOUBLE,
    passed                   INT,
    avg_delay_sec            DOUBLE,
    max_queue                INT,
    fuel_litres              DOUBLE,
    co2_kg                   DOUBLE,
    avg_ambulance_delay_sec  DOUBLE
);

-- Handy queries for the demo
-- SELECT direction, ROUND(AVG(waiting),1) AS avg_waiting FROM traffic_reading GROUP BY direction;
-- SELECT control_mode, avg_delay_sec FROM run_summary ORDER BY id DESC LIMIT 3;
