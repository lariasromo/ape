### Scenario
Records with transactions are sent to a kafka broker, such records include a transaction type, status and clientId reference
Records are written in avro

Example of a record in json just for visual representation, actual records are bytes representing an Avro record compressed using an avro schema

```json
{
  "status": "PENDING",
  "transactionType": "DEPOSIT",
  "clientId": "123456"
}
```

This example consists on a pipeline that will; 
1. Reading bytes from the Kafka message 
2. Convert the message to a case class
3. Apply a simple filter (status=SUCCEEDED or SUCCESS)
4. Write the filtered record to Cassandra and PostgreSQL

See the example code on this file [Example 1](/core/src/main/scala/examples/pipes/Example1.scala)