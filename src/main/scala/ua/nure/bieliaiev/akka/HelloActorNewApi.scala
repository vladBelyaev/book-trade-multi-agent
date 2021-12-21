package ua.nure.bieliaiev.akka

import akka.actor.typed.{ActorRef, ActorSystem, Behavior, PostStop}
import akka.actor.typed.scaladsl.Behaviors

object HelloActorNewApi {
  var isStopped: Boolean = false
  def apply(myName: String): Behavior[String] = Behaviors.receive { (context, message) =>
    message match {
      case "hello" => println("hello from %s".format(myName))
      case "bye" =>
        println("bye from %s".format(myName))
        isStopped = true
      case _ => println("'huh?', said %s".format(myName))
    }
    if (isStopped) Behaviors.stopped
    else Behaviors.same
  }
}

@main def helloActorNewApiMain(): Unit =
  val helloActor: ActorSystem[String] = ActorSystem(HelloActorNewApi("Fred"), "helloactor")
  helloActor ! "hello"
  helloActor ! "buenos dias"
  helloActor ! "bye"
  Thread.sleep(3000)
  helloActor ! "hello"
