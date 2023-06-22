Suser
BBB
-- BB special character, which mean  begin of query
SELECT A.UUIDUser as UUIDUser, 
       CASE 
           WHEN COUNT(B.UUIDUser) = 0 THEN false
           ELSE true
       END as FinishRegistration
FROM registerusers AS A
LEFT JOIN userinformation AS B ON A.UUIDUser = B.UUIDUser
 where Email= ZZZ and passwords=ZZZ;