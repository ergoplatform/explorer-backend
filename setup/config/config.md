## Ergo TestNet Config

TestNet configuration for explorer backend modules

```app.conf
#TestNet Master Node
network.master-nodes = ["http://195.201.82.115:9052"]
#TestNet Master Node

#TestNet DB
db.url = "http://195.201.82.115:8082/?pgsql=postgres&username=ergo_admin&db=explorer&ns=public" 
db.user = "postgres"
db.pass = "iJolMnx1wsRinlk9muNidEYe3Q2Rei4LoJ5iPaus7"
db.cp-size = 8
#TestNet DB

#TestNet Protocol
protocol.network-prefix = 16
protocol.genesis-address = "AfYgQf5PappexKq8Vpig4vwEuZLjrq7gV97BWBVcKymTYqRzCoJLE9cDBpGHvtAAkAgQf8Yyv7NQUjSphKSjYxk3dB3W8VXzHzz5MuCcNbqqKHnMDZAa6dbHH1uyMScq5rXPLFD5P8MWkD5FGE6RbHKrKjANcr6QZHcBpppdjh9r5nra4c7dsCgULFZfWYTaYqHpx646BUHhhp8jDCHzzF33G8XfgKYo93ABqmdqagbYRzrqCgPHv5kxRmFt7Y99z26VQTgXoEmXJ2aRu6LoB59rKN47JxWGos27D79kKzJRiyYNEVzXU8MYCxtAwV"
```