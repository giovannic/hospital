package jadeCW;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.abs.AbsContentElement;
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

	//Constants
	public static final String NEW_APP = "NEW_APP";
	public static final String FIND_OWNER = "FIND_OWNER";
	
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
			getContentManager().registerOntology(AppointmentOntology.getInstance(), AppointmentOntology.NAME);
	  		// Agents that want to use this service need to "speak" the FIPA-SL language
	  		sd.addLanguages(FIPANames.ContentLanguage.FIPA_SL);
	  		sd.addOntologies(AppointmentOntology.NAME);
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
				reply.setPerformative(ACLMessage.REFUSE);
				ContentElement content;
				try {
					content = getContentManager().extractContent(msg);
					Concept action = ((Action)content).getAction();
					if( action instanceof AssignAppointment ) {
						System.out.println("Looking for appointment to assign " +
								"to " + msg.getSender().getLocalName());
						if(available <= 0){
							reply.setPerformative(ACLMessage.REFUSE);
							System.out.println("No appointment available for " +
								msg.getSender().getLocalName());
						}
						else {
							int slot = -1;
							takenSlots[--available] = msg.getSender();
							slot = available;
							if(slot >= 0) {
								reply.setPerformative(ACLMessage.INFORM);
								Available av = new Available();
								Appointment appt = new Appointment();
								appt.setNumber(++slot);
								av.setAppointment(appt);
								getContentManager().fillContent(reply, av);
								System.out.println("Appointment " + slot + " assigned to " + msg.getSender().getLocalName());
							} else {
								System.out.println("No appointment available for " +
										msg.getSender().getLocalName());
								reply.setPerformative(ACLMessage.REFUSE);
							}
						}
					} else if( action instanceof FindOwner) {
						
					}
					send(reply);
				} catch (UngroundedException e) {
					e.printStackTrace();
					block();
				} catch (CodecException e) {
					e.printStackTrace();
					block();
				} catch (OntologyException e) {
					e.printStackTrace();
					block();
				}
			} else {
				block();
			}
		}	
	}
}
