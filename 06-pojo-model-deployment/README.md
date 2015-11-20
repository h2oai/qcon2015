#QCon2015: Using generated models inside Spark Stream

  - deploy save POJO model as Spark stream
  
## Steps

 - create a standalone application
 - instantiate H2O Pojo model
 - load Spark model from disk
 - initialize Spark Streaming context
 - create a stream for processing incoming messages
  
0. Explanation
   - Where is POJO?
   - What are differences between binary model and POJO? 

1. Start with template of standalone application from previous example
    ```scala
    /**
      * Streaming app using saved models.
      */
    object AskCraigStreamingApp extends SparkContextSupport  {
    
      def main(args: Array[String]) {
        // Prepare environment
        ...
        // Start H2O services
        ...
      }
    }
    ```
 
2. Prepare environment - SparkContext, SQLContext, H2OContext and Spark StreamingContext
    ```scala
    // In the context of main function
    val sc = new SparkContext(configure("AskCraigStreamingApp"))
    val sqlContext = new SQLContext(sc)
    
    // We need also streaming context
    val ssc = new StreamingContext(sc, Seconds(10))
    ```

3. Instantiate H2O POJO model directly or via reflection
    ```scala
    val h2oModel: GenModel = Class.forName("GbmModel").newInstance().asInstanceOf[GenModel]
    val classNames = h2oModel.getDomainValues(h2oModel.getResponseIdx)
    ```

4. Load Spark Word2VecModel
    ```scala
      def loadSparkModel[M](source: URI) : M = {
        import java.io.FileInputStream
        import java.io.ObjectInputStream
        val fos = new FileInputStream(new File(source))
        val oos = new ObjectInputStream(fos)
        val newModel = oos.readObject().asInstanceOf[M]
        newModel
      }
    ```
    and then
    ```scala
    // - Load Spark model
    val sparkModel = loadSparkModel[Word2VecModel](new File("../models/sparkmodel.bin").toURI)
    ```

5. Create Spark DStream (Discretized stream) which will be connected to localhost:9999
    ```scala
    val jobTitlesStream = ssc.socketTextStream("localhost", 9999)
    ``` 	

6. Define DStream behavior
    ```scala
           // Classify incoming messages
          jobTitlesStream.filter(!_.isEmpty)
            .map(jobTitle => (jobTitle, classify(jobTitle, model, sparkModel)))
            .map(pred => "\"" + pred._1 + "\" = " + show(pred._2, classNames))
            .print()
    ``` 

7. Implement classify method for POJO model
    ```scala
    def classify(jobTitle: String, model: GenModel, w2vModel: Word2VecModel): (String, Array[Double]) = {
      import CraigsListJobTitles._
      val tokens = tokenize(jobTitle, STOP_WORDS)
      if (tokens.length == 0)
        EMPTY_PREDICTION
      else {
        val vec = wordsToVector(tokens, w2vModel)
    
        val prediction = new Array[Double](model.getNumResponseClasses + 1)
        // Low-leve API
        model.score0(vec.toArray, prediction)
        (model.getDomainValues(model.getResponseIdx)(prediction(0).asInstanceOf[Int]), prediction slice (1, prediction.length))
      }
    }
    ```

8. Simple show method
    ```scala
    def show(pred: (String, Array[Double]), classNames: Array[String]): String = {
        val probs = classNames.zip(pred._2).map(v => f"${v._1}: ${v._2}%.3f")
        pred._1 + ": " + probs.mkString("[", ", ", "]")
    }
    ```

8. Reuse `tokenize` and `wordsToVector` methods from previous demos
  
## Exercise
  - How would you expose POJO in different way?
  

