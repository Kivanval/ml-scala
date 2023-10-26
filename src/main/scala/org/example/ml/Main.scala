package org.example.ml

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.cibo.evilplot.colors.RGB
import com.cibo.evilplot.geometry.LineStyle
import com.cibo.evilplot.plot._
import com.cibo.evilplot.plot.aesthetics.DefaultTheme._
import org.example.ml.Implicits._
import smile.classification._
import smile.data.DataFrame
import smile.data.`type`.{DataTypes, StructField}
import smile.data.formula._
import smile.validation.ClassificationValidation
import smile.validation.validate.classification

import java.io.File
import java.util
import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import scala.util.Random

object Main {
  def main(args: Array[String]): Unit = {
    val fields = new util.ArrayList[StructField](
      (new StructField("Activity", DataTypes.ByteType) +:
        (for {number <- 1 to 1776} yield new StructField(s"D$number", DataTypes.DoubleType)))
        .asJava)
    val dataFrameFromCsvWithSchema = SmileUtils.fromResourceCsv(schema = DataTypes.struct(fields)) _
    val workflow = for {
      data <- dataFrameFromCsvWithSchema("bioresponse.csv")
      (trainData, testData) = Random.shuffle(data.toList.asScala).splitAt(Math.round(0.8F * data.size()))
      decisionTree = classification("Activity" ~,
        DataFrame.of(trainData.asJava), DataFrame.of(testData.asJava)) {
        (formula, trainData) => cart(formula, trainData, maxDepth = 5)
      }
      _ <- IO.delay(println(s"Decision Tree ${decisionTree.qualityCharacteristics}"))
      _ <- IO.delay(println(s"Decision Tree Without False Negative ${decisionTree.qualityCharacteristicsWithoutFn}"))
      deepDecisionTree = classification("Activity" ~,
        DataFrame.of(trainData.asJava), DataFrame.of(testData.asJava)) {
        (formula, trainData) => cart(formula, trainData, maxDepth = 50)
      }
      _ <- IO.delay(println(s"Deep Decision Tree ${deepDecisionTree.qualityCharacteristics}"))
      randForest = classification("Activity" ~,
        DataFrame.of(trainData.asJava), DataFrame.of(testData.asJava)) {
        (formula, trainData) => randomForest(formula, trainData, maxDepth = 5, ntrees = 100)
      }
      _ <- IO.delay(println(s"Random Forest ${randForest.qualityCharacteristics}"))
      deepRandForest = classification("Activity" ~,
        DataFrame.of(trainData.asJava), DataFrame.of(testData.asJava)) {
        (formula, trainData) => randomForest(formula, trainData, maxDepth = 50, ntrees = 100)
      }
      _ <- IO.delay(println(s"Deep Random Forest ${deepRandForest.qualityCharacteristics}"))
      _ <- plot(decisionTree, deepDecisionTree, randForest, deepRandForest)("plot.png")
    } yield ()

    workflow.unsafeRunSync()
  }

  private def plot(
                    decisionTree: ClassificationValidation[DecisionTree],
                    deepDecisionTree: ClassificationValidation[DecisionTree],
                    randForest: ClassificationValidation[RandomForest],
                    deepRandForest: ClassificationValidation[RandomForest],
                  )(fileName: String): IO[Unit] = {
    IO.delay {
      Facets(
        Seq(Seq(
          Overlay(
            LinePlot.series(decisionTree.prc, name = f"Decision Tree (AP = ${decisionTree.metrics.precision}%.2f)", color = RGB(19, 152, 32)),
            LinePlot.series(deepDecisionTree.prc, name = f"Deep Decision Tree (AP = ${deepDecisionTree.metrics.precision}%.2f)", color = RGB(255, 128, 0)),
            LinePlot.series(randForest.prc, name = f"Random Forest (AP = ${randForest.metrics.precision}%.2f)", color = RGB(102, 0, 204)),
            LinePlot.series(deepRandForest.prc, name = f"Deep Random Forest (AP = ${deepRandForest.metrics.precision}%.2f)", color = RGB(15, 15, 15))
          ).title("Precision-Recall Curve")
            .overlayLegend(0.97, 0.98)
            .standard()
            .xbounds(0, 1)
            .ybounds(0.4, 1)
            .hline(0.5, color = RGB(45, 45, 45), lineStyle = LineStyle.DashDot)
            .xLabel("Recall")
            .yLabel("Precision"),
          Overlay(
            LinePlot.series(decisionTree.roc, name = f"Decision Tree (AUC = ${decisionTree.metrics.auc}%.2f)", color = RGB(19, 152, 32)),
            LinePlot.series(deepDecisionTree.roc, name = f"Deep Decision Tree (AUC = ${deepDecisionTree.metrics.auc}%.2f)", color = RGB(255, 128, 0)),
            LinePlot.series(randForest.roc, name = f"Random Forest (AUC = ${randForest.metrics.auc}%.2f)", color = RGB(102, 0, 204)),
            LinePlot.series(deepRandForest.roc, name = f"Deep Random Forest (AUC = ${deepRandForest.metrics.auc}%.2f)", color = RGB(15, 15, 15))
          ).title("RoC Curve")
            .overlayLegend(0.97, 0.98)
            .standard()
            .xbounds(0, 1)
            .ybounds(0, 1)
            .xLabel("False Negative Rate")
            .yLabel("True Positive Rate")
            .trend(1, 0, color = RGB(45, 45, 45), lineStyle = LineStyle.DashDot)
        )))
        .render()
        .write(new File(fileName))
    }
  }
}