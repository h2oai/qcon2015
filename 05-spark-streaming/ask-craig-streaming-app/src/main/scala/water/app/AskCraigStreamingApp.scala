package org.apache.spark.examples.h2o

import java.io.File
import java.net.URI

import hex.Model
import hex.Model.Output
import org.apache.spark.SparkContext
import org.apache.spark.h2o.H2OContext
import org.apache.spark.mllib.feature.Word2VecModel
import org.apache.spark.sql.SQLContext
import org.apache.spark.streaming.{Seconds, StreamingContext}
import water.app.SparkContextSupport
import water.serial.ObjectTreeBinarySerializer

/**
  * Streaming app using saved models.
  */
object AskCraigStreamingApp extends SparkContextSupport  {

  def main(args: Array[String]) {
    // Prepare environment
    val sc = new SparkContext(configure("AskCraigStreamingApp"))
    val ssc = new StreamingContext(sc, Seconds(10))
    val sqlContext = new SQLContext(sc)
    // Start H2O services
    val h2oContext = new H2OContext(sc).start()
    val staticApp = new CraigslistJobTitlesApp()(sc, sqlContext, h2oContext)

    try {
      val h2oModel: Model[_, _, _] = loadH2OModel(URI.create("file:///tmp/h2omodel"))
      val modelId = h2oModel._key.toString
      val classNames = h2oModel._output.asInstanceOf[Output].classNames()
      val sparkModel = loadSparkModel[Word2VecModel](URI.create("file:///tmp/sparkmodel"))

      // Start streaming context
      val jobTitlesStream = ssc.socketTextStream("localhost", 9999)

      // Classify incoming messages
      jobTitlesStream.filter(!_.isEmpty)
        .map(jobTitle => (jobTitle, staticApp.classify(jobTitle, modelId, sparkModel)))
        .map(pred => "\"" + pred._1 + "\" = " + show(pred._2, classNames))
        .print()

      println("Please start the event producer at port 9999, for example: nc -lk 9999")
      ssc.start()
      ssc.awaitTermination()
    } catch {
      case e: Throwable => e.printStackTrace()
    } finally {
      ssc.stop()
      staticApp.shutdown()
    }
  }

  val EMPTY_PREDICTION = ("NA", Array[Double]())

  def classify(jobTitle: String, model: Model[_,_,_], w2vModel: Word2VecModel): (String, Array[Double]) = {
    val tokens = tokenize(jobTitle, STOP_WORDS)
    if (tokens.length == 0)
      EMPTY_PREDICTION
    else {
      val vec = wordsToVector(tokens, w2vModel)

      hex.ModelUtils.classify(vec.toArray, model)
    }
  }

  def show(pred: (String, Array[Double]), classNames: Array[String]): String = {
    val probs = classNames.zip(pred._2).map(v => f"${v._1}: ${v._2}%.3f")
    pred._1 + ": " + probs.mkString("[", ", ", "]")
  }

  def loadH2OModel[M <: Model[_, _, _]](source: URI) : M = {
    val l = new ObjectTreeBinarySerializer().load(source)
    l.get(0).get().asInstanceOf[M]
  }

  def loadSparkModel[M](source: URI) : M = {
    import java.io.FileInputStream
    import java.io.ObjectInputStream
    val fos = new FileInputStream(new File(source))
    val oos = new ObjectInputStream(fos)
    val newModel = oos.readObject().asInstanceOf[M]
    newModel
  }

}

