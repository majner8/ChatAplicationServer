drop database user;
drop database userquickchat;
create database userquickchat;
CREATE DATABASE IF NOT EXISTS user;
use user;
CREATE TABLE if not exists RegisterUsers (
  Email VARCHAR(250) unique,
  PhonePreflix BIGINT,
  Phone BIGINT,
  passwords VARCHAR(50),
  UUIDUser VARCHAR(20) primary key
);
create table if not exists UserInformation(
  UUIDUser VARCHAR(20) primary key,
  name varchar(50) not null,
  LastName varchar(50) not null,
  Born Date not null,
  LastActivity DateTime
  );

-- function which return UUID for each device
DELIMITER //
CREATE FUNCTION GenerateUUID() RETURNS CHAR(5) DETERMINISTIC
BEGIN
  DECLARE Chars VARCHAR(5);
  DECLARE random_char CHAR(1);
  DECLARE loops INT;

  SET Chars = '';
  SET loops = 0;

  WHILE loops < 5 DO
    SET random_char = CHAR(65 + FLOOR(26 * RAND()));

    IF RAND() < 0.5 THEN
      SET Chars = CONCAT(Chars, random_char);
    ELSE
      SET Chars = CONCAT(Chars, CHAR(ASCII(random_char) + 32));
    END IF;

    SET loops = loops + 1;
  END WHILE;

  RETURN Chars;
END //
DELIMITER ;
drop function GenerateUUID;
  
drop database chat;
create database if not exists Chat;

