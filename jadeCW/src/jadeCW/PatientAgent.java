package jadeCW;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jade.content.lang.Codec.CodecException;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
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
import jade.util.leap.Iterator;

@SuppressWarnings("serial")
public class PatientAgent extends Agent {

	List<Set<Integer>> preferences;
	AID provider;
	Appointment allocation;

	protected void setup() {
		preferences = parsePreferences(getArguments()[0].toString());
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
					if (results.length > 0) {
						for (int i = 0; i < results.length; ++i) {
							DFAgentDescription dfd = results[i];
							AID provider = dfd.getName();
							// The same agent may provide several services; we
							// are only interested
							// in the weather-forcast one
							Iterator it = dfd.getAllServices();
							while (it.hasNext()) {
								ServiceDescription sd = (ServiceDescription) it
										.next();
								if (sd.getType().equals("allocate-appointments")) {
									setProvider(provider);
									System.out.println("Hospital found:");
									System.out.println("- Service \""
											+ sd.getName()
											+ "\" provided by agent "
											+ provider.getName());
								}
							}
						}
					}
					System.out.println();
				} catch (FIPAException fe) {
					fe.printStackTrace();
				}
			}
		});
	}

	private List<Set<Integer>> parsePreferences(String string) {
		List<Set<Integer>> preferences = new LinkedList<Set<Integer>>();
		for (String level : string.split("-")) {
			Set<Integer> levelSet = new HashSet<Integer>();
			for (String preference : level.split(" ")) {
				levelSet.add(Integer.parseInt(preference));
			}
			preferences.add(levelSet);
		}
		return null;
	}
	
	private void setProvider(AID provider){
		this.provider = provider;
	}
	
	

	public class RequestAppointment extends Behaviour {

		@Override
		public void action() {
			if(allocation == null || provider == null){
				return;
			}
			// Create an ACL message for hospital
			ACLMessage requestMsg = new ACLMessage(ACLMessage.REQUEST);
			requestMsg.addReceiver(provider);
			requestMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			requestMsg.setOntology(AppointmentOntology.NAME);
			requestMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
			
			Free f = new Free();
			Appointment a = new Appointment();
			a.setNumber(preferences.iterator().next().iterator().next());
			f.set_appointment(a);
			try {
    			myAgent.getContentManager().fillContent(requestMsg, f);
			} catch (Exception pe) {
				pe.printStackTrace();
			}
			
			//add response behaviour
			addBehaviour(new SimpleAchieveREInitiator(myAgent, requestMsg) {
				protected void handleAgree(ACLMessage msg) {
					System.out.println("Engagement agreed");
					try {
						allocation = (Appointment)myAgent.getContentManager().extractContent(msg);
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
			});
		}
		
		

		@Override
		public boolean done() {
			// TODO Auto-generated method stub
			return false;
		}

	}
}
