# QCon2015: Build standalone Spark application

 - Get familiar with standalone Spark application
 - Open, build and run application in IntelliJ IDEA
 - Build and run application from Gradle
 - Submit application via `spark-submit`
   
## Project structure
 
```
├─ gradle/        - Gradle definition files
├─ src/           - Source code
│  ├─ main/       - Main implementation code 
│  │  ├─ scala/
│  ├─ test/       - Test code
│  │  ├─ scala/
├─ build.gradle   - Build file for this project
├─ gradlew        - Gradle wrapper 
```

## Project building

For building, please, use provided `gradlew` command:
```
./gradlew build
```

### Run demo
For running a simple application:
```
./gradlew run
```

## Starting with Idea

There are two ways to open this project in Idea

  * Using Gradle build file directly
    1. Open project's `build.gradle` in Idea via _File > Open_ 

## Starting with Eclipse
  1. Generate Eclipse project files via `./gradlew eclipse`
  2. Open project in Eclipse via _File > Import > Existing Projects into Workspace_

## Creating and Running Spark Application

Create application assembly which can be directly submitted to Spark cluster:
```
./gradlew shadowJar
```
The command creates jar file `build/libs/sparkling-water-droplet-app.jar` containing all necessary classes to run application on top of Spark cluster.

### Deploying application to Spark cluster 
Submit application to Spark cluster (in this case, local cluster is used):
```
export MASTER='local-cluster[3,2,1024]' # Point this to existing Spark cluster
$SPARK_HOME/bin/spark-submit --class water.droplets.SparklingWaterDroplet build/libs/sparkling-water-droplet-app.jar
```

## Exercise
  - Try to re-implement Ham Or Spam example or Ask Craig example as standalone application

## Points to remember
  - How to develop standalone Spark/Sparkling standalone application
  - How to deploy Spark application on Spark cluster.
  - How to reference a Spark cluster
