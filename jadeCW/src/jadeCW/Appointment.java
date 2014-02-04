package jadeCW;

import jade.content.Concept;

public class Appointment implements Concept {
	private Integer    _number;
	public void setNumber(Integer integer) {
		_number=integer;
	}
	public Integer getNumber() {
		return _number;
	}
	// Other application specific methods
	public boolean equals(Appointment a){
		return (_number.longValue() == a.getNumber().longValue());
	}
}
