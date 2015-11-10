package water.apps

import java.util

import hex.tree.gbm.GBMModel
import org.apache.spark.SparkContext
import org.apache.spark.h2o.H2OContext
import org.apache.spark.ml.feature.Word2VecModel
import org.apache.spark.mllib.feature
import org.apache.spark.sql.SQLContext
import water.DKV
import water.api.Prediction
import water.api.AskCraig.Iface

/**
 * Implementation of ask craig controller.
 */
class AskCraigHandler(val app : CraigslistJobTitlesApp) extends Iface {

  private var gbmModel : String = _
  private var w2vModel : feature.Word2VecModel = _

  override def getLabels: util.List[String] = {
    val model : GBMModel = DKV.getGet(gbmModel)
    util.Arrays.asList(model._output.classNames():_*)
  }

  override def predict(jobTitle: String): Prediction = {
    val pred = app.classify(jobTitle, gbmModel, w2vModel)
    new Prediction(pred._1,
                   util.Arrays.asList(pred._2.map(v => {
                     val x: java.lang.Double = v
                     x
                   }):_*))
  }

  override def buildModel(file: String): Unit = {
    val (gbmModel, w2vModel) = app.buildModels(file, "gbm.model");
    this.gbmModel = gbmModel._key.toString
    this.w2vModel = w2vModel
  }

  override def shutdown(): Unit = {
    app.shutdown()
  }

  private def cl(): ClassLoader = this.getClass.getClassLoader

  private def thriftFile(f : String): String =
    scala.io.Source.fromInputStream(cl.getResourceAsStream(f))
      .getLines()
      .mkString("\n")
}
