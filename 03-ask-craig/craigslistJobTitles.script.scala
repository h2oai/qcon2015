/**
 *  Craigslist example
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

// All strings which are not useful for text-mining
val STOP_WORDS = Set("ax","i","you","edu","s","t","m","subject","can","lines","re","what"
    ,"there","all","we","one","the","a","an","of","or","in","for","by","on"
    ,"but", "is", "in","a","not","with", "as", "was", "if","they", "are", "this", "and", "it", "have"
    , "from", "at", "my","be","by","not", "that", "to","from","com","org","like","likes","so")

val allLabelledWords = data.map(d => (d(0), tokenize(d(1), STOP_WORDS)))
val rareWords = computeRareWords(allLabelledWords.map(r => r._2))
// Filter all rare words
val labelledWords = allLabelledWords.map(row => {
  val tokens = row._2.filterNot(token => rareWords.contains(token))
  (row._1, tokens)
}).filter(row => row._2.length > 0)

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

def wordsToVector(words: Array[String], model: Word2VecModel): Vector = {
  val vec = Vectors.dense(
    divArray(
      words.map(word => wordToVector(word, model).toArray).reduceLeft(sumArray),
      words.length))
  vec
}


//
// Word2Vec Model
//
val word2vec = new Word2Vec()
val word2VecModel = word2vec.fit(words.map(r => r.toSeq))

// Sanity Check
word2VecModel.findSynonyms("teacher", 5).foreach(println)

// Create H2OFrame
import org.apache.spark.mllib
case class JobPosting(category: String, a: mllib.linalg.Vector)

// Start H2O services
import org.apache.spark.h2o._
val h2oContext = new H2OContext(sc).start()

val resultRDD: DataFrame = labels.zip(words).map(row => {
  val label = row._1
  val tokens = row._2
  val vectorizedTokens = wordsToVector(tokens, word2VecModel)
  JobPosting(label, vectorizedTokens)
}).toDF

val table: H2OFrame = h2oContext.asH2OFrame(resultRDD, "craigslistTable")
table.replace(table.find("category"), table.vec("category").toCategoricalVec).remove()
table.update(null)
// OPEN FLOW UI
h2oContext.openFlow

//
// Build model in Flow and call it "GbmModel"
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
// Get model from H2O DKV
val gbmModel: _root_.hex.tree.gbm.GBMModel = DKV.getGet("GbmModel")
exportH2OModel(gbmModel, new File("./models/h2omodel.bin").toURI)


// Save Spark model
def exportSparkModel(model: Any, destination: URI): Unit = {
  import java.io.FileOutputStream
  import java.io.ObjectOutputStream
  val fos = new FileOutputStream(new File(destination))
  val oos = new ObjectOutputStream(fos)
  oos.writeObject(model)
  oos.close
}
exportSparkModel(word2VecModel, new File("./models/sparkmodel.bin").toURI)

def exportPOJOModel(model : Model[_, _,_], destination: URI): URI = {
    val destFile = new File(destination)
    val fos = new java.io.FileOutputStream(destFile)
    val writer = new model.JavaModelStreamWriter(false)
    try {
      writer.writeTo(fos)
    } finally {
      fos.close()
    }
    destination
}

exportPOJOModel(gbmModel, new File("./models/GbmModel.java").toURI)

