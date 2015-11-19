/**
 *  * Craigslist example
 *
 * It predicts job category based on job description (called "job title").
 *
 * Launch following commands:
 *    export MASTER="local-cluster[3,2,4096]"
 *   bin/sparkling-shell -i examples/scripts/craigslistJobTitles.script.scala
 *
 * When running using spark shell or using scala rest API:
 *    SQLContext is available as sqlContext
 *    SparkContext is available as sc
 */
import org.apache.spark.mllib.feature.Word2Vec
import org.apache.spark.mllib.feature.Word2VecModel
import org.apache.spark.mllib.linalg._
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.DataFrame
import _root_.hex._
import water.serial.ObjectTreeBinarySerializer
import water._
import java.net.URI
import java.io.File

val sc: org.apache.spark.SparkContext = _

// Load and split data based on ",", skip header
val data = sc.textFile("../data/craigslistJobTitles.csv").filter(line => !line.contains("category")).map(d => d.split(','))

// Extract job category from job description
val jobCategories = data.map(l => l(0))
val jobTitles = data.map(l => l(1))

// Count of different job categories
val labelCounts = jobCategories.map(n => (n, 1)).reduceByKey(_+_).collect.mkString("\n")

/* Should return:
(education,2438)
(administrative,2500)
(labor,2500)
(accounting,1593)
(customerservice,2319)
(foodbeverage,2495)
*/

// All strings which are not useful for text-mining
val STOP_WORDS = Set("ax","i","you","edu","s","t","m","subject","can","lines","re","what"
    ,"there","all","we","one","the","a","an","of","or","in","for","by","on"
    ,"but", "is", "in","a","not","with", "as", "was", "if","they", "are", "this", "and", "it", "have"
    , "from", "at", "my","be","by","not", "that", "to","from","com","org","like","likes","so")

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

def computeRareWords(dataRdd : RDD[Array[String]]): Set[String] = {
  // Compute frequencies of words
  val wordCounts = dataRdd.flatMap(s => s).map(w => (w,1)).reduceByKey(_ + _)

  // Collect rare words
  val rareWords = wordCounts.filter { case (k, v) => v < 2 }
    .map {case (k, v) => k }
    .collect
    .toSet
  rareWords
}

val labelledWords = data.map(d => (d(0), tokenize(d(1), STOP_WORDS))).filter(s => s._2.length > 0)
val rareWords = computeRareWords(labelledWords.map(r => r._2))

val words = labelledWords.map(v => v._2)
val labels = labelledWords.map(v => v._1)

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

//
// Word2Vec Model
//

val word2vec = new Word2Vec()
val model = word2vec.fit(words)

// Sanity Check
model.findSynonyms("teacher", 5).foreach(println)

val titleVectors = words.map(x => new DenseVector(
    divArray(x.map(m => wordToVector(m, model).toArray).
            reduceLeft(sumArray),x.length)).asInstanceOf[Vector])

// Create H2OFrame
import org.apache.spark.mllib
case class CRAIGSLIST(target: String, a: mllib.linalg.Vector)

import org.apache.spark.h2o._
val h2oContext = new H2OContext(sc).start()
import h2oContext._

val resultRDD: DataFrame = labels.zip(titleVectors).map(v => CRAIGSLIST(v._1, v._2)).toDF

val table:H2OFrame = resultRDD

// OPEN FLOW UI
openFlow

//
// Build model in Flow
//

// Save H2O model
def exportH2OModel(model : Model[_,_,_], destination: URI): URI = {
  val modelKey = model._key.asInstanceOf[Key[_ <: Keyed[_ <: Keyed[_ <: AnyRef]]]]
  val keysToExport = model.getPublishedKeys()
  // Prepend model key
  keysToExport.add(0, modelKey)

  new ObjectTreeBinarySerializer().save(keysToExport, destination)
  destination
}
TODO: save

// Save Spark model
def exportSparkModel(model: Any, destination: URI): Unit = {
  import java.io.FileOutputStream
  import java.io.ObjectOutputStream
  val fos = new FileOutputStream(new File(destination))
  val oos = new ObjectOutputStream(fos)
  oos.writeObject(model)
  oos.close
}

