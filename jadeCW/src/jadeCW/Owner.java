package jadeCW;

import jade.content.Concept;
import jade.core.AID;

public class Owner implements Concept {
	private String patient;
	
	public Owner(String patient) {
		this.patient = patient;
	}
	
	public Owner(AID patient) {
		this.patient = patient.getName();
	}
	
	public void setPatient(String patient) {
		this.patient = patient;
	}
	public String getPatient() {
		return patient;
	}
}
