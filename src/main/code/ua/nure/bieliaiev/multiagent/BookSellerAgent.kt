package ua.nure.bieliaiev.multiagent

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


class BookSellerAgent : Agent() {
    private lateinit var catalogue: Hashtable<Any, Any>
    private lateinit var myGui: BookSellerGui

    override fun setup() {
        catalogue = Hashtable<Any, Any>()
        myGui = BookSellerGui(this)
        myGui.showGui()

        val dfd: DFAgentDescription = DFAgentDescription()
        dfd.name = aid
        val sd: ServiceDescription = ServiceDescription()
        sd.type = "book-selling"
        sd.name = "JADE-book-trading"
        dfd.addServices(sd)
        try {
            DFService.register(this, dfd)
        } catch (fe: FIPAException) {
            fe.printStackTrace()
        }

        addBehaviour(OfferRequestsServer())
        addBehaviour(PurchaseOrdersServer())
    }

    override fun takeDown() {
        try {
            DFService.deregister(this)
        } catch (fe: FIPAException) {
            fe.printStackTrace()
        }
        myGui.dispose().also { println("Seller-agent ${aid.name} terminating.") }
    }

    fun updateCatalogue(title: String, price: Int) {
        addBehaviour(object : OneShotBehaviour() {
            override fun action() {
                catalogue[title] = price
                println("$title inserted into catalogue. Price = $price, seller ${this.myAgent.name}")
            }
        })
    }

    inner class OfferRequestsServer : CyclicBehaviour() {
        override fun action() {
            val messageTemplate = MessageTemplate.MatchPerformative(ACLMessage.CFP)
            myAgent.receive(messageTemplate)?.let { msg ->
                val title = msg.content
                val reply = msg.createReply()
                val price: Any? = catalogue[title]
                if (price != null) {
                    reply.performative = ACLMessage.PROPOSE
                    reply.content = price.toString()
                } else {
                    reply.content = "not-available"
                    reply.performative = ACLMessage.REFUSE
                }
                myAgent.send(reply)
            } ?: block()
        }
    }

    inner class PurchaseOrdersServer : CyclicBehaviour() {
        override fun action() {
            val mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL)
            myAgent.receive(mt)?.let { msg ->
                val title = msg.content
                val reply = msg.createReply()
                val price: Any? = catalogue[title]
                if(price != null) {
                    reply.performative = ACLMessage.INFORM
                    println("$title sold to agent ${msg.sender.name}")
                } else {
                    println("Purchase failure")
                    reply.performative = ACLMessage.FAILURE
                    reply.content = "not-available"
                }
                myAgent.send(reply)
            } ?: block()
        }
    }
}