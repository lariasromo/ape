### Scenario
S3 files are stored compressed (gzip) and contains multiple records (as json), one per line (json lines)

Example of records:
```json lines
{"firstName":"Keith", "lastName":"Richards", "country": "AR"}
{"firstName":"Neil", "lastName":"McDonald", "country": "US"}
{"firstName":"Michael", "lastName":"Murphy", "country": "GB"}
{"firstName":"Stephen", "lastName":"Smith", "country": "MX", "address": "1/2 Example Street. State. Country. AB10CD"}
```

This example consists on a pipeline that will read from S3;
1. Reads new files from S3 (using a path pattern) every 5 minutes
2. Opens the file and converts each record to a class. The class may not contain all fields from the file.  
3. Applies a simple filter by country (any country from LATAM)
5. Writes the extracted and filtered records to console, clickhouse and kafka