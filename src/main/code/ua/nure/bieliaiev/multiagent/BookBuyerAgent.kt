package ua.nure.bieliaiev.multiagent

import jade.core.AID
import jade.core.Agent
import jade.core.behaviours.Behaviour
import jade.core.behaviours.TickerBehaviour
import jade.domain.DFService
import jade.domain.FIPAAgentManagement.DFAgentDescription
import jade.domain.FIPAAgentManagement.ServiceDescription
import jade.domain.FIPAException
import jade.lang.acl.ACLMessage
import jade.lang.acl.MessageTemplate

private const val BOOK_TRADE_CONVERSATION_ID = "book-trade"

class BookBuyerAgent : Agent() {
    private lateinit var targetBookTitle: String
    private var sellerAgents: MutableList<AID> = mutableListOf()

    override fun setup() {
        println("Hello!  Buyer-agent ${aid.name} is ready.")

        if (arguments.isNullOrEmpty()) {
            println("No book title specified")
            doDelete()
            return
        }
        targetBookTitle = arguments.first() as String
        println("Trying to buy $targetBookTitle")
        addBehaviour(object : TickerBehaviour(this, 60000) {
            override fun onTick() {
                val template: DFAgentDescription = DFAgentDescription()
                val sd: ServiceDescription = ServiceDescription()
                sd.type = "book-selling"
                template.addServices(sd)
                try {
                    val result: Array<DFAgentDescription> = DFService.search(myAgent, template)
                    sellerAgents = mutableListOf()
                    result.forEach { sellerAgents.add(it.name) }
                } catch (fe: FIPAException) {
                    fe.printStackTrace()
                }

                myAgent.addBehaviour(RequestPerformer())
            }
        })
    }

    override fun takeDown() {
        println("Buyer-agent ${aid.name} terminating.")
    }

    inner class RequestPerformer : Behaviour() {

        private var bestSeller: AID? = null
        private var mt: MessageTemplate? = null
        private var bestPrice: Int = 0
        private var repliesCnt: Int = 0
        private var step: Int = 0

        override fun action() {
            when (step) {
                0 -> {
                    val cfp = ACLMessage(ACLMessage.CFP)
                    println(sellerAgents)
                    sellerAgents.forEach { cfp.addReceiver(it) }
                    cfp.content = targetBookTitle
                    cfp.replyWith = "cfp ${System.currentTimeMillis()}"
                    cfp.conversationId = BOOK_TRADE_CONVERSATION_ID
                    myAgent.send(cfp)
                    mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId(BOOK_TRADE_CONVERSATION_ID),
                        MessageTemplate.MatchInReplyTo(cfp.replyWith)
                    )
                    step = 1
                }
                1 -> {
                    myAgent.receive(mt)?.let { reply ->
                        if (reply.performative == ACLMessage.PROPOSE) {
                            val price = reply.content.toInt()
                            if (bestSeller == null || price < bestPrice) {
                                bestPrice = price
                                bestSeller = reply.sender
                            }
                        }
                        repliesCnt++
                        if (repliesCnt >= sellerAgents.size) {
                            step = 2
                        }
                    } ?: block()
                }
                2 -> {
                    val order: ACLMessage = ACLMessage(ACLMessage.ACCEPT_PROPOSAL)
                    order.addReceiver(bestSeller)
                    order.content = targetBookTitle
                    order.conversationId = BOOK_TRADE_CONVERSATION_ID
                    order.replyWith = "order ${System.currentTimeMillis()}"
                    myAgent.send(order)

                    mt = MessageTemplate.and(
                        MessageTemplate.MatchConversationId(BOOK_TRADE_CONVERSATION_ID),
                        MessageTemplate.MatchInReplyTo(order.replyWith)
                    )
                    step = 3
                }
                3 -> {
                    myAgent.receive(mt)?.let { reply ->
                        if (reply.performative == ACLMessage.INFORM) {
                            println("$targetBookTitle successfully purchased.")
                            println("Price = $bestPrice, seller ${bestSeller?.name}")
                            myAgent.doDelete()
                        }
                        step = 4
                    } ?: block()
                }
                else -> Unit
            }
        }

        override fun done(): Boolean = (step == 2 && bestSeller == null) || step == 4
    }
}
