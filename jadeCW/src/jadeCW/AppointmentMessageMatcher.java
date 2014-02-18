package jadeCW;
import jade.content.Concept;
import jade.content.ContentElement;
import jade.content.lang.Codec.CodecException;
import jade.content.onto.OntologyException;
import jade.content.onto.UngroundedException;
import jade.content.onto.basic.Action;
import jade.core.Agent;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.lang.acl.MessageTemplate.MatchExpression;;

public class AppointmentMessageMatcher {
	private Agent myAgent;
	
	public AppointmentMessageMatcher(Agent myAgent){
		this.myAgent = myAgent;
	}
	
	//match message on content being of type owner
	@SuppressWarnings("serial")
	private final MatchExpression Owner = new MatchExpression(){

		@Override
		public boolean match(ACLMessage arg0) {
			try {
				ContentElement content = myAgent.getContentManager().extractContent(arg0);
				Concept action = ((Action)content).getAction();
				return (action instanceof FindOwner);
			} catch (UngroundedException e) {
				e.printStackTrace();
			} catch (CodecException e) {
				e.printStackTrace();
			} catch (OntologyException e) {
				e.printStackTrace();
			}
			return false;
		}

	};
	
	//match message on content being of type owner
	@SuppressWarnings("serial")
	private final MatchExpression Obtain = new MatchExpression(){

		@Override
		public boolean match(ACLMessage arg0) {
			try {
				ContentElement content = myAgent.getContentManager().extractContent(arg0);
				Concept action = ((Action)content).getAction();
				return (action instanceof AssignAppointment);
			} catch (UngroundedException e) {
				e.printStackTrace();
			} catch (CodecException e) {
				e.printStackTrace();
			} catch (OntologyException e) {
				e.printStackTrace();
			}
			return false;
		}

	};
	
	//match message on content being of type owner
		@SuppressWarnings("serial")
		private final MatchExpression Swap = new MatchExpression(){

			@Override
			public boolean match(ACLMessage arg0) {
				try {
					ContentElement content = myAgent.getContentManager().extractContent(arg0);
					Concept action = ((Action)content).getAction();
					return (action instanceof HospitalSwapInform);
				} catch (UngroundedException e) {
					e.printStackTrace();
				} catch (CodecException e) {
					e.printStackTrace();
				} catch (OntologyException e) {
					e.printStackTrace();
				}
				return false;
			}

		};
	
	public final MessageTemplate OwnerRequest = MessageTemplate.and(
			MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
			new MessageTemplate(Owner));
	
	public final MessageTemplate ObtainRequest = MessageTemplate.and(
			MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
			new MessageTemplate(Obtain));
	
	public final MessageTemplate SwapInform = MessageTemplate.and(
			MessageTemplate.MatchPerformative(ACLMessage.INFORM),
			new MessageTemplate(Swap));
	
}
