package jadeCW;

import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.schema.ConceptSchema;
import jade.content.schema.PredicateSchema;
import jade.content.schema.PrimitiveSchema;

@SuppressWarnings("serial")
public class AppointmentOntology extends Ontology {

	// Concepts
	public static final String APPOINTMENT = "APPOINTMENT";
	public static final String APPOINTMENT_NUMBER = "number";

	public static final String AVAILABLE = "AVAILABLE";
	public static final String AVAILABLE_APPOINTMENT = "appointment";

	private static Ontology theInstance = new AppointmentOntology();
	public static String NAME = "appointment-ontology";

	public static Ontology getInstance() {
		return theInstance;
	}

	private AppointmentOntology() {
		super(NAME, BasicOntology.getInstance());

		try {
			add(new ConceptSchema(APPOINTMENT), Appointment.class);
			add(new PredicateSchema(AVAILABLE), Available.class);

			ConceptSchema cs = (ConceptSchema) getSchema(APPOINTMENT);
			cs.add(APPOINTMENT_NUMBER,
					(PrimitiveSchema) getSchema(BasicOntology.INTEGER));

			PredicateSchema ps = (PredicateSchema) getSchema(AVAILABLE);
			ps.add(AVAILABLE_APPOINTMENT,
					(ConceptSchema) getSchema(APPOINTMENT));
		} catch (OntologyException oe) {
			oe.printStackTrace();
		}
	}
}
