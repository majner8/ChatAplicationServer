IChat
BBB
insert into chatXXX(userUUID, message,MessageUUID)
values (ZZZ,ZZZ,ZZZ);

CCC
SChat
BBB
SELECT numberOFmessage AS "LastGenerateUUID", TimeOfMessage 
FROM chatXXX 
WHERE numberOFmessage = LAST_INSERT_ID();

