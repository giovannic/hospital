package jadeCW;

import jade.content.Predicate;

@SuppressWarnings("serial")
public class Owner implements Predicate {

	Appointment _appointment;
	
	public Appointment getAppointment() {
		return _appointment;
	}

}
