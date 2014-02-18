package jadeCW;

import jade.content.AgentAction;

public class PatientRequestSwap implements AgentAction {
	
	private Appointment currentAppointment;
	private Appointment requestedAppointment;
	
	public Appointment getCurrentAppointment() {
		return currentAppointment;
	}

	public void setCurrentAppointment(Appointment appointment) {
		this.currentAppointment = appointment;
	}

	public Appointment getRequestedAppointment() {
		return requestedAppointment;
	}

	public void setRequestedAppointment(Appointment requestedAppointment) {
		this.requestedAppointment = requestedAppointment;
	}
	
	

}
