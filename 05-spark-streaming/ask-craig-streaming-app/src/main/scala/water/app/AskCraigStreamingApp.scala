package org.apache.spark.examples.h2o

import java.io.File
import java.net.URI

import hex.Model
import hex.Model.Output
import org.apache.spark.SparkContext
import org.apache.spark.h2o.H2OContext
import org.apache.spark.mllib.feature.Word2VecModel
import org.apache.spark.mllib.linalg.{Vectors, Vector}
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
    val sqlContext = new SQLContext(sc)

    // We need also streaming context
    val ssc = new StreamingContext(sc, Seconds(10))

    // Start H2O services
    val h2oContext = new H2OContext(sc).start()

    try {
      // Load models
      // - Load H2O model
      val h2oModel: Model[_, _, _] = loadH2OModel(new File("../models/h2omodel.bin").toURI)
      val modelId = h2oModel._key.toString
      val classNames = h2oModel._output.asInstanceOf[Output].classNames()
      // - Load Spark model
      val sparkModel = loadSparkModel[Word2VecModel](new File("../models/sparkmodel.bin").toURI)

      // Create DStream on port 9999
      val jobTitlesStream = ssc.socketTextStream("localhost", 9999)

      // Classify incoming messages
      jobTitlesStream.filter(!_.isEmpty)
        .map(jobTitle => (jobTitle, classify(jobTitle, modelId, sparkModel)))
        .map(pred => "\"" + pred._1 + "\" = " + show(pred._2, classNames))
        .print()

      println("Please start the event producer at port 9999, for example: nc -lk 9999")
      ssc.start()
      ssc.awaitTermination()
    } catch {
      case e: Throwable => e.printStackTrace()
    } finally {
      ssc.stop(stopSparkContext = true)
      h2oContext.stop()
    }
  }

  def classify(jobTitle: String, modelId: String, w2vModel: Word2VecModel): (String, Array[Double]) = {
    val model : Model[_, _, _] = water.DKV.getGet(modelId)

    import CraigsListJobTitles._
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

/** Reused from 03-ask-craig script. */
object CraigsListJobTitles {
  val STOP_WORDS = Set("ax","i","you","edu","s","t","m","subject","can","lines","re","what"
                       ,"there","all","we","one","the","a","an","of","or","in","for","by","on"
                       ,"but", "is", "in","a","not","with", "as", "was", "if","they", "are", "this", "and", "it", "have"
                       , "from", "at", "my","be","by","not", "that", "to","from","com","org","like","likes","so")

  val EMPTY_PREDICTION = ("NA", Array[Double]())

  // Define tokenizer function
  def tokenize(line: String, stopWords: Set[String]): Array[String] = {
    //get rid of nonWords such as puncutation as opposed to splitting by just " "
    line.split("""\W+""")
      .map(_.toLowerCase)

      //remove mix of words+numbers
      .filter(word => """[^0-9]*""".r.pattern.matcher(word).matches)

      //remove stopwords defined above (you can add to this list if you want)
      .filterNot(word => stopWords.contains(word))

      //leave only words greater than 1 characters.
      //this deletes A LOT of words but useful to reduce our feature-set
      .filter(word => word.length >= 2)
  }
  // Make some helper functions
  def sumArray (m: Array[Double], n: Array[Double]): Array[Double] = {
    for (i <- 0 until m.length) {m(i) += n(i)}
    return m
  }

  def divArray (m: Array[Double], divisor: Double) : Array[Double] = {
    for (i <- 0 until m.length) {m(i) /= divisor}
    return m
  }

  def wordToVector (w:String, m: Word2VecModel): Vector = {
    try {
      return m.transform(w)
    } catch {
      case e: Exception => return Vectors.zeros(100)
    }
  }

  def wordsToVector(words: Array[String], model: Word2VecModel): Vector = {
    val vec = Vectors.dense(
      divArray(
        words.map(word => wordToVector(word, model).toArray).reduceLeft(sumArray),
        words.length))
    vec
  }
}

