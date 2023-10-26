package org.example.ml

import cats.effect.IO
import smile.data.DataFrame
import smile.data.`type`.StructType
import smile.read
import smile.validation._

import java.nio.file.Paths

object SmileUtils {
  def fromResourceCsv(schema: StructType)(resourceCsvFilename: String): IO[DataFrame] = IO.delay {
    val resourcePath = Paths.get(getClass.getResource(s"/$resourceCsvFilename").toURI)
    read.csv(resourcePath.toAbsolutePath.toString, schema = schema)
  }
  def qualityCharacteristics(truth: Array[Int], prediction: Array[Int], probability: Array[Double]): QualityCharacteristics = {
    QualityCharacteristics(
      accuracy(truth, prediction),
      precision(truth, prediction),
      recall(truth, prediction),
      f1(truth, prediction),
      logloss(truth, probability)
    )
  }
}

case class QualityCharacteristics(accuracy: Double, precision: Double, recall: Double, f1: Double, logLoss: Double) {
  override def toString: String =
    f"""{
       |  accuracy $accuracy%.4f,
       |  precision $precision%.4f,
       |  recall $recall%.4f,
       |  F1 score $f1%.4f,
       |  log loss $logLoss%.4f,
       |}""".stripMargin
}