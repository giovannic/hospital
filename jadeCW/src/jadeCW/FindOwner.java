package jadeCW;

import jade.content.AgentAction;

public class FindOwner implements AgentAction {
	
	private Appointment slot;

	public Appointment getAppointment() {
		return slot;
	}
	
	public void setAppointment(Appointment slot) {
		this.slot = slot;
	}
}
