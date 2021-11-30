package ua.nure.bieliaiev.multiagent.wumpus

import aima.core.environment.wumpusworld.AgentPosition
import aima.core.environment.wumpusworld.WumpusAction
import aima.core.environment.wumpusworld.WumpusCave
import aima.core.environment.wumpusworld.WumpusEnvironment
import aima.core.environment.wumpusworld.WumpusPercept
import jade.core.AID
import jade.core.Agent
import jade.core.behaviours.CyclicBehaviour
import jade.core.behaviours.OneShotBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.domain.FIPAException
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate
import java.util.*
import aima.core.agent.Agent as AimaAgent

class CaveAgent : Agent() {

    private var agentWithGold: JadeWumpusAgent<WumpusPercept, WumpusAction>? = null
    private val speliologsInCave = mutableListOf<JadeWumpusAgent<WumpusPercept, WumpusAction>>()
    private val wumpusEnvironment = WumpusEnvironment(
        WumpusCave(
            4,
            4,
            ""
                + ". . W G "
                + ". . ." +
                " P "
                + ". . . . "
                + "S . . . "
        )
    )

    override fun setup() {
        val dfd: DFAgentDescription = DFAgentDescription()
        dfd.name = aid
        val sd: ServiceDescription = ServiceDescription()
        sd.type = "cave"
        sd.name = "cave-wumpus"
        dfd.addServices(sd)
        try {
            DFService.register(this, dfd)
        } catch (fe: FIPAException) {
            fe.printStackTrace()
        }

        println("Cave setup ${aid.name}")
        addBehaviour(AddWumpusAgentBehaviour())
        addBehaviour(EmailBehaviour())
    }

    private inner class AddWumpusAgentBehaviour : CyclicBehaviour() {
        override fun action() {
            val messageTemplate = MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE)
            myAgent.receive(messageTemplate)?.let { msg ->
                val agentAID = msg.sender
                val speliologCaveAgent = JadeWumpusAgent<WumpusPercept, WumpusAction>(agentAID)
                wumpusEnvironment.addAgent(speliologCaveAgent)
                speliologsInCave.add(speliologCaveAgent)
                val reply = msg.createReply()
                reply.content = "Agent ${agentAID.name} entered the cave ${aid.name}"
                reply.performative = ACLMessage.INFORM
                myAgent.send(reply)
            } ?: block()
        }
    }

    private inner class EmailBehaviour : CyclicBehaviour() {
        override fun action() {
            val messageTemplate = MessageTemplate.MatchExpression { msg ->
                speliologsInCave.any { it.speliologAID == msg.sender } &&
                    (msg.performative == ACLMessage.CFP || msg.performative == ACLMessage.REQUEST)
            }
            myAgent.receive(MessageTemplate(messageTemplate))?.let { msg ->
                when (msg.performative) {
                    ACLMessage.REQUEST -> addBehaviour(SendAgentPositionBehaviour(msg))
                    ACLMessage.CFP -> addBehaviour(ChangeAgentPositionBehaviour(msg))
                }
            } ?: block()
        }
    }

    private inner class SendAgentPositionBehaviour(
        private val msg: ACLMessage
    ) : OneShotBehaviour() {
        override fun action() {
            val agentAID = msg.sender
            val speliolog = findSpeliolog(agentAID)
            val reply = msg.createReply()
            reply.content = wumpusEnvironment.getPerceptSeenBy(speliolog).toAgentPredicate()
            reply.performative = ACLMessage.INFORM_REF
            myAgent.send(reply)
        }
    }

    private fun findSpeliolog(agentAID: AID) = speliologsInCave
        .first { it.speliologAID == agentAID }

    private fun WumpusPercept.toAgentPredicate(): String {
        val percepts = mutableListOf<String>()
        if (isBreeze) percepts.add("breeze")
        if (isBump) percepts.add("bump")
        if (isGlitter) percepts.add("glitter")
        if (isScream) percepts.add("scream")
        if (isStench) percepts.add("stench")
        return "Percept(${percepts.joinToString(separator = ",", prefix = "[", postfix = "]")})"
    }

    private fun AgentPosition.toAgentPredicate(): String {
        return "Position($this)"
    }

    private inner class ChangeAgentPositionBehaviour(
        private val msg: ACLMessage
    ) : OneShotBehaviour() {
        override fun action() {
            val agentAID = msg.sender
            val speliolog = findSpeliolog(agentAID)

            val reply = msg.createReply()

            val actionEnum = ActionEnum.fromKey(msg.content)
            wumpusEnvironment.execute(speliolog, actionEnum!!.action)
            println("Agent ${speliolog.speliologAID.name} position after action {${wumpusEnvironment.getAgentPosition(speliolog)}")

            reply.performative = ACLMessage.ACCEPT_PROPOSAL
            myAgent.send(reply)
            if (actionEnum.action == WumpusAction.GRAB && wumpusEnvironment.isGoalGrabbed) {
                agentWithGold = speliolog
                println("Agent ${speliolog.speliologAID.name} grab gold")
            }

            if (actionEnum.action == WumpusAction.CLIMB) {
                if (speliolog == agentWithGold) {
                    println("Agent ${speliolog.speliologAID.name} climb with gold")
                    myAgent.doDelete()
                } else {
                    println("Agent ${speliolog.speliologAID.name} run away.")
                }
            }
        }
    }
}

class JadeWumpusAgent<T : WumpusPercept, Z : WumpusAction>(
    val speliologAID: AID
) : AimaAgent<T, Z> {
    private var alive: Boolean = true

    override fun act(p0: T): Optional<Z> = Optional.empty()

    override fun isAlive(): Boolean = alive

    override fun setAlive(p0: Boolean) {
        alive = p0
    }
}