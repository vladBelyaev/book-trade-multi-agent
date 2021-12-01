package ua.nure.bieliaiev.multiagent.wumpus

import jade.core.AID
import jade.core.Agent
import jade.core.behaviours.Behaviour
import jade.core.behaviours.OneShotBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.domain.FIPAException
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate

private const val SPELEOLOGIST_CAVE_CONVERSATION_ID = "speleologist-cave"
private const val SPELEOLOGIST_CAVE_DO_ACTION_CONVERSATION_ID = "speleologist-cave-action"
private const val SPELEOLOGIST_NAVIGATOR_REQUEST_CONVERSATION_ID = "speleologist-navigator-request"

class SpeleologistAgent : Agent() {

    private lateinit var caveAgent: AID
    private lateinit var navigatorAgent: AID

    private val perceptsMap = mapOf(
        "scream" to listOf("I hear scream here", "There is a scream", "It’s a awful scream here"),
        "bump" to listOf("I feel bump here", "There is a bump", "It’s a cool bump here"),
        "glitter" to listOf("I see glitter here", "There is a glitter", "It’s a dazzling glitter here"),
        "breeze" to listOf("I feel breeze here", "There is a breeze", "It’s a cool breeze here"),
        "stench" to listOf("I feel stench here", "There is a stench", "It’s a cool stench here")
    )

    override fun setup() {
        println("Hello! speleologistAgent ${aid.name} is ready.")
        if (!setCaveFromDF() || !setNavigatorFromDF()) {
            return
        }
        addBehaviour(object : OneShotBehaviour() {
            override fun action() {
                val subscribeToCave = ACLMessage(ACLMessage.SUBSCRIBE)
                subscribeToCave.addReceiver(caveAgent)
                subscribeToCave.content = "Try to enter cave"
                subscribeToCave.replyWith = "enter cave ${System.currentTimeMillis()} from speleologist ${aid.name}"
                subscribeToCave.conversationId = SPELEOLOGIST_CAVE_CONVERSATION_ID
                myAgent.send(subscribeToCave)
                val mt = MessageTemplate.and(
                    MessageTemplate.MatchConversationId(SPELEOLOGIST_CAVE_CONVERSATION_ID),
                    MessageTemplate.MatchInReplyTo(subscribeToCave.replyWith)
                )
                addBehaviour(RequestPerformer(mt))
            }
        })
    }

    override fun takeDown() {
        println("speleologistAgent ${aid.name} terminating.")
    }

    private fun setNavigatorFromDF(): Boolean {
        return arguments.firstOrNull()?.let { navName ->
            val template = DFAgentDescription()
            val sd = ServiceDescription()
            sd.type = "navigator"
            template.addServices(sd)
            try {
                val result: DFAgentDescription? = DFService.search(this, template)
                    .firstOrNull { it.name == navName }
                if (result != null) {
                    navigatorAgent = result.name
                    println("Set for speleologist ${aid.name} navigator ${navigatorAgent.name}")
                    return true
                }
            } catch (fe: FIPAException) {
                fe.printStackTrace()
            }
            println("No navigators for speleologist")
            this.doDelete()
            false
        } ?: false
    }

    private fun setCaveFromDF() =
        when (val caveAgent = findServiceFromDF("cave")) {
            null -> {
                println("No caves for speleologist")
                this.doDelete()
                false
            }
            else -> {
                println("Set for speleologist ${aid.name} cave ${caveAgent.name.name}")
                this.caveAgent = caveAgent.name
                true
            }
        }

    private fun findServiceFromDF(type: String): DFAgentDescription? {
        val template = DFAgentDescription()
        val sd = ServiceDescription()
        sd.type = type
        template.addServices(sd)
        try {
            val result: Array<DFAgentDescription> = DFService.search(this, template)
            return when (result.size) {
                0 -> null
                1 -> result.first()
                else -> {
                    result.shuffle()
                    result.first()
                }
            }
        } catch (fe: FIPAException) {
            fe.printStackTrace()
        }
        return null
    }

    inner class RequestPerformer(
        private var mt: MessageTemplate
    ) : Behaviour() {

        private var step: Int = 0
        private var navigatorMessage: String = ""
        private var isFinished = false
        private lateinit var lastActionEnum: ActionEnum

        override fun action() {
            when (step) {
                0 -> {
                    myAgent.receive(mt)?.let { reply ->
                        println(reply.content)
                        step = 1
                    } ?: block()
                }
                1 -> {
                    val request = ACLMessage(ACLMessage.REQUEST)
                    request.addReceiver(caveAgent)
                    request.replyWith = "cave request ${System.currentTimeMillis()} from speleologist ${aid.name}"
                    request.conversationId = SPELEOLOGIST_CAVE_CONVERSATION_ID
                    myAgent.send(request)
                    mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId(SPELEOLOGIST_CAVE_CONVERSATION_ID),
                        MessageTemplate.MatchInReplyTo(request.replyWith)
                    )
                    step = 2
                }
                2 -> {
                    myAgent.receive(mt)?.let { reply ->
                        println("Speleologist ${aid.name} state ${reply.content} from cave")
                        val percepts = reply.content.substringAfter("[")
                            .substringBefore("]")
                            .split(",")
                            .filter { it.isNotBlank() }
                        navigatorMessage = percepts.joinToString(
                            separator = ".",
                            postfix = "."
                        ) { percept -> perceptsMap[percept]!![(Math.random() * 3).toInt()] }
                        step = 3
                    } ?: block()
                }
                3 -> {
                    val request = ACLMessage(ACLMessage.REQUEST)
                    request.addReceiver(navigatorAgent)
                    request.replyWith = "navigator request ${System.currentTimeMillis()} from speleologist ${aid.name}"
                    request.conversationId = SPELEOLOGIST_NAVIGATOR_REQUEST_CONVERSATION_ID
                    request.content = navigatorMessage
                    myAgent.send(request)
                    mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId(SPELEOLOGIST_NAVIGATOR_REQUEST_CONVERSATION_ID),
                        MessageTemplate.MatchInReplyTo(request.replyWith)
                    )
                    step = 4
                }
                4 -> {
                    myAgent.receive(mt)?.let { reply ->
                        ActionEnum.fromKey(reply.content)?.let {
                            lastActionEnum = it
                            println("Response from navigator {${reply.content}} for agent ${aid.name}")

                            val cfp = ACLMessage(ACLMessage.CFP)
                            cfp.addReceiver(caveAgent)
                            cfp.replyWith =
                                "change state in cave for speleologist ${aid.name}. ${System.currentTimeMillis()}"
                            cfp.conversationId = SPELEOLOGIST_CAVE_DO_ACTION_CONVERSATION_ID
                            cfp.content = it.key
                            myAgent.send(cfp)
                            mt = MessageTemplate.and(
                                MessageTemplate.MatchConversationId(SPELEOLOGIST_CAVE_DO_ACTION_CONVERSATION_ID),
                                MessageTemplate.MatchInReplyTo(cfp.replyWith)
                            )
                            step = 5
                        }
                        if (ActionEnum.fromKey(reply.content) == null) {
                            println(reply.content)
                            myAgent.doDelete()
                        }
                    } ?: block()
                }
                5 -> {
                    myAgent.receive(mt)?.let { reply ->
                        if (reply.performative == ACLMessage.ACCEPT_PROPOSAL && lastActionEnum == ActionEnum.CLIMB) {
                            println("Agent ${aid.name} away from cave")
                            isFinished = true
                            myAgent.doDelete()
                        } else step = 1
                    } ?: block()
                }
                else -> Unit
            }
        }

        override fun done(): Boolean = isFinished
    }
}
