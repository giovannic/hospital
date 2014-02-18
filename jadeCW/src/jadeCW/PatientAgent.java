package jadeCW;

import java.util.Set;

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
import jade.core.messaging.TopicManagementHelper;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.SimpleAchieveREInitiator;
import jade.proto.SubscriptionInitiator;

@SuppressWarnings("serial")
public class PatientAgent extends Agent {

	
	AID provider;
	Appointment allocation;
	Preferences preferences;
	protected boolean finished = false;
	AID agentWithPreferred = null;
	Appointment preferredApp;
	
	//topics
	AID obtainRequest;
	AID ownerRequest;
	
	
	protected void setup() {
		preferences = Preferences.parsePreferences(getArguments()[0].toString());
		getContentManager().registerOntology(AppointmentOntology.getInstance(), AppointmentOntology.NAME);
		getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL);
		subscribeToAppointments();
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
			
			finished = true;
		}
		
		

		@Override
		public boolean done() {
			return finished;
		}
	
	}
	
	public class FindAppointmentOwner extends Behaviour {

		ACLMessage requestMsg;
		Set<Integer> best;
		
		public FindAppointmentOwner(Agent patientAgent) {
			super(patientAgent);
			// Create an ACL message for hospital
			requestMsg = new ACLMessage(ACLMessage.REQUEST);
			requestMsg.addReceiver(provider);
			requestMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			requestMsg.setOntology(AppointmentOntology.NAME);
			requestMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
			best = preferences.getBestPreferences();
			System.out.println("find appt owner constructed");
		}



		@Override
		public void action() {
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
				//already have best, do nothing
				return;
			} else {
				
				System.out.println("action FAO");

				
				FindOwner act = new FindOwner();
				Appointment idealAppt = new Appointment();
				Integer[] a = new Integer[best.size()];
				Integer[] bestArr = (Integer[]) best.toArray(a);
				idealAppt.setNumber(bestArr[0]);
				act.setAppointment(idealAppt);
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
							Appointment app = content.getAppointment();
							agentWithPreferred = new AID(owner, true);
							preferredApp = app;
							System.out.println("agent with preferred appt: " + agentWithPreferred.getLocalName());
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
				
				finished = true;
				System.out.println("sending fao");
			}
		}
		
		

		@Override
		public boolean done() {
			return finished;
		}
		
	}
	
	
	
	public class ProposeSwap extends Behaviour {

		ACLMessage proposeMsg;
		Set<Integer> best;
		
		public ProposeSwap(Agent patientAgent) {
			super(patientAgent);
			// Create an ACL message for hospital
			proposeMsg = new ACLMessage(ACLMessage.REQUEST);
			proposeMsg.addReceiver(agentWithPreferred);
			proposeMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			proposeMsg.setOntology(AppointmentOntology.NAME);
			proposeMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
			
			best = preferences.getBestPreferences();
			System.out.println("Request Swap Constructed");
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
				return;
			} else {
				
				PatientRequestSwap act = new PatientRequestSwap();
				act.setCurrentAppointment(allocation);
				act.setRequestedAppointment(preferredApp);
				
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
							
							// Construct inform message for hospital
							HospitalSwapInform informAct = new HospitalSwapInform();
							informAct.setCurrentlyOwned(allocation);
							informAct.setNewAppointment(newApp);
							ACLMessage hospInform = new ACLMessage(ACLMessage.INFORM);
							hospInform.addReceiver(provider);
							hospInform.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
							hospInform.setOntology(AppointmentOntology.NAME);
							hospInform.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
							getContentManager().fillContent(hospInform, new Action(provider, informAct));
							
							// Send inform message
							send(hospInform);					
							
							// Set our allocation to the new appointment
							allocation = newApp;
							
						} catch (UngroundedException e) {
							e.printStackTrace();
						} catch (CodecException e) {
							e.printStackTrace();
						} catch (OntologyException e) {
							e.printStackTrace();
						}
					}
					protected void handleRefuse(ACLMessage msg) {
						System.out.println("Swap request refused");						
					}
				});
				
			}
			
			finished = true;
			
		}
		
		@Override
		public boolean done() {
			return finished;
		}
		
		
	}
}
