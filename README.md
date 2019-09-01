# Forensics

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites
* [Kafka](https://kafka.apache.org/quickstart) - Queuing framework used widely in our projects
* [Scylladb](https://docs.scylladb.com/getting-started/getting-started/) - Database storage system
* [compress_linux Project](http://118.126.94.181/byy_/compress_linux) - Sender framework for json data (mainly for testing)

### Importing the project
Import to IntelliJ should be done using gradle import

### Configuration
The configuration scheme is bad because it's just a class instead of using a dynamic config file.
The class edu.nu.forensic.GlobalConfig is the part where you would do the configuration. Cassandra and Kafka can be configured easily.
Number of hosts supported by our system should be indicated using NUM_SERVERS. This creates receivers for each queue (abc0, abc1,...).
NUM_SERVERS_PER_CONNECTION indicates how many servers should use the same Scylladb conncection. We haven't found the optimal number, but I think it's okay to put this number 1/10 of NUM_SERVERS. So if NUM_SERVERS is 150, the recommend number for this parameter is 15.
The last config is FORMAT_FILE_LOCATION which needs to be exactly the same one used in compress_linux project

### Running
If using gradle, on the right pane in IntelliJ, open Gradle->distribution->distZip. The zip file generated in [root project folder]/distributions/forensic.zip can be run on Windows and Linux machines. You just need to run ./forensic inside the bin folder of that zip.

Otherwise, you just need to run edu.nu.forensic.reader.JsonReceiveDataFromKafkaAndSave class to run the project.

### Tracking functions
Tracking functions are not used anywhere in the project as of now. All functions are implemented in edu.nu.forensic.db.cassandra.ConnectionToCassandra class. Testing can be done in the root Main class. There is an example on how to use those functions in edu.nu.forensic.Main



