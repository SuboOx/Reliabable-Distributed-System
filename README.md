# A Demo of Reliable Distributed System

## Protocol
 
 ### Input
 
Currently support input in client `var=value`, an `id` will be automatically generated by protocol.

### Packing and unpacking

`val=value` will be packed in the form of `?id?$var$#value#` and send to server. server will unpack it to `var` and `value` and store it in database.

## Database

Currently a naive `hashmap<String, String>`.
