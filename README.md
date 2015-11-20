# QCon 2015

Materials for the San Francisco QConf 2015 Workshop. The goal for the day is to learn to use Spark, H2O and Sparkling Water to build smart applications driven by machine learning models. The tutorials will go over:
    - How to clean and munge data in Spark and H2O.
    - How to read in multiple datasets and join them to provide more features to the machine learning process.
    - How to use MLlib in conjunction with H2O's library or algorithms to take the best of platforms using Sparkling Water.
    - How to integrate the scoring engine from your Sparkling Water script into Spark Streaming to produce real-time predictions.
    - How to deploy smarter applications on top of Spark.
    - How to deploy simple models

## Outline 

1. [Spark & Sparkling Water Introduction](01-sparkling-water-intro/README.md)
    - H2O and Spark intro
    - Sparkling Water intro
    - Installation and setup of Spark
      - Running Spark shell
    - Installation and setup of Sparkling Water
    - Basic architecture and overview of functionalities
    - Hands on demonstration of Sparkling Water
      - Running Sparkling Shell      
2. [Simple Spam Detector](02-ham-or-spam/README.md)
    - Use Spark to tokenize text
    - Use MLlib's TF-IDF model to transform the data into a table
    - Build GBM model to label incoming text as spam or not spam (ham)
3. [Ask Craig(list) Application](03-ask-craig/README.md)
    - Build a classifier to label job description into appropriate industry categories
    - Deploy it as Spark application    
4. [Spark Streaming](04-spark-streaming/README.md)
    - Deploy the classification model inside Spark Streaming
5. [Model Deployment](05-model-deployment/README.md)
    - Exporting the H2O model in binary form
    - Exposing the model via Spark stream
6. [Model Deployment #2)](06-pojo-model-deployment)
    - Using exported POJO model in Spark stream
6. [Final Application](07-final-app/README.md) 
    - Assembling the final application: combining the front end and back end
7. [Lending Club Example](08-lending-club-app/README.md)
    - A smart app predicting loan interest
    - Off-line training pipeline driven from R
    - POJO models exposed via REST API
    

## Requirements
  * Mac OS X or Linux
  * Java 7
  * Spark 1.5+
  * Sparkling Water 1.5.6
  * IntelliJ IDEA development environment
  * Scala SDK 2.10.4 for IDEA (can be fetch from Ivy cache)
  * Maven dependencies (fetch by Gradle)
  
## Goals
  * Get familiar with Spark
  * Understand Sparkling Water
  * Combine power of Spark MLLib and Sparkling Water library to write machine
    learning flows
  * Write Spark/Sparkling Water standalone application
  * Deploy applications on Spark cluster
  * Deploy models
  
