package jadeCW;

import jade.content.Concept;

public class Owner implements Concept {
	private String patient;
	public void setPatient(String patient) {
		this.patient = patient;
	}
	public String getPatient() {
		return patient;
	}
}
