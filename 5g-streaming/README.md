# 5G Data Streaming

## The Problem

Telecommunication companies produce data **extremely fast.**

To make use of this data, companies need to **capture, transform, and analyze it in real time.** They also need to store the data in an easy format for future use (AI model training etc).

But sometimes, this data comes in so quickly that it can overwhelm a traditional database. So **we require an intermediary buffer** (Data Grid) for fast reads and writes.

## The Architecture

Our architecture will generate data from cell devices, transform it, and store it in our cache for fast operations. We’ll also hook up a dashboard to see what's going on in our cell tower.

![5G Streaming Architecture](img/5g-streaming-arch.png)

## Deploy AMQ Streams (Kafka)

We’ll use AMQ Streams to handle events being generated from our cell devices.

First, we’ll install the AMQ Streams Operator. On the left menu of the OpenShift console, **select Operators → OperatorHub** and **type “AMQ Streams”.**

**Click Install.** Make sure the “Installed Namespace” is openshift-operators. Click Install again.

![AMQ Streams Installation](img/streams_install_overview.png)

After the operator finishes installing, we’ll create a new Kafka cluster. On the left menu, **select “Operators → Installed Operators” and click “AMQ Streams”.**

[PIC]

Under the “Provided APIs”, **create an instance of Kafka.** Name the cluster my-cluster and use the default settings. Click “Create”.

![AMQ Streams Installation](img/streams_cluster.png)

Finally, we’ll create a topic to stream our call events. In the horizontal menu, **select Kafka Topic.** Click “Create Kafka Topic” and name the topic `call-records.`

## Deploy Data Grid & MySQL Storage

We’ll use data grid to cache our call events for quick access. **Data Grid is a distributed, in-memory cache that accelerates data processing.**

Technically, our call events flow to data grid after we transform it using Flink (below), but the Flink setup actually requires Data Grid to be up.

First, we’ll install the Data Grid Operator. On the left menu, **select Operators → OperatorHub and type “Data Grid”.** Click Install. The “Installed Namespace” should be openshift-operators. Click Install again.

After the operator finishes installing, we’ll create a new data grid cluster. On the left menu, **select Operators → Installed Operators** and click “Data Grid”.

Under the provided APIs, **select Infinispan Cluster.**

![Data Grid Installation](img/data_grid_install.png)

**Switch to the YAML view.**
Replace everything in the editor with the configuration below.

```
apiVersion: infinispan.org/v1
kind: Infinispan
metadata:
  name: datagrid
  namespace: openshift-operators
spec:
  security:
    authorization:
      enabled: false
    endpointEncryption:
      type: None
    endpointAuthentication: false
  dependencies:
    artifacts:
      - maven: 'com.mysql:mysql-connector-j:8.3.0'
  replicas: 1
```
**This creates a single-replica data grid cluster.** It also downloads a connector that we’ll use to save our cache data to mysql.

### Deploy MySQL

After our call records get stored in cache, we’ll want to put them in a database for future access. So we’ll create a mysql database.

On the left menu, **click “Administrator” and switch to the developer view.** Select the `openshift-operators` project.

Under the developer catalog, select Database.

[PIC]

**Select mysql (Ephemeral)**, click “Instantiate Template”, and use the following properties.

```
Database Service Name: sensordb
MySQL Connection Username: tolarewaju3
MySQL Connection Password: tolarewaju3
MySQL Connection root user password: myP@ssword!
MySQL Database Name: sensor
```
**Click Create.** Wait for the pod to fully deploy. You should see a blue filled circle appear over the pod.

Finally, we’ll create a table to store our call events. Under topology, **click on** the `sensordb` pod and **select Terminal.**

[PIC]

Login to our mysql server and enter the password (tolarewaju3).

`mysql -u tolarewaju3 -p`

**Change the database to sensor.**

`USE sensor;`

**Create a table** for our call records.
```
CREATE TABLE call_record (
    id INT PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    location varchar(255),
    signalStrength varchar(255),
    network varchar(255)
);
```
## Deploy Apache Flink

The data that comes in from cell devices is often unstructured, So we’ll use Apache Flink to transform the data and store it in data grid.

First, we’ll install the Flink Operator. 

**Switch back to the Administrator view.** On the left menu, **select Operators → OperatorHub** and type Flink. Click Install, use the `openshift-operator` namespace, and create the flink operator.

![Flink Installation](img/flink.png)

After the operator finishes installing, we’ll create a new Flink deployment. On the left menu, **select Installed Operators and click Flink Kubernetes Operator.**

Under the Provided APIs, **select Flink deployment.** **Switch the view to YAML** and paste in the configuration below.

```
kind: FlinkDeployment
apiVersion: flink.apache.org/v1beta1
metadata:
  name: flink-streaming
  namespace: openshift-operators
spec:
  image: 'flink:1.16'
  flinkVersion: v1_16
  flinkConfiguration:
    taskmanager.numberOfTaskSlots: '2'
  serviceAccount: flink
  jobManager:
    resource:
      memory: 2048m
      cpu: 1
  taskManager:
    resource:
      memory: 2048m
      cpu: 1
```
We’ll also create a route so we can access the Flink deployment. On the right hand menu, **click Networking → Routes.**

**Click Create route.** Change to the YAML view, and replace everything in the editor with the configuration below.

```
kind: Route
apiVersion: route.openshift.io/v1
metadata:
  name: flink-rest
  namespace: openshift-operators
  labels:
    app: flink-streaming
    type: flink-native-kubernetes
spec:
  to:
    kind: Service
    name: flink-streaming-rest
  tls: null
  port:
    targetPort: rest
```
Next, we’ll deploy a Flink job. This streaming job will transform our call record data into one that’s suitable for storage and viewing.

Open the Flink deployment. Under Location, **navigate to the Flink deployment url.** *If you get an error, make sure your browser didn’t change the “http” to “https”*

[PIC]

On the left menu, **select Submit New Job.** Click “+Add New” and upload this jar [LINK]. This may take a minute.

When this finishes, click on the job and enter `com.demo.flink.streaming.StreamingJob` for the entry class field. **Hit “Submit”.**

## Generate Calls

Now, we’ll generate some call record data.

**Switch back to the Developer view.** Click “+Add” and select “Container Images” under the Developer Catalog. Fill in the properties below.

```
Image name from external registry: tolarewaju3/call-record-generator-amd64
Application: No application group
Name: call-record-generator
```
## Create Dashboard

The best part of capturing any data is viewing it. We’ll create a dashboard that shows interesting facts about our data. Metabase is a great library that makes it easy for us to visualize data.

First, we’ll create a mysql database to support our metabase dashboard.

On the left menu, **click “Administrator” and switch to the developer view.** Make sure the `openshift-operator` project is selected.

**Click +Add and select Database** under the Developer Catalog.

**Select MySql (Ephemeral)**, click Instantiate Template and use the properties below.

```
Namespace: openshift-operators
Database Service Name: metabasedb
MySQL Connection Username: tolarewaju3
MySQL Connection Password: tolarewaju3
MySQL Database Name: metabase
```
Next, we’ll create our metabase container. **Click the “+Add” and select “Container Images”**. Fill in the properties below.

```
Image name from external registry: metabase/metabase
Application: No application group
Name: metabase
```
Set environment properties on our container. On the bottom of the window, **select “Deployment” and fill in the following variables.**
```
MB_DB_TYPE: mysql
MB_DB_DBNAME: metabase
MB_DB_PORT: 3306
MB_DB_USER: tolarewaju3
MB_DB_PASS: tolarewaju3
MB_DB_HOST: metabasedb
```
After the deployment finishes (you’ll see a dark blue ring around the pod), go to the metabase url by clicking on the arrow.

[PIC]

Click “Let’s get started” and fill in your information. Don’t worry, they won’t spam you.

You’ll see a screen asking what you’ll use metabase for. Select “Self-service analytics for my own company”.

When you reach the “Add data” screen, select MySQL and fill in the details below.

```
Display Name: Sensor Data
Host: sensordb
Database Name: sensor
Username: tolarewaju3
Password: tolarewaju3
```
**Click continue.** Click Finish. Click “Take me to Metabase”.

First, we’ll create a new dashboard for our sensor data. In the top right corner, **click “+New → Dashboard”.**

**Name the dashboard** `Sensor Data`. Click Create. And click save in the upper right corner.

[PIC]

We’ll add three SQL queries to our dashboard. In the upper right corner, **click “+New → SQL Query”.** 

Select our Sensor Data database and enter the query below.

```
SELECT
  COUNT(*)
FROM
  `call_record`
```
**Click the play button on the bottom right.** You should see the number of calls displayed.

At the top right, **click “Save” and name the question “Total Calls Received”.** When prompted to add to a dashboard, select “Sensor Data”. 

**Click Save** in the top right corner.

We’ll create our next item on our dashboard. In the upper right corner, **click “+New → SQL Query”.** Select our Sensor Data database and enter the query below.

```
SELECT
  `call_record`.`timestamp` AS `Time`,
  `call_record`.`location` AS `Location`,
  `call_record`.`signalStrength` AS `Signal Strength`,
  `call_record`.`network` AS `Network`
FROM
  `call_record`
LIMIT
  100
```
**Hit the play button.** You should see our call record data displayed in a table.

At the top right, click “Save” and name the question “All Call Records”. Click Save. Add the query to the dashboard we created. Click Save.

Follow the same steps above to add our third dashboard element. Use the query below.

```
SELECT
  `call_record`.`location` AS `location`,
  COUNT(*) AS `count`
FROM
  `call_record`
GROUP BY
  `call_record`.`location`
ORDER BY
  `count` DESC,
  `call_record`.`location` ASC
LIMIT
  10
```
**Click play on the bottom right.** Click the visualization button on the bottom left and select the bar chart.

Save the question and name it “Calls by Location”. Add it to our dashboard. Hit Save.

Ok, last one! Add a new SQL query just as before.

```
SELECT
  `call_record`.`network` AS `network`,
  COUNT(*) AS `count`
FROM
  `call_record`
GROUP BY
  `call_record`.`network`
ORDER BY
  `call_record`.`network` ASC
```

**Click the play button on the bottom.** Select Visualization and choose a pie chart. Save the question and name it “Calls by Network Type”.

Back at your dashboard, you can resize the elements to make it look pretty. Here’s my finished dashboard.

![Flink Installation](img/dashboard.png)

We’ve successfully created a Streaming dashboard for our call record data!


