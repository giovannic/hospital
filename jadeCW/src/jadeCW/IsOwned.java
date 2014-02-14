package jadeCW;

import jade.content.Predicate;

public class IsOwned implements Predicate {
	private Appointment appointment;
	private Owner owner;

	public Appointment getAppointment() {
		return appointment;
	}

	public void setAppointment(Appointment appointment) {
		this.appointment = appointment;
	}

	public Owner getOwner() {
		return owner;
	}

	public void setOwner(Owner owner) {
		this.owner = owner;
	}
}
