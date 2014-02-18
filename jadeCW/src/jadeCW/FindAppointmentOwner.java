package jadeCW;

import jade.content.lang.Codec.CodecException;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.SimpleAchieveREInitiator;
import jadeCW.AppointmentOntology;
import jadeCW.FindOwner;
import jadeCW.IsOwned;
import jadeCW.PatientAgent;
import jadeCW.PatientAgent.ProposeSwap;


public class FindAppointmentOwner extends Behaviour {

		ACLMessage requestMsg;
		private boolean finished = false;
		PatientAgent patientAgent;
		
		
		public FindAppointmentOwner(Agent agent) {
			super(agent);
			patientAgent = (PatientAgent) patientAgent;
			// Create an ACL message for hospital
			requestMsg = new ACLMessage(ACLMessage.REQUEST);
			requestMsg.addReceiver(patientAgent.getProvider());
			requestMsg.setLanguage(FIPANames.ContentLanguage.FIPA_SL);
			requestMsg.setOntology(AppointmentOntology.NAME);
			requestMsg.setProtocol(FIPANames.InteractionProtocol.FIPA_QUERY);
			System.out.println("find appt owner constructed");
		}



		@Override
		public void action() {
			System.out.println("beginning to find appt owner");
			//only request if appointment is allocated
			if(patientAgent.getAllocation() == null){
				System.out.println("allocation null");
				return;
			}
			//if there is no provider, return
			if(patientAgent.getProvider() == null){
				return;
			}
			
			Integer appointment = patientAgent.getAllocation().getNumber();
			
			if(patientAgent.getBest().contains(appointment)) {
				
				this.finished = true;
				//already have best, do nothing
				return;
			} else {
				
				System.out.println("action FAO");

				
				FindOwner act = new FindOwner();
				act.setAppointment(patientAgent.getPreferredApp());
				try {
					patientAgent.getContentManager().fillContent(requestMsg, new Action(patientAgent.getProvider(), act));
				} catch (Exception pe) {
					pe.printStackTrace();
				}
				
				//add response behaviour
				patientAgent.addBehaviour(new SimpleAchieveREInitiator(myAgent, requestMsg) {
					protected void handleInform(ACLMessage msg) {
						try {
							IsOwned content = (IsOwned) patientAgent.getContentManager().extractContent(msg);
							String owner = content.getOwner().getPatient();
							patientAgent.setAgentWithPreferred(new AID(owner, true));
							System.out.println("agent with preferred appt: " + patientAgent.getAgentWithPreferred().getLocalName());
							System.out.println("this agent: " + patientAgent.getAID().getLocalName());
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