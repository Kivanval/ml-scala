package org.example.ml

import com.cibo.evilplot.numeric.Point
import smile.validation._

object Implicits {
  implicit class ClassificationValidationOps(cv: ClassificationValidation[_]) {

    private lazy val predictions = {
      val thresholds = BigDecimal(0) to BigDecimal(1) by BigDecimal(0.005)
      thresholds.map(threshold =>
        cv.posteriori.map(_(1)).map(prob => if (prob > threshold) 1 else 0) -> threshold
      )
    }

    lazy val prc: Seq[Point] = {
      predictions.map(_._1).map { prediction =>
        val rc = recall(cv.truth, prediction)
        val pr = precision(cv.truth, prediction)
        Point(
          if (rc.isNaN) 1 else rc,
          if (pr.isNaN) 1 else pr
        )
      }
    }

    lazy val roc: Seq[Point] = {
      predictions.map(_._1).map { prediction =>
        val fpr = 1 - specificity(cv.truth, prediction)
        val tpr = sensitivity(cv.truth, prediction)
        Point(
          if (fpr.isNaN) 1 else fpr,
          if (tpr.isNaN) 1 else tpr
        )
      }
    }
    lazy val qualityCharacteristics: QualityCharacteristics = {
      SmileUtils.qualityCharacteristics(cv.truth, cv.prediction, cv.posteriori.map(_(1)))
    }

    lazy val qualityCharacteristicsWithoutFn: QualityCharacteristics = {
      val (prediction, _) = predictions
        .filter { case (prediction, _) => fnCount(cv.truth, prediction) == 0 }
        .maxBy(_._2)
      SmileUtils.qualityCharacteristics(cv.truth, prediction, cv.posteriori.map(_(1)))
    }

    private def fnCount(truth: Array[Int], prediction: Array[Int]): Int = {
      truth.zip(prediction).count { case (t, p) => t > p }
    }

  }
}