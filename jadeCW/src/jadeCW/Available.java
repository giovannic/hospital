package jadeCW;

import jade.content.Predicate;

public class Available implements Predicate {
	private Appointment _appointment;

	public Appointment get_appointment() {
		return _appointment;
	}

	public void set_appointment(Appointment _appointment) {
		this._appointment = _appointment;
	}
}
