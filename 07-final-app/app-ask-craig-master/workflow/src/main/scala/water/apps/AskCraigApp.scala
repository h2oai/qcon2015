/*
* Licensed to the Apache Software Foundation (ASF) under one or more
* contributor license agreements.  See the NOTICE file distributed with
* this work for additional information regarding copyright ownership.
* The ASF licenses this file to You under the Apache License, Version 2.0
* (the "License"); you may not use this file except in compliance with
* the License.  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package water.apps

import org.apache.spark.h2o.H2OContext
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.thrift.server.TServer.Args
import org.apache.thrift.server.TSimpleServer
import org.apache.thrift.transport.TServerSocket
import water.api.AskCraig

/**
 * Example of Sparkling Water based application.
 */
object AskCraigApp {

  def main(args: Array[String]) {

    // Create Spark Context
    val conf = configure("Ask Craig Application server")
    val sc = new SparkContext(conf)
    // Create SQL Context
    val sqlContext = new SQLContext(sc)
    // Create and start H2O Context
    val h2oContext = new H2OContext(sc).start()

    val app = new CraigslistJobTitlesApp()(sc, sqlContext, h2oContext)
    // Create application server handler
    try {
      val handler = new AskCraigHandler(app)
      // Perform init
      handler.buildModel("data/craigslistJobTitles.csv")
      // Setup web server
      val processor = new AskCraig.Processor(handler)
      val serverTransport = new TServerSocket(9090)
      val args: Args = new Args(serverTransport).processor(processor)
      val server = new TSimpleServer(args)
      //
      println(s"\nApplication API is available on ${serverTransport.getServerSocket.getLocalSocketAddress} ...")
      // And start it!!!
      server.serve()
    } finally {
      // Shutdown application
      sc.stop()
    }

  }

  def configure(appName:String = "Sparkling Water Demo"):SparkConf = {
    val conf = new SparkConf()
      .setAppName(appName)
    conf.setIfMissing("spark.master", sys.env.getOrElse("spark.master", "local"))
    conf
  }
}
