Suser
BBB
SELECT UserName FROM (
    SELECT CONCAT(name, ' ', LastName) as UserName
    FROM userinformation
    WHERE UUIDUser=ZZZ
) as t;
