package jadeCW;

import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;

public class RespondToProposal1 extends CyclicBehaviour {
	
	PatientAgent patientAgent;

	public RespondToProposal1(PatientAgent patientAgent) {
		super(patientAgent);
		this.patientAgent = patientAgent;
	}
	
	@Override
	public void action() {
		ACLMessage msg = patientAgent.receive(patientAgent.messageMatcher.RequestSwap);
		
		if(msg != null) {
			//only request if appointment is allocated
			if(patientAgent.getAllocation() == null){
				System.out.println("allocation null");
				//TODO send reject
			}
			ContentElement content;
			try {
				content = patientAgent.getContentManager().extractContent(msg);
				Concept action = ((Action)content).getAction();
				if(action instanceof PatientRequestSwap) {
					if(((PatientRequestSwap) action).getRequestedAppointment().getNumber() > patientAgent.getPreferredApp().getNumber() && 
							!patientAgent.responded) {
						patientAgent.addToQueue(msg);
					} else {
						patientAgent.respond(msg, (PatientRequestSwap) action);
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
