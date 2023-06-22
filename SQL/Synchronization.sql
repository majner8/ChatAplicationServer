SChat
BBB
-- Synchronizace

SELECT r1.tablename, r1.message, r1.userUUID, r1.TimeOfMessage, r1.numberOFmessage,r1.MessageUUID
FROM (
    SELECT r2.tablename, r2.message, r2.userUUID, r2.TimeOfMessage, r2.numberOFmessage,r2.MessageUUID,
        ROW_NUMBER() OVER(PARTITION BY r2.tablename ORDER BY r2.TimeOfMessage DESC) AS rn,
        MAX(r2.TimeOfMessage) OVER(PARTITION BY r2.tablename) AS MaxTimeOfMessage
    FROM (
    %SELECT message,userUUID ,TimeOfMessage,numberOFmessage, MessageUUID,'XXX' AS tablename FROM chatXXX
    where TimeOfMessage>ZZZ   
 
	-F UNION ALL -F %
) as r2
	ORDER BY TimeOfMessage DESC
    LIMIT 100
) as r1
ORDER BY MaxTimeOfMessage DESC, r1.rn;
