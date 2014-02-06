package jadeCW;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;


@SuppressWarnings("serial")
public class HospitalAgent extends Agent {
	
	private int available;
	
	@Override
	protected void setup(){
		available = Integer.parseInt(getArguments()[0].toString());
		registerAppointmentAgent("Hospital");
	}
	
	protected void registerAppointmentAgent(String serviceName) {
		try {
	  		DFAgentDescription dfd = new DFAgentDescription();
	  		dfd.setName(getAID());
	  		ServiceDescription sd = new ServiceDescription();
	  		sd.setName(serviceName);
	  		sd.setType("allocate-appointments");
	  		// Agents that want to use this service need to "know" the appointment-ontology
			getContentManager().registerOntology(AppointmentOntology.getInstance());
	  		// Agents that want to use this service need to "speak" the FIPA-SL language
	  		sd.addLanguages(FIPANames.ContentLanguage.FIPA_SL);
	  		dfd.addServices(sd);
	  		
	  		DFService.register(this, dfd);
	  	}
	  	catch (FIPAException fe) {
	  		fe.printStackTrace();
	  	}
	}
}
