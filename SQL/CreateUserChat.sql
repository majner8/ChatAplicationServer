WChat
BBB
create table if not exists chatXXX(
userUUID varchar(20), message varchar(300),  TimeOfMessage datetime default now(),
numberOFmessage int auto_increment primary key,
MessageUUID varchar(15) unique
);
CCC

WChat
BBB
create table if not exists AdministrationXXX(
userUUID varchar(20) unique, chatName varchar(15) default null
);
CCC

WChat
BBB

CREATE TRIGGER  if not exists TriggerXXX
BEFORE INSERT ON AdministrationXXX 
FOR EACH ROW 
BEGIN 
DECLARE userName VARCHAR(255);
    DECLARE userLastName VARCHAR(255);
    
    SELECT name, LastName INTO userName, userLastName
    FROM user.userinformation
    WHERE UUIDUser = NEW.userUUID;
    
    SET NEW.chatName = CONCAT(userName, ' ', userLastName);
END;
CCC

IChat
BBB
insert ignore into AdministrationXXX(userUUID)
values
(ZZZ); 

CCC

SChat
BBB
SELECT *  FROM AdministrationXXX;
