package jadeCW;

import java.util.LinkedList;
import java.util.List;
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
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.proto.SubscriptionInitiator;

@SuppressWarnings("serial")
public class PatientAgent extends Agent {

	
	private AID provider;
	private Appointment allocation;
	Preferences preferences;
	private AID agentWithPreferred = null;
	private Appointment preferredApp;
	private List<ACLMessage> pendingRequests = new LinkedList<ACLMessage>();
	
	//topics
	AID obtainRequest;
	AID ownerRequest;
	
	final AppointmentMessageMatcher messageMatcher = 
			new AppointmentMessageMatcher(this);
	public boolean responded = true;
	private Set<Integer> best;
	
	
	protected void setup() {
		preferences = Preferences.parsePreferences(getArguments()[0].toString());
		getContentManager().registerOntology(AppointmentOntology.getInstance(), AppointmentOntology.NAME);
		getContentManager().registerLanguage(new SLCodec(), FIPANames.ContentLanguage.FIPA_SL);
		subscribeToAppointments();
		setBest(preferences.getBestPreferences());
		Integer[] a = new Integer[getBest().size()];
		Integer[] bestArr = (Integer[]) getBest().toArray(a);
		setPreferredApp(new Appointment());
		getPreferredApp().setNumber(bestArr[0]);
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
						myAgent.addBehaviour(new RequestAppointment((PatientAgent) myAgent));
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
				(getAllocation() != null ? "Appointment " + getAllocation().getNumber() : "null"));
	}


	
	

	public void respond(ACLMessage msg, PatientRequestSwap action) throws CodecException, OntologyException {
		ACLMessage reply = msg.createReply();
		reply.addReceiver(msg.getSender());

		reply.setPerformative(ACLMessage.REFUSE);
		if(!action.getRequestedAppointment().equals(getAllocation())) {
			System.err.println("WRONG!!! incorrect appointment sent");
			reply.setPerformative(ACLMessage.REFUSE);
			System.err.println("requested appointment " + action.getRequestedAppointment().getNumber()
					+ ". actual appointment owned is " + getAllocation().getNumber());
		} else if(!isAsDesirable(action.getCurrentAppointment())){
			reply.setPerformative(ACLMessage.REFUSE);
			System.err.println("requested appointment " + action.getCurrentAppointment().getNumber()
					+ " isn't as desirable as " + getAllocation().getNumber());
			System.err.println("received from " + msg.getSender().getLocalName() + " to " + getLocalName());
		} else {
			reply.setPerformative(ACLMessage.INFORM);
			Appointment toSend = getAllocation();
			setAllocation(action.getCurrentAppointment());
			Owner owner = new Owner(getAID());
			IsOwned isOwned = new IsOwned(toSend, owner);
			System.out.println("from " + getLocalName() + " sending allocation " + toSend.getNumber());
			System.out.println("currently in " + getLocalName() + " my allocation " + getAllocation().getNumber());
			getContentManager().fillContent(reply, isOwned);
			ACLMessage hospitalInfMsg = new ACLMessage(ACLMessage.INFORM);
			hospitalInfMsg.addReceiver(getProvider());
			hospitalInfMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			hospitalInfMsg.setOntology(AppointmentOntology.NAME);
			hospitalInfMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
			HospitalSwapInform hsi = new HospitalSwapInform();
			hsi.setCurrentlyOwned(toSend);
			hsi.setNewAppointment(getAllocation());
			getContentManager().fillContent(hospitalInfMsg, hsi);
			System.out.println("sending to hospital now from " + getLocalName());
			send(hospitalInfMsg);
		}
		send(reply);
	}
	
	public void fireResponses() throws UngroundedException, CodecException, OntologyException {
		for(ACLMessage request: pendingRequests){
			ContentElement content = getContentManager().extractContent(request);
			Concept action = ((Action)content).getAction();
			respond(request, (PatientRequestSwap) action);
		}
	}

	public void addToQueue(ACLMessage msg) {
		pendingRequests.add(msg);
	}

	private boolean isAsDesirable(Appointment toCompare) {
		return preferences.isPreferable(toCompare.getNumber(), getAllocation().getNumber());
	}

	public AID getProvider() {
		return provider;
	}

	public Appointment getAllocation() {
		return allocation;
	}

	public void setAllocation(Appointment allocation) {
		this.allocation = allocation;
	}

	public Set<Integer> getBest() {
		return best;
	}

	public void setBest(Set<Integer> best) {
		this.best = best;
	}

	public Appointment getPreferredApp() {
		return preferredApp;
	}

	public void setPreferredApp(Appointment preferredApp) {
		this.preferredApp = preferredApp;
	}

	public AID getAgentWithPreferred() {
		return agentWithPreferred;
	}

	public void setAgentWithPreferred(AID agentWithPreferred) {
		this.agentWithPreferred = agentWithPreferred;
	}

}
