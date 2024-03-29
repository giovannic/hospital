package jadeCW;


import java.util.HashSet;
import java.util.Set;

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


@SuppressWarnings("serial")
public class HospitalAgent extends Agent {
	
	private int available;
	private AID[] takenSlots;
	private Set<Pair> swapRequests;

	//Constants
	public static final String NEW_APP = "NEW_APP";
	public static final String FIND_OWNER = "FIND_OWNER";
	
	//message matcher
	private final AppointmentMessageMatcher messageMatcher = 
			new AppointmentMessageMatcher(this);
	
	@Override
	protected void setup(){
		int appts = Integer.parseInt(getArguments()[0].toString());
		available = appts;
		takenSlots = new AID[appts];
		swapRequests = new HashSet<Pair>();
		addBehaviour(new AllocateAppointment(this));
		addBehaviour(new RespondToQuery(this));
		addBehaviour(new RespondToProposal2(this));
		addBehaviour(new UpdateAppointments(this));
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
			//receive only relevant messages
			ACLMessage msg = receive(messageMatcher.ObtainRequest);
			
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
							--available;
							for(int i = 0; i < takenSlots.length; i++) {
								if(takenSlots[i] == null) {
									takenSlots[i] = msg.getSender();
									slot = i;
									break;
								}
							}
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
	
	
	public class RespondToQuery extends CyclicBehaviour {

		public RespondToQuery(Agent agent) {
			super(agent);
		}

		//note: patient appts use 1-based indexing, so always minus one on receive
		//and plus one on send
		public void action() {
			//receive only owner requests
			ACLMessage msg = receive(messageMatcher.OwnerRequest);
			
			if(msg != null) {
				System.out.println("message received");
				//System.out.println("Agent "+getLocalName()+": REQUEST message received.");
				ACLMessage reply = msg.createReply();
				reply.addReceiver(msg.getSender());
				try {
					ContentElement content = (ContentElement)myAgent.getContentManager().extractContent(msg);
					FindOwner action = (FindOwner) ((Action)content).getAction();
					Appointment a = action.getAppointment();
					int wanted = a.getNumber()-1;
					
					if (wanted < 0 || wanted >= takenSlots.length){
						//appointment not in range
						System.out.println("Invalid appointment: " +
								(wanted+1));
						reply.setPerformative(ACLMessage.REFUSE);
						
					}					 
					else  { // appt found
						AID agentOwner = takenSlots[wanted];
						reply.setPerformative(ACLMessage.INFORM);
						Owner owner = null;
						
						if( agentOwner == null) {
							//not assigned
							System.out.println("No patient assigned appointment: " +
									(wanted+1));
							owner = new Owner(getAID());
						}
						else{
							owner =	new Owner(agentOwner);
						}
						
						Appointment appt = new Appointment();
						appt.setNumber(wanted+1);
						IsOwned isOwned = new IsOwned(appt, owner);
						getContentManager().fillContent(reply, isOwned);
						System.out.println("Preferred appointment " + (wanted+1) + 
								" assigned to patient: " + owner.getPatient());
						System.out.println("Desired by patient: " + msg.getSender().getName());
						
					}
				} catch (UngroundedException e) {
					e.printStackTrace();
				} catch (CodecException e) {
					e.printStackTrace();
				} catch (OntologyException e) {
					e.printStackTrace();
				}
				send(reply);
			} else {
				block();
			}
			
		}	
	}
	
	
	public class RespondToProposal2 extends CyclicBehaviour {
		
		public RespondToProposal2(Agent agent) {
			super(agent);
		}
		
		@Override
		public void action() {
			ACLMessage msg = receive(messageMatcher.RequestSwap);
			
			if(msg != null) {
				
				ACLMessage reply = msg.createReply();
				reply.addReceiver(msg.getSender());

				try {
					ContentElement content = getContentManager().extractContent(msg);
					Concept action = ((Action)content).getAction();
					if(action instanceof PatientRequestSwap) {
						Appointment requested = ((PatientRequestSwap) action).getRequestedAppointment();
						if(isTaken(requested)) {
							System.err.println("WRONG!!! appointment " + requested.getNumber() + " is owned");
							reply.setPerformative(ACLMessage.REFUSE);
						} else {
							int current = ((PatientRequestSwap) action).getCurrentAppointment().getNumber();
							takenSlots[requested.getNumber()-1] = takenSlots[current-1];
							takenSlots[current-1] = null;
							reply.setPerformative(ACLMessage.INFORM);
							Owner owner = new Owner(getAID());
							IsOwned isOwned = new IsOwned(requested, owner);
							getContentManager().fillContent(reply, isOwned);
						}
						send(reply);
					}
				} catch (UngroundedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (CodecException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (OntologyException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}		
	}
	
	public class UpdateAppointments extends CyclicBehaviour {
		
		public UpdateAppointments(Agent agent){
			super(agent); 
		}

		@Override
		public void action() {
			//receive only owner requests
			ACLMessage msg = receive(messageMatcher.SwapInform);
			
			if(msg != null) {
				try {
					HospitalSwapInform content = (HospitalSwapInform)myAgent.getContentManager().extractContent(msg);
					
					// Get appointments to be swapped
					int current = content.getCurrentlyOwned().getNumber();
					int requested = content.getNewAppointment().getNumber();
					Pair pair = new Pair(current, requested);
					System.out.println("Hospital receieved swap request for apps: " + current + " " + requested + "from " + msg.getSender().getLocalName());

					// Check if other agent in swap has already informed
					if(swapRequests.contains(pair)){
						// If so, remove from swapRequests and swap values in takeSlots
						swapRequests.remove(pair);
						AID temp = takenSlots[current-1];
						takenSlots[current-1] = takenSlots[requested-1];
						takenSlots[requested-1] = temp;
						System.out.println("Hospital - " + current + " and " + requested + " swapped for " + msg.getSender().getLocalName());
					}
					else{
						// else add to swapRequests and wait for message from other agent in swap
						System.out.println("Hospital adding pair: " + current + " " + requested);
						swapRequests.add(pair);
					}
					
					
				} catch (UngroundedException e) {
					e.printStackTrace();
				} catch (CodecException e) {
					e.printStackTrace();
				} catch (OntologyException e) {
					e.printStackTrace();
				}
				
			} else {
				block();
			}
			
		}
		
	}

	public boolean isTaken(Appointment requested) {
		int toCompare = requested.getNumber()-1;
		return takenSlots[toCompare] != null;
	}
	
}
