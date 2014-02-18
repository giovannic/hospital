package jadeCW;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.SimpleAchieveREInitiator;

import java.util.Set;

public class ProposeSwap extends Behaviour {

	ACLMessage proposeMsg;
	Set<Integer> best;
	private boolean finished = false;
	PatientAgent patientAgent;
	
	public ProposeSwap(Agent agent) {
		super(agent);
		patientAgent = (PatientAgent) patientAgent;
		// Create an ACL message for hospital
		proposeMsg = new ACLMessage(ACLMessage.REQUEST);
		proposeMsg.addReceiver(patientAgent.getAgentWithPreferred());
		proposeMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
		proposeMsg.setOntology(AppointmentOntology.NAME);
		proposeMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
		
		best = patientAgent.getBest();
		System.out.println("Proposal Swap Constructed");
	}
	
	@Override
	public void action() {
		
		//only request if appointment is allocated
		if(patientAgent.getAllocation() == null){
			System.out.println("allocation null");
			return;
		}
		//if there is no partner, return
		if(patientAgent.getAgentWithPreferred() == null){
			return;
		}
		
		Integer appointment = patientAgent.getAllocation().getNumber();
		
		if(best.contains(appointment)) {
			this.finished = true;
			return;
		} else {
			PatientRequestSwap act = new PatientRequestSwap();
			act.setCurrentAppointment(patientAgent.getAllocation());
			act.setRequestedAppointment(patientAgent.getPreferredApp());
			System.out.println("Proposing swap with agent " + patientAgent.getAgentWithPreferred());
			
			try {
				patientAgent.getContentManager().fillContent(proposeMsg, new Action(patientAgent.getAgentWithPreferred(), act));
			} catch (Exception pe) {
				pe.printStackTrace();
			}
			
			//add response behaviour
			patientAgent.addBehaviour(new SimpleAchieveREInitiator(myAgent, proposeMsg) {
				protected void handleInform(ACLMessage msg) {
					try {
						// Extract new appointment
						IsOwned content = (IsOwned) patientAgent.getContentManager().extractContent(msg);
						Appointment newApp = content.getAppointment();
						System.out.println("Swap confirmed in " + myAgent.getLocalName() + ". NewApp set to " + newApp.getNumber());
						
						if(!patientAgent.getAgentWithPreferred().equals(patientAgent.getProvider())){
							System.out.println("Informing hospital");
							// Construct inform message for hospital
							HospitalSwapInform inform = new HospitalSwapInform();
							inform.setCurrentlyOwned(patientAgent.getAllocation());
							inform.setNewAppointment(newApp);
							System.out.println("allocation for " + myAgent.getLocalName() + " is " + patientAgent.getAllocation().getNumber());
							System.out.println("new appointment to override in" + myAgent.getLocalName() + " is " + newApp.getNumber());
							System.out.println("sendinding these values to hospital from " + myAgent.getLocalName());
							ACLMessage hospInform = new ACLMessage(ACLMessage.INFORM);
							hospInform.addReceiver(patientAgent.getProvider());
							hospInform.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
							hospInform.setOntology(AppointmentOntology.NAME);
							hospInform.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
							patientAgent.getContentManager().fillContent(hospInform, inform);
							
							// Send inform message
							patientAgent.send(hospInform);
						}
						
						// Set our allocation to the new appointment
						patientAgent.setAllocation(newApp);
						System.out.println(myAgent.getLocalName() + " appointment is now " + newApp.getNumber());
						patientAgent.responded = true;
						patientAgent.fireResponses();
						
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
		
		this.finished = true;
		patientAgent.responded = false;
		
	}
	
	@Override
	public boolean done() {
		return this.finished;
	}
	
	
}
