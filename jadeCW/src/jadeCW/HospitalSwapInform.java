package jadeCW;

import jade.content.Predicate;

public class HospitalSwapInform implements Predicate{
	
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
