/**
 * Copyright 2013, 2014  by Patrick Nicolas - Scala for Machine Learning - All rights reserved
 *
 * The source code in this file is provided by the author for the sole purpose of illustrating the 
 * concepts and algorithms presented in "Scala for Machine Learning" ISBN: 978-1-783355-874-2 Packt Publishing.
 * Unless required by applicable law or agreed to in writing, software is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * Version 0.95d
 */
package org.scalaml.app.chap12

import org.scalaml.app.Eval
import scala.util.Random
import org.scalaml.scalability.akka.Partitioner
import org.scalaml.core.XTSeries
import akka.actor.Props
import org.scalaml.scalability.akka.Master
import org.scalaml.filtering.DFT
import org.scalaml.scalability.akka.Start
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable}
import akka.actor.ActorSystem	
import org.apache.log4j.Logger
import org.scalaml.core.types.ScalaMl._


class DFTMaster(xt: XTSeries[Double], partitioner: Partitioner) extends Master(xt, DFT[Double], partitioner) {
	override protected def aggregate: Seq[Double] = 
	  aggregator.transpose.map( _.sum).toSeq
}



object ActorsManagerEval extends Eval {   
	val name: String = "ActorsManagerEval"
	private val logger = Logger.getLogger(name)
	
	val DONE= 0
	val NUM_WORKERS = 4
	val NUM_DATA_POINTS = 1000000
	val h = (x:Double) =>	2.0*Math.cos(Math.PI*0.005*x) +	// simulated first harmonic
							Math.cos(Math.PI*0.05*x) +   	// simulated second harmonic
							0.5*Math.cos(Math.PI*0.2*x) + 	// simulated third harmonic 
							0.2*Random.nextDouble			// noise
	 
	implicit val actorSystem = ActorSystem("system") 
	
	def run(args: Array[String]): Int = {
  	 
		val xt = XTSeries[Double](Array.tabulate(NUM_DATA_POINTS)(h(_)))
		val partitioner = new Partitioner(NUM_WORKERS)
	
		val master = actorSystem.actorOf(Props(new DFTMaster(xt, partitioner)), "Master")
		master ! Start
		Thread.sleep(5000)
		actorSystem.shutdown
		DONE
	}
}


object ActorsManagerEvalApp extends App {
	ActorsManagerEval.run(Array.empty)
}


// ----------------------------------  EOF ------------------------