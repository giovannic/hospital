package jadeCW;

import jade.content.AgentAction;

public class PatientRequestSwap implements AgentAction {
	
	private Appointment appointment;
	
	public Appointment getAppointment() {
		return appointment;
	}

	public void setAppointment(Appointment appointment) {
		this.appointment = appointment;
	}
	
	

}
