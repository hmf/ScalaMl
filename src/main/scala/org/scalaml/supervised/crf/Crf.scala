/**
 * Copyright 2013, 2014  by Patrick Nicolas - Scala for Machine Learning - All rights reserved
 *
 * The source code in this file is provided by the author for the sole purpose of illustrating the 
 * concepts and algorithms presented in "Scala for Machine Learning" ISBN: 978-1-783355-874-2 Packt Publishing.
 * Unless required by applicable law or agreed to in writing, software is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * 
 * Version 0.92
 * 
 * This code uses the iitb CRF library 
 * Copyright (c) <2004> <Sunita Sarawagi Indian Institute of Technology Bombay> All rights reserved.
 */
package org.scalaml.supervised.crf

import iitb.CRF.{CRF, CrfParams, DataSequence, DataIter, FeatureGenerator}
import iitb.Model.{FeatureGenImpl, CompleteModel}
import org.scalaml.core.XTSeries
import org.scalaml.workflow.data.DataSource
import org.scalaml.workflow.PipeOperator
import org.scalaml.supervised.Supervised
import java.io.IOException
import org.scalaml.core.Types.ScalaMl._


import CrfConfig._
import Crf._


	/**
	 * <p>Generic class for the linear chained CRF for tagging words, N-Grams or regular expression. The class
	 * define a Feature generator class that inherits the default implementation FeatureGenImpl of iitb features generator.
	 * The class assumes the training sequences are loaded from file with *.raw and *.tagged extensions.
	 * @param nLabels Number of labels (or tags) used in tagging training sequences of observations.
	 * @param config minimalist set of configuration parameters used in the CRF
	 * @param delims delimiters used in extracting labels and data from the training files
	 * @param taggedObs identifier for the training data set. The training set of sequences is defined by the raw observations
	 * and its associated tagged file    taggedObs.raw and taggedObs.tagged files.
	 * @throws IllegalArgumentException if nLabels, config, delims and taggedObs are either undefined or out of range.
	 * @see org.scalaml.workflow.PipeOperator
	 * 
	 * @author Patrick Nicolas
	 * @since April 3, 2014
	 * @note Scala for Machine Learning
	 */
final class Crf(val nLabels: Int, val config: CrfConfig, val delims: CrfSeqDelimiter, val taggedObs: String) 
                                               extends PipeOperator[String, Double] with Supervised[String] {
	
  validate(nLabels, config, delims, taggedObs)
  
  class TaggingGenerator(nLabels: Int) extends FeatureGenImpl(new CompleteModel(nLabels) , nLabels, true)
	
  private[this] val features = new TaggingGenerator(nLabels)
  private[this] lazy val crf = new CRF(nLabels, features, config.params)
  
  private val model: Option[CrfModel] = {
  	 val seqIter = CrfSeqIter(nLabels: Int, taggedObs, delims)
  	 try {
	  	 features.train(seqIter)
	  	 Some(crf.train(seqIter))
  	 }
  	 catch {
  		 case e: IOException => Console.println(e.toString); None
  		 case e: Exception  =>Console.println(e.toString); None
  	 }
  }
  
  		/**
  		 * <p>Predictive method for the conditional random field.</p>
  		 */

  override def |> (obs: String): Option[Double] = model match {
  	 case Some(w) => {
  	     require( obs != null && obs.length > 1, "Argument for CRF prediction is undefined")
  	     
	  	 val dataSeq =  new CrfRecommendation(nLabels, obs, delims.dObs)
	  	 Some(crf.apply(dataSeq))
  	 }
  	 case None => None
  }
  
  @inline
  final def weights: Option[CrfModel] = model
  
  override def validate(output: XTSeries[(Array[String], Int)], index: Int): Double = -1.0
  
  private def validate(nLabels: Int, config: CrfConfig, delims: CrfSeqDelimiter, taggedObs: String): Unit = {
	 require(nLabels > NUM_LABELS_LIMITS._1 && nLabels < NUM_LABELS_LIMITS._2, "Number of labels for generating tags for CRF " + nLabels + " is out of range")
	 require(config != null, "Configuration of the linear Chain CRF is undefined")
	 require(delims != null, "delimiters used in the CRF training files are undefined")
	 require(taggedObs != null, "Tagged observations used in the CRF training files are undefined")
  }
}



	/**
	 * Companion object for the Linear chained Conditional random field. The singleton is used
	 * to define the constructors.
	 */
object Crf {
  final val NUM_LABELS_LIMITS = (1, 200)
  type CrfModel = DblVector
	
  def apply(nLabels: Int, config: CrfConfig, delims: CrfSeqDelimiter, taggedObs: String): Crf = 
		           new Crf(nLabels, config, delims, taggedObs)
}


// ---------------------------- EOF ------------------------------------------------------