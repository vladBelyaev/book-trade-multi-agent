package ua.nure.bieliaiev.multiagent.wumpus

import aima.core.environment.wumpusworld.AgentPosition
import aima.core.environment.wumpusworld.EfficientHybridWumpusAgent
import aima.core.environment.wumpusworld.Room
import aima.core.environment.wumpusworld.WumpusAction
import aima.core.environment.wumpusworld.WumpusPercept
import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.core.behaviours.OneShotBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.domain.FIPAException
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate

class NavigatorAgent : Agent() {

    private var wumpusAgent: EfficientHybridWumpusAgent? = null

    override fun setup() {
        val dfd: DFAgentDescription = DFAgentDescription()
        dfd.name = aid
        val sd: ServiceDescription = ServiceDescription()
        sd.type = "navigator"
        sd.name = "navigator-wumpus"
        dfd.addServices(sd)
        try {
            DFService.register(this, dfd)
        } catch (fe: FIPAException) {
            fe.printStackTrace()
        }
        addBehaviour(EmailBehaviour())
    }

    private inner class EmailBehaviour : CyclicBehaviour() {
        override fun action() {
            val messageTemplate = MessageTemplate.MatchPerformative(ACLMessage.REQUEST)
            addBehaviour(NavigateSpeliologBehaviour(myAgent.receive(messageTemplate)))
        }
    }

    private inner class NavigateSpeliologBehaviour(
        private val aclMessage: ACLMessage?
    ) : OneShotBehaviour() {
        override fun action() {
            aclMessage?.let { msg ->
                if (wumpusAgent == null) {
                    wumpusAgent = EfficientHybridWumpusAgent(
                        4,
                        4,
                        AgentPosition(Room(1, 1), AgentPosition.Orientation.FACING_NORTH)
                    )
                }
                val requestMessageContent = msg.content
                val matcher = Regex("stench|breeze|glitter|bump|scream")
                    .toPattern().matcher(requestMessageContent)
                val wumpusPercept = WumpusPercept()
                while (matcher.find()) {
                    when (matcher.group()) {
                        "stench" -> wumpusPercept.setStench()
                        "breeze" -> wumpusPercept.setBreeze()
                        "glitter" -> wumpusPercept.setGlitter()
                        "bump" -> wumpusPercept.setBump()
                        "scream" -> wumpusPercept.setScream()
                    }
                }
                val actOptional = wumpusAgent!!.act(wumpusPercept)
                val reply = msg.createReply()
                if (actOptional.isPresent) {
                    reply.performative = ACLMessage.PROPOSE
                    val act: WumpusAction = actOptional.get()
                    println("Navigator ${aid.name} propose $act for speliolog ${msg.sender.name}")
                    reply.content = ActionEnum.fromWumpusAction(act).key
                } else {
                    reply.performative = ACLMessage.FAILURE
                    reply.content = "IDK where to go, so you will die:("
                }
                myAgent.send(reply)

                actOptional.ifPresent {
                    if (actOptional.get() == WumpusAction.CLIMB) myAgent.doDelete()
                }
            } ?: block()
        }
    }
}