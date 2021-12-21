package ua.nure.bieliaiev.akka

import akka.actor.typed.{ActorRef, ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors

object HelloActorNewApi {
  def apply(myName: String): Behavior[String] = Behaviors.receive { (context, message) =>
    message match {
      case "hello" => println("hello from %s".format(myName))
      case _ => println("'huh?', said %s".format(myName))
    }
    Behaviors.same
  }
}

@main def helloActorNewApi(): Unit =
  val helloActor: ActorSystem[String] = ActorSystem(HelloActorNewApi("Fred"), "helloactor")
  helloActor ! "hello"
  helloActor ! "buenos dias"
