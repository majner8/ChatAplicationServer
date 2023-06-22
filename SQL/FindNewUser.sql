Suser
BBB
select UUIDUser, CONCAT (name," ",LastName) AS UserTableName from userinformation
where UUIDUser !=ZZZ and( name likeZZZ or LastName likeZZZ or( name likeZZZ and LastName likeZZZ )) ;

