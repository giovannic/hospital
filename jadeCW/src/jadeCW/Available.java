package jadeCW;

import jade.content.Predicate;

@SuppressWarnings("serial")
public class Available implements Predicate {
	private Appointment _appointment;

	public Appointment getAppointment() {
		return _appointment;
	}

	public void setAppointment(Appointment _appointment) {
		this._appointment = _appointment;
	}
}
