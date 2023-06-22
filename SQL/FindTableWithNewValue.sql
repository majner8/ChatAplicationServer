Suser
BBB

select chatUUID from(
%(select XXX as "chatUUID" from XXX where TimeOfreceived>=ZZZ and TimeOfreceived<ZZZ 
limit 1)
   -F UNION ALL -F %

) as t;