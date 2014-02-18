package jadeCW;

import jade.content.lang.Codec.CodecException;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.SimpleAchieveREInitiator;

public class RequestAppointment extends Behaviour {

	ACLMessage requestMsg;
	private boolean finished = false;
	PatientAgent patientAgent;
	
	public RequestAppointment(PatientAgent patientAgent) {
		super(patientAgent);
		this.patientAgent = patientAgent;
		// Create an ACL message for hospital
		requestMsg = new ACLMessage(ACLMessage.REQUEST);
		requestMsg.addReceiver(patientAgent.getProvider());
		requestMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
		requestMsg.setOntology(AppointmentOntology.NAME);
		requestMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
		
	}

	@Override
	public void action() {
		
		//only request once
		if(patientAgent.getAllocation() != null){
			return;
		}
		//if there is no provider, return
		if(patientAgent.getProvider() == null){
			return;
		}
		
		AssignAppointment act = new AssignAppointment();
					
		try {
			patientAgent.getContentManager().fillContent(requestMsg, new Action(patientAgent.getProvider(), act));
		} catch (Exception pe) {
			pe.printStackTrace();
		}
		
		//add response behaviour
		patientAgent.addBehaviour(new SimpleAchieveREInitiator(myAgent, requestMsg) {
			protected void handleInform(ACLMessage msg) {
				System.out.println("Engagement successfully completed");
				try {
					Available content = (Available) patientAgent.getContentManager().extractContent(msg);
					patientAgent.setAllocation(content.getAppointment());
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
