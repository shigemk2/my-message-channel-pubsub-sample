package com.example

import java.math.BigDecimal

import akka.actor.{Actor, ActorRef, ActorSystem}
import java.math.RoundingMode

import akka.event.{EventBus, SubchannelClassification}
import akka.util.Subclassification

class CompletableApp(val steps:Int) extends App {
  val canComplete = new java.util.concurrent.CountDownLatch(1);
  val canStart = new java.util.concurrent.CountDownLatch(1);
  val completion = new java.util.concurrent.CountDownLatch(steps);

  val system = ActorSystem("eaipatterns")

  def awaitCanCompleteNow = canComplete.await

  def awaitCanStartNow = canStart.await

  def awaitCompletion = {
    completion.await
    system.shutdown()
  }

  def canCompleteNow() = canComplete.countDown()

  def canStartNow() = canStart.countDown()

  def completeAll() = {
    while (completion.getCount > 0) {
      completion.countDown()
    }
  }

  def completedStep() = completion.countDown()
}

object SubClassificationDriver extends CompletableApp(6) {

}

case class Money(amount: BigDecimal) {
  def this(amount: String) = this(new java.math.BigDecimal(amount))

  amount.setScale(4, BigDecimal.ROUND_HALF_UP)
}

case class Market(name: String)

case class PriceQuoted(market: Market, ticker: Symbol, price: Money)

class QuotesEventBus extends EventBus with SubchannelClassification {
  type Classifier = Market
  type Event = PriceQuoted
  type Subscriber = ActorRef

  protected def classify(event: Event): Classifier = {
    event.market
  }

  protected def publish(event: Event, subscriber: Subscriber): Unit = {
    subscriber ! event
  }

  protected def subclassification = new Subclassification[Classifier] {
    def isEqual(
                 subscribedToClassifier: Classifier,
                 eventClassifier: Classifier): Boolean = {
      subscribedToClassifier.equals(eventClassifier)
    }

    def isSubclass(
                    subscribedToClassifier: Classifier,
                    eventClassifier: Classifier): Boolean = {
      subscribedToClassifier.name.startsWith(eventClassifier.name)
    }
  }
}

class AllMarketsSubscriber extends Actor {
  def receive = {
    case quote: PriceQuoted =>
      println(s"AllMarketsSubscriber received: $quote")
      SubClassificationDriver.completedStep()
  }
}

class NASDAQSubscriber extends Actor {
  def receive = {
    case quote: PriceQuoted =>
      println(s"NASDAQSubscriber received: $quote")
      SubClassificationDriver.completedStep()
  }
}

class NYSESubscriber extends Actor {
  def receive = {
    case quote: PriceQuoted =>
      println(s"NYSESubscriber received: $quote")
      SubClassificationDriver.completedStep()
  }
}

