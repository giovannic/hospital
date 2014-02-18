package jadeCW;

import jade.content.AgentAction;

public class HospitalSwapInform implements AgentAction{
	
	private Appointment currentlyOwned;
	private Appointment newAppointment;
	
	public Appointment getCurrentlyOwned() {
		return currentlyOwned;
	}
	public void setCurrentlyOwned(Appointment a) {
		this.currentlyOwned = a;
	}
	public Appointment getNewAppointment() {
		return newAppointment;
	}
	public void setNewAppointment(Appointment a) {
		this.newAppointment = a;
	}

}
