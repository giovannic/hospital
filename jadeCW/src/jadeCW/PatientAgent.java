package jadeCW;

import jade.content.lang.sl.SLCodec;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
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
			
			//set content
			Available f = new Available();
			Appointment a = new Appointment();
			a.setNumber(preferences.getBestPreferences().iterator().next());
			f.setAppointment(a);
			
			try {
    			myAgent.getContentManager().fillContent(requestMsg, f);
    			send(requestMsg);
    			//System.out.println("requested");
			} catch (Exception pe) {
				pe.printStackTrace();
			}
			
			//add response behaviour
			addBehaviour(new SimpleAchieveREInitiator(myAgent, requestMsg) {
				protected void handleInform(ACLMessage msg) {
					System.out.println("Engagement successfully completed");
					allocation = new Appointment();
					
				}
				protected void handleRefuse(ACLMessage msg) {
					System.out.println("Engagement refused");
					
				}
			});
			System.out.println("finished = " + finished);
			finished = true;
		}
		
		

		@Override
		public boolean done() {
			return finished;
		}

	}
}
