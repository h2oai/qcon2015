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
  }
}
