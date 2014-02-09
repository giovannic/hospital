package jadeCW;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


@SuppressWarnings("serial")
public class HospitalAgent extends Agent {
	
	private int available;
	private AID[] takenSlots;
	
	@Override
	protected void setup(){
		int appts = Integer.parseInt(getArguments()[0].toString());
		available = appts;
		takenSlots = new AID[appts];
		addBehaviour(new AllocateAppointment(this));
		registerAppointmentAgent("Hospital");
	}
	
	protected void registerAppointmentAgent(String serviceName) {
		try {
	  		DFAgentDescription dfd = new DFAgentDescription();
	  		dfd.setName(getAID());
	  		ServiceDescription sd = new ServiceDescription();
	  		sd.setName(serviceName);
	  		sd.setType("allocate-appointments");
			getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL);
	  		// Agents that want to use this service need to "know" the appointment-ontology
			getContentManager().registerOntology(AppointmentOntology.getInstance());
	  		// Agents that want to use this service need to "speak" the FIPA-SL language
	  		sd.addLanguages(FIPANames.ContentLanguage.FIPA_SL);
	  		dfd.addServices(sd);
	  		
	  		DFService.register(this, dfd);
	  	}
	  	catch (FIPAException fe) {
	  		fe.printStackTrace();
	  	}
	}
	
	public void takeDown() {
		for( int i = 0; i < takenSlots.length; i++) {
			System.out.println(this.getLocalName() + ": Appointment " + (i+1) + ": " + 
					(takenSlots[i]!=null?takenSlots[i].getLocalName():"null"));
		}
	}
	
	public class AllocateAppointment extends CyclicBehaviour {

		public AllocateAppointment(Agent agent) {
			super(agent);
		}

		//note: patient appts use 1-based indexing, so always minus one on receive
		//and plus one on send
		public void action() {
			ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
			if(msg != null) {
				//System.out.println("Agent "+getLocalName()+": REQUEST message received.");
				ACLMessage reply = msg.createReply();
				reply.addReceiver(msg.getSender());
				
				if(available <= 0){
					reply.setPerformative(ACLMessage.REFUSE);
				}
				else {
					int slot = -1;
					takenSlots[--available] = msg.getSender();
					slot = available;
					/*
					try {
						ContentElement content = getContentManager().extractContent(msg);
						takenSlots[--available] = msg.getSender();
						slot = available;
						
						
					} catch (UngroundedException e) {
						e.printStackTrace();
					} catch (CodecException e) {
						e.printStackTrace();
					} catch (OntologyException e) {
						e.printStackTrace();
					}
					*/
					if(slot >= 0) {
						reply.setPerformative(ACLMessage.INFORM);
						reply.setContent(Integer.toString(++slot));
						System.out.println("inform! " + slot);
					} else {
						System.out.println("REFUSE!");
						reply.setPerformative(ACLMessage.REFUSE);
					}
				}
				send(reply);
			} else {
				block();
			}
		}	
	}
}
