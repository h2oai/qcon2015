#QCon2015: Using generated models inside Spark Stream

  - Create and start Spark stream
  - Load Spark model from disk
  - Load H2O model from disk
  - Deploy saved binary models as Spark stream
  
## Steps

 - Create a standalone application
 - Load H2O model from disk
 - Load Spark model from disk
 - Initialize Spark Streaming context
 - Create a stream for processing incoming messages

1. Start with template of standalone application
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
    
    // Start H2O services
    val h2oContext = new H2OContext(sc).start()
    ```

3. Load H2O Model
    ```scala
      def loadH2OModel[M <: Model[_, _, _]](source: URI) : M = {
        val l = new ObjectTreeBinarySerializer().load(source)
        l.get(0).get().asInstanceOf[M]
      }
    ```
    and then
    ```scala
    // Load model
    val h2oModel: Model[_, _, _] = loadH2OModel(new File("../models/h2omodel.bin").toURI)
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
            .map(jobTitle => (jobTitle, classify(jobTitle, modelId, sparkModel)))
            .map(pred => "\"" + pred._1 + "\" = " + show(pred._2, classNames))
            .print()
    ``` 

7. Implement classify method
    ```scala
      def classify(jobTitle: String, modelId: String, w2vModel: Word2VecModel): (String, Array[Double]) = {
        val model : Model[_, _, _] = water.DKV.getGet(modelId)
    
        import CraigsListJobTitles._
        val tokens = tokenize(jobTitle, STOP_WORDS)
        if (tokens.length == 0)
          EMPTY_PREDICTION
        else {
          val vec = wordsToVector(tokens, w2vModel)
           // Use helper	
          hex.ModelUtils.classify(vec.toArray, model)
        }
      }
    ```

8. Reuse `tokenize` and `wordsToVector` methods from previous demos
  
## Exercise
  - How would you implement model re-training mechanism?
  - How would you stop stream?
  
## Points to remember
  - Spark streaming: stream definition
  - H2O model deploy
  - H2O model scoring API
  

