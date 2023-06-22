Wuser
BBB
create table  XXX(IpAdressDevice varchar(50) primary key, DeviceUUID varchar(5) unique);
CCC



WUserQuickChat
BBB
-- ChatEnd if null user is not kick
create table XXX(chatUUID varchar(45) primary key,ChatEnd DateTime default null,UserTableName varchar(20));
CCC
Suser
BBB
-- first query if this statement return null then have to run second query
select DeviceUUID from XXX where IpAdressDevice=ZZZ;

 



