package ua.nure.bieliaiev.akka

import akka.actor._

class HelloActor(myName: String) extends Actor {
  def receive: Receive = {
    case "hello" => println("hello from %s".format(myName))
    case _       => println("'huh?', said %s".format(myName))
  }
}

@main def helloActorMain(): Unit =
  val system = ActorSystem("HelloSystem")
  val helloActor = system.actorOf(Props(new HelloActor("Fred")), name = "helloactor")
  helloActor ! "hello"
  helloActor ! "buenos dias"
