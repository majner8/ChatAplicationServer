Wuser
BBB
-- @@@ is special character, which is use only for createUserChat
-- because XXX is belongs to first query
update @@@
set UserTableName=(select concat(name," ", LastName) from userinformation
where UUIDUser=ZZZ)
where chatUUID=ZZZ;
