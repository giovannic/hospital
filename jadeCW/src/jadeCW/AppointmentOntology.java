package jadeCW;

import jade.content.onto.BasicOntology;
import jade.content.onto.Ontology;
import jade.content.onto.OntologyException;
import jade.content.schema.AgentActionSchema;
import jade.content.schema.ConceptSchema;
import jade.content.schema.ObjectSchema;
import jade.content.schema.PredicateSchema;
import jade.content.schema.PrimitiveSchema;

@SuppressWarnings("serial")
public class AppointmentOntology extends Ontology {

	// Concepts
	public static final String APPOINTMENT = "Appointment";
	public static final String APPOINTMENT_NUMBER = "number";
	
	public static final String OWNER = "Owner";
	public static final String PATIENT = "patient";

	//Predicates
	public static final String AVAILABLE = "Available";
	public static final String IS_OWNED = "IsOwned";
	
	//Actions
	public static final String ASSIGN_APPOINTMENT = "AssignAppointment";
	public static final String FIND_OWNER = "FindOwner";

	private static Ontology theInstance = new AppointmentOntology();
	public static String NAME = "appointment-ontology";

	public static Ontology getInstance() {
		return theInstance;
	}

	private AppointmentOntology() {
		super(NAME, BasicOntology.getInstance());

		try {
			add(new ConceptSchema(APPOINTMENT), Appointment.class);
			ConceptSchema csApp = (ConceptSchema) getSchema(APPOINTMENT);
			csApp.add(APPOINTMENT_NUMBER, (PrimitiveSchema) getSchema(BasicOntology.INTEGER), 
					ObjectSchema.MANDATORY);
			
			add(new AgentActionSchema(ASSIGN_APPOINTMENT), AssignAppointment.class);
			add(new AgentActionSchema(FIND_OWNER), FindOwner.class);
			AgentActionSchema fo = (AgentActionSchema) getSchema(FIND_OWNER);
			fo.add(APPOINTMENT, 
					csApp, ObjectSchema.MANDATORY);
			
			add(new PredicateSchema(AVAILABLE), Available.class);

			PredicateSchema psAvail = (PredicateSchema) getSchema(AVAILABLE);
			psAvail.add(APPOINTMENT, (ConceptSchema) getSchema(APPOINTMENT));
			
			add(new ConceptSchema(OWNER), Owner.class);
			ConceptSchema csOwn =  (ConceptSchema) getSchema(OWNER);
			csOwn.add(PATIENT, (PrimitiveSchema) getSchema(BasicOntology.STRING),
					ObjectSchema.MANDATORY);
			
			add(new PredicateSchema(IS_OWNED), IsOwned.class);

			PredicateSchema psIsOwn = (PredicateSchema) getSchema(IS_OWNED);
			psIsOwn.add(APPOINTMENT, (ConceptSchema) getSchema(APPOINTMENT));
			psIsOwn.add(OWNER, (ConceptSchema) getSchema(OWNER));
			
		} catch (OntologyException oe) {
			oe.printStackTrace();
		}
	}
}
