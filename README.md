# QCon 2105

Materials for the San Francisco QConf 2015 Workshop. The goal for the day is to learn to use Sparkling Water to build smart applications driven by machine learning models. The tutorials will go over:
    - How to clean and munge data in Spark and H2O.
    - How to read in multiple datasets and join them to provide more features to the machine learning process.
    - How to use MLlib in conjunction with H2O's library or algorithms to take the best of platforms using Sparkling Water.
    - How to integrate the scoring engine from your Sparkling Water script into Spark Streaming to produce real-time predictions.
    - How to deploy smarter applications in Databricks' platform.

1. [Sparkling Water Introduction](#01-sparkling-water-intro/README.md)
    - Setup and Installation of Sparkling Water
    - Basic architecture and overview of functionalities
    - Hands on demostration of Sparkling Water
2. [Spam Detector](#02-ham-or-spam/README.md)
    - Use Spark to tokenize text
    - Use MLlib's TF-IDF model to transform the data into a table
    - Build GBM model to label incoming text as spam or not spam (ham)
3. [Ask Craig(list) Application](#03-ask-craig/README.md)
    - Build a classifier to label job description into appropriate industry categories
4. [Spark Streaming](#04-spark-streaming/README.md)
    - Deploy the classification model inside Spark Streaming
5. [Model Deployment](#05-model-deployment/README.md)
    - Exporting the model as a POJO and binary form
    - Exposing the model through the REST API
6. [Final Application](#06-final-app/README.md) 
    - Assembling the final application: combining the front end and back end
    - Deploying the application to the cluster
