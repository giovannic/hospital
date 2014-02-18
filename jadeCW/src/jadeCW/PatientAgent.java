package jadeCW;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.Predicate;
import jade.content.lang.Codec.CodecException;
import jade.content.lang.sl.SLCodec;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.ServiceException;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.messaging.TopicManagementHelper;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.SimpleAchieveREInitiator;
import jade.proto.SubscriptionInitiator;

@SuppressWarnings("serial")
public class PatientAgent extends Agent {

	
	AID provider;
	Appointment allocation;
	Preferences preferences;
	AID agentWithPreferred = null;
	Appointment preferredApp;
	private List<ACLMessage> pendingRequests = new LinkedList<ACLMessage>();
	
	//topics
	AID obtainRequest;
	AID ownerRequest;
	
	private final AppointmentMessageMatcher messageMatcher = 
			new AppointmentMessageMatcher(this);
	public boolean responded = true;
	private Set<Integer> best;
	
	
	protected void setup() {
		preferences = Preferences.parsePreferences(getArguments()[0].toString());
		getContentManager().registerOntology(AppointmentOntology.getInstance(), AppointmentOntology.NAME);
		getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL);
		subscribeToAppointments();
		best = preferences.getBestPreferences();
		Integer[] a = new Integer[best.size()];
		Integer[] bestArr = (Integer[]) best.toArray(a);
		preferredApp = new Appointment();
		preferredApp.setNumber(bestArr[0]);
	}
	
	private void subscribeToAppointments() {
		// Build the description used as template for the subscription
		DFAgentDescription template = new DFAgentDescription();
		ServiceDescription templateSd = new ServiceDescription();
		templateSd.setType("allocate-appointments");
		template.addServices(templateSd);

		SearchConstraints sc = new SearchConstraints();
		// We want to receive 10 results at most
		sc.setMaxResults(new Long(10));

		addBehaviour(new SubscriptionInitiator(this,
				DFService.createSubscriptionMessage(this, getDefaultDF(),
						template, sc)) {
			protected void handleInform(ACLMessage inform) {

				System.out.println("Agent " + getLocalName()
						+ ": Notification received from DF");
				try {
					DFAgentDescription[] results = DFService
							.decodeNotification(inform.getContent());
					if (results.length != 1) {
						System.out.print("There should only be one hospital");
					} else {
						setProvider(results[0].getName());
						myAgent.addBehaviour(new RequestAppointment(myAgent));
						System.out.println("found hospital");
					}
				} catch (FIPAException fe) {
					fe.printStackTrace();
				}
			}
		});
		addBehaviour(new RespondToProposal1(this));
	}
	
	/*
	 * store the hospital's id for later requests
	 */
	private void setProvider(AID provider){
		this.provider = provider;
	}
	
	protected void takeDown() {
		System.out.println(getLocalName() +": " + 
				(allocation != null ? "Appointment " + allocation.getNumber() : "null"));
	}

	public class RequestAppointment extends Behaviour {

		ACLMessage requestMsg;
		private boolean finished = false;
		
		public RequestAppointment(Agent patientAgent) {
			super(patientAgent);
			// Create an ACL message for hospital
			requestMsg = new ACLMessage(ACLMessage.REQUEST);
			requestMsg.addReceiver(provider);
			requestMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			requestMsg.setOntology(AppointmentOntology.NAME);
			requestMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
			
		}



		@Override
		public void action() {
			
			//only request once
			if(allocation != null){
				return;
			}
			//if there is no provider, return
			if(provider == null){
				return;
			}
			
			AssignAppointment act = new AssignAppointment();
						
			try {
				getContentManager().fillContent(requestMsg, new Action(provider, act));
			} catch (Exception pe) {
				pe.printStackTrace();
			}
			
			//add response behaviour
			addBehaviour(new SimpleAchieveREInitiator(myAgent, requestMsg) {
				protected void handleInform(ACLMessage msg) {
					System.out.println("Engagement successfully completed");
					try {
						Available content = (Available) getContentManager().extractContent(msg);
						allocation = content.getAppointment();
						myAgent.addBehaviour(new FindAppointmentOwner(myAgent));
						return;
					} catch (UngroundedException e) {
						e.printStackTrace();
					} catch (CodecException e) {
						e.printStackTrace();
					} catch (OntologyException e) {
						e.printStackTrace();
					}
					//retry if error occurs
					finished = false;
				}
				protected void handleRefuse(ACLMessage msg) {
					System.out.println("Engagement refused");
					
				}
			});
			
			this.finished = true;
		}
		
		

		@Override
		public boolean done() {
			return this.finished ;
		}
	
	}
	
	public class FindAppointmentOwner extends Behaviour {

		ACLMessage requestMsg;
		private boolean finished = false;
		
		public FindAppointmentOwner(Agent patientAgent) {
			super(patientAgent);
			// Create an ACL message for hospital
			requestMsg = new ACLMessage(ACLMessage.REQUEST);
			requestMsg.addReceiver(provider);
			requestMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			requestMsg.setOntology(AppointmentOntology.NAME);
			requestMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
			System.out.println("find appt owner constructed");
		}



		@Override
		public void action() {
			System.out.println("beginning to find appt owner");
			//only request if appointment is allocated
			if(allocation == null){
				System.out.println("allocation null");
				return;
			}
			//if there is no provider, return
			if(provider == null){
				return;
			}
			
			Integer appointment = allocation.getNumber();
			
			if(best.contains(appointment)) {
				
				this.finished = true;
				//already have best, do nothing
				return;
			} else {
				
				System.out.println("action FAO");

				
				FindOwner act = new FindOwner();
				act.setAppointment(preferredApp);
				try {
					getContentManager().fillContent(requestMsg, new Action(provider, act));
				} catch (Exception pe) {
					pe.printStackTrace();
				}
				
				//add response behaviour
				addBehaviour(new SimpleAchieveREInitiator(myAgent, requestMsg) {
					protected void handleInform(ACLMessage msg) {
						try {
							IsOwned content = (IsOwned) getContentManager().extractContent(msg);
							String owner = content.getOwner().getPatient();
							agentWithPreferred = new AID(owner, true);
							System.out.println("agent with preferred appt: " + agentWithPreferred.getLocalName());
							System.out.println("this agent: " + getAID().getLocalName());
							System.out.println("creating a proposal to swap");
							myAgent.addBehaviour(new ProposeSwap(myAgent));
						} catch (UngroundedException e) {
							e.printStackTrace();
						} catch (CodecException e) {
							e.printStackTrace();
						} catch (OntologyException e) {
							e.printStackTrace();
						}
					}
					protected void handleRefuse(ACLMessage msg) {
						System.out.println("Engagement refused");
						
					}
				});
				
				this.finished = true;
				System.out.println("sending fao");
			}
		}
		
		

		@Override
		public boolean done() {
			return this.finished;
		}
		
	}
	
	
	
	public class ProposeSwap extends Behaviour {

		ACLMessage proposeMsg;
		Set<Integer> best;
		private boolean finished = false;
		
		public ProposeSwap(Agent patientAgent) {
			super(patientAgent);
			// Create an ACL message for hospital
			proposeMsg = new ACLMessage(ACLMessage.REQUEST);
			proposeMsg.addReceiver(agentWithPreferred);
			proposeMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			proposeMsg.setOntology(AppointmentOntology.NAME);
			proposeMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
			
			best = preferences.getBestPreferences();
			System.out.println("Proposal Swap Constructed");
		}
		
		@Override
		public void action() {
			
			//only request if appointment is allocated
			if(allocation == null){
				System.out.println("allocation null");
				return;
			}
			//if there is no partner, return
			if(agentWithPreferred == null){
				return;
			}
			
			Integer appointment = allocation.getNumber();
			
			if(best.contains(appointment)) {
				this.finished = true;
				return;
			} else {
				PatientRequestSwap act = new PatientRequestSwap();
				act.setCurrentAppointment(allocation);
				act.setRequestedAppointment(preferredApp);
				System.out.println("Proposing swap with agent " + agentWithPreferred);
				
				try {
					getContentManager().fillContent(proposeMsg, new Action(agentWithPreferred, act));
				} catch (Exception pe) {
					pe.printStackTrace();
				}
				
				//add response behaviour
				addBehaviour(new SimpleAchieveREInitiator(myAgent, proposeMsg) {
					protected void handleInform(ACLMessage msg) {
						try {
							// Extract new appointment
							IsOwned content = (IsOwned) getContentManager().extractContent(msg);
							Appointment newApp = content.getAppointment();
							System.out.println("Swap confirmed in " + myAgent.getLocalName() + ". NewApp set to " + newApp.getNumber());
							
							if(!agentWithPreferred.equals(provider)){
								System.out.println("Informing hospital");
								// Construct inform message for hospital
								HospitalSwapInform inform = new HospitalSwapInform();
								inform.setCurrentlyOwned(allocation);
								inform.setNewAppointment(newApp);
								System.out.println("allocation for " + myAgent.getLocalName() + " is " + allocation.getNumber());
								System.out.println("new appointment to override in" + myAgent.getLocalName() + " is " + newApp.getNumber());
								System.out.println("sendinding these values to hospital from " + myAgent.getLocalName());
								ACLMessage hospInform = new ACLMessage(ACLMessage.INFORM);
								hospInform.addReceiver(provider);
								hospInform.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
								hospInform.setOntology(AppointmentOntology.NAME);
								hospInform.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
								getContentManager().fillContent(hospInform, inform);
								
								// Send inform message
								send(hospInform);
							}
							
							// Set our allocation to the new appointment
							allocation = newApp;
							System.out.println(myAgent.getLocalName() + " appointment is now " + newApp.getNumber());
							responded = true;
							fireResponses();
							
						} catch (UngroundedException e) {
							e.printStackTrace();
						} catch (CodecException e) {
							e.printStackTrace();
						} catch (OntologyException e) {
							e.printStackTrace();
						}
					}
					
					private void fireResponses() throws UngroundedException, CodecException, OntologyException {
						for(ACLMessage request: pendingRequests){
							ContentElement content = getContentManager().extractContent(request);
							Concept action = ((Action)content).getAction();
							respond(request, (PatientRequestSwap) action);
						}
					}

					protected void handleRefuse(ACLMessage msg) {
						System.out.println("Swap request refused");						
					}
				});
				
			}
			
			this.finished = true;
			responded = false;
			
		}
		
		@Override
		public boolean done() {
			return this.finished;
		}
		
		
	}
	
	
	public class RespondToProposal1 extends CyclicBehaviour {
		
		

		public RespondToProposal1(Agent patientAgent) {
			super(patientAgent);
		}
		
		@Override
		public void action() {
			ACLMessage msg = receive(messageMatcher.RequestSwap);
			
			if(msg != null) {
				//only request if appointment is allocated
				if(allocation == null){
					System.out.println("allocation null");
					//TODO send reject
				}
				ContentElement content;
				try {
					content = getContentManager().extractContent(msg);
					Concept action = ((Action)content).getAction();
					if(action instanceof PatientRequestSwap) {
						if(((PatientRequestSwap) action).getRequestedAppointment().getNumber() > preferredApp.getNumber() && !responded) {
							addToQueue(msg);
						} else {
							respond(msg, (PatientRequestSwap) action);
						}
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
			} else {
				block();
			}
			
		}
		
		
	}

	private void respond(ACLMessage msg, PatientRequestSwap action) throws CodecException, OntologyException {
		ACLMessage reply = msg.createReply();
		reply.addReceiver(msg.getSender());

		reply.setPerformative(ACLMessage.REFUSE);
		if(!action.getRequestedAppointment().equals(allocation)) {
			System.err.println("WRONG!!! incorrect appointment sent");
			reply.setPerformative(ACLMessage.REFUSE);
			System.err.println("requested appointment " + action.getRequestedAppointment().getNumber()
					+ ". actual appointment owned is " + allocation.getNumber());
		} else if(!isAsDesirable(action.getCurrentAppointment())){
			reply.setPerformative(ACLMessage.REFUSE);
			System.err.println("requested appointment " + action.getCurrentAppointment().getNumber()
					+ " isn't as desirable as " + allocation.getNumber());
			System.err.println("received from " + msg.getSender().getLocalName() + " to " + getLocalName());
		} else {
			reply.setPerformative(ACLMessage.INFORM);
			Appointment toSend = allocation;
			allocation = action.getCurrentAppointment();
			Owner owner = new Owner(getAID());
			IsOwned isOwned = new IsOwned(toSend, owner);
			System.out.println("from " + getLocalName() + " sending allocation " + toSend.getNumber());
			System.out.println("currently in " + getLocalName() + " my allocation " + allocation.getNumber());
			getContentManager().fillContent(reply, isOwned);
			ACLMessage hospitalInfMsg = new ACLMessage(ACLMessage.INFORM);
			hospitalInfMsg.addReceiver(provider);
			hospitalInfMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			hospitalInfMsg.setOntology(AppointmentOntology.NAME);
			hospitalInfMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
			HospitalSwapInform hsi = new HospitalSwapInform();
			hsi.setCurrentlyOwned(toSend);
			hsi.setNewAppointment(allocation);
			getContentManager().fillContent(hospitalInfMsg, hsi);
			System.out.println("sending to hospital now from " + getLocalName());
			send(hospitalInfMsg);
		}
		send(reply);
	}

	private void addToQueue(ACLMessage msg) {
		pendingRequests.add(msg);
	}

	private boolean isAsDesirable(Appointment toCompare) {
		return preferences.isPreferable(toCompare.getNumber(), allocation.getNumber());
	}

}
