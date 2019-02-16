# ChordDistributedHashTable

1) Uses Apache thrift to create basic distributed hash table with an architecture similar to chord system. 
2) Writes data provided by client on server side nodes by generating SHA-256 and reads the data from node where the particular data is 
stored by using the finger tables to reduce the number of hops from node to node. 
3) Uses Java and Apache Thrift.

Execution:
1)Type ./server.sh port on server remotes.

2)Then, type ./init node.txt in project folder

3)Then cd java
  ./client.sh ip_address port_number
