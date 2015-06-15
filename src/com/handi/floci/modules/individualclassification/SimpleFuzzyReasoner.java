package com.handi.floci.modules.individualclassification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataCardinalityRestriction;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

public class SimpleFuzzyReasoner {
	private Reasoner crispReasoner;
	private OWLOntology ontology;
	private OWLOntologyManager manager;
	private String ontologyPath;
	private OWLAnnotationProperty label;
	
	private OWLNamedIndividual individu;
	private Set<OWLClass> individuClasses;
	
	public SimpleFuzzyReasoner(Reasoner crispReasoner, OWLOntology ontology, OWLOntologyManager manager) {
		this.crispReasoner = crispReasoner; 
		this.ontology = ontology;
		this.manager = manager;
		
		ontologyPath = this.ontology.getOntologyID().getOntologyIRI().toString();
		label = this.manager.getOWLDataFactory().getOWLAnnotationProperty(IRI.create(ontologyPath + "#" + "fuzzyLabel"));
	}
	
	public double calculateTruthDegree(OWLClassExpression clazz, OWLNamedIndividual individu) {
		crispReasoner.classifyClasses();
		
		if(crispReasoner.hasType(individu, clazz, false)) { //l'individu appartient à la classe expression
			switch (clazz.getClassExpressionType()) {
			
				case OWL_CLASS: // Si l'expression est une classe elle est donc atomique = non flou (degré d'appartenance 1)  			
				case OBJECT_SOME_VALUES_FROM: // l'expression a la forme (existe)R.C impossible qu'elle soit floue sous OWL2
				case OBJECT_ALL_VALUES_FROM: // l'expression a la forme (pour tout)R.C impossible qu'elle soit floue sous OWL2
				case OBJECT_MAX_CARDINALITY: // c'est du domaine du crisp
				case OBJECT_MIN_CARDINALITY: // c'est du domaine du crisp
				case OBJECT_EXACT_CARDINALITY: // c'est du domaine du crisp
				case DATA_HAS_VALUE: // c'est du domaine du crisp
				case OBJECT_HAS_VALUE: // c'est du domaine du crisp
				case OBJECT_ONE_OF:
				case OBJECT_HAS_SELF:
					return 1;
					
				case OBJECT_COMPLEMENT_OF:
					return 1 - calculateTruthDegree(((OWLObjectComplementOf) clazz).getOperand(), individu); 
					
				case OBJECT_INTERSECTION_OF:
					return Collections.min(getOperandsDegrees(((OWLObjectIntersectionOf) clazz).getOperands(), individu)); 
					
				case OBJECT_UNION_OF:
					return Collections.max(getOperandsDegrees(((OWLObjectUnionOf) clazz).getOperands(), individu)); 
					
				case DATA_SOME_VALUES_FROM:
					OWLDataProperty dataProperty = ((OWLDataSomeValuesFrom) clazz).getProperty().asOWLDataProperty();
					OWLDataRange range = ((OWLDataSomeValuesFrom) clazz).getFiller();
					if (range == null) // Restriction not qualified
						return Collections.min(getDatatypeDegrees(dataProperty, null, individu));
					// Restriction is qualified
					try{
						return Collections.min(getDatatypeDegrees(dataProperty, range.asOWLDatatype(), individu));
					} catch(Exception e) {
						// if the range is a default range like : double, or integer.. range.asOWLDataype throws an exception
						return 1;
					}
					
			case DATA_ALL_VALUES_FROM:
					OWLDataProperty allDataProperty = ((OWLDataAllValuesFrom) clazz).getProperty().asOWLDataProperty();
					OWLDataRange allRange = ((OWLDataSomeValuesFrom) clazz).getFiller();
					if (allRange == null) // Restriction not qualified
						return Collections.min(getDatatypeDegrees(allDataProperty, null, individu));
					// Restriction is qualified
					return Collections.min(getDatatypeDegrees(allDataProperty, allRange.asOWLDatatype(), individu));
				
				case DATA_MAX_CARDINALITY:
					OWLDataCardinalityRestriction maxCardinality = (OWLDataCardinalityRestriction) clazz;
					OWLDataProperty maxDataProperty = maxCardinality.getProperty().asOWLDataProperty();
					int maxCard = maxCardinality.getCardinality();
					ArrayList<Double> acceptableDegrees_max;
					if(maxCardinality.isQualified()) {
						OWLDatatype maxRange = ((OWLDataCardinalityRestriction) clazz).getFiller().asOWLDatatype();
						acceptableDegrees_max = getAcceptableDegreesForDatatype(maxDataProperty, maxRange, individu);
					} else {
						acceptableDegrees_max = getAcceptableDegreesForDatatype(maxDataProperty, null, individu);
					}
					
					if(acceptableDegrees_max.size() <= maxCard) 
						return moyenne(acceptableDegrees_max);
					// Above the max
					return 0;
					
				case DATA_MIN_CARDINALITY:
					OWLDataMinCardinality minCardinality = (OWLDataMinCardinality) clazz;
					OWLDataProperty minDataProperty = minCardinality.getProperty().asOWLDataProperty();
					int minCard = minCardinality.getCardinality();
					ArrayList<Double> acceptableDegrees_min;
					if(minCardinality.isQualified()) {
						OWLDatatype minRange = ((OWLDataCardinalityRestriction) clazz).getFiller().asOWLDatatype();
						acceptableDegrees_min = getAcceptableDegreesForDatatype(minDataProperty, minRange, individu);
					} else {
						acceptableDegrees_min = getAcceptableDegreesForDatatype(minDataProperty, null, individu);
					}
					
					if(acceptableDegrees_min.size() >= minCard) 
						return moyenne(acceptableDegrees_min);
					// Below the min
					return 0;

				case DATA_EXACT_CARDINALITY:
					OWLDataMinCardinality exactCardinality = (OWLDataMinCardinality) clazz;
					OWLDataProperty exactDataProperty = exactCardinality.getProperty().asOWLDataProperty();
					int exactCard = exactCardinality.getCardinality();
					ArrayList<Double> acceptableDegrees_exact;
					if(exactCardinality.isQualified()) {
						OWLDatatype exactRange = ((OWLDataCardinalityRestriction) clazz).getFiller().asOWLDatatype();
						acceptableDegrees_exact = getAcceptableDegreesForDatatype(exactDataProperty, exactRange, individu);
					} else {
						acceptableDegrees_exact = getAcceptableDegreesForDatatype(exactDataProperty, null, individu);
					}
					
					if(acceptableDegrees_exact.size() == exactCard) 
						return moyenne(acceptableDegrees_exact);
					// Not the exact number
					return 0;
					
				default: return 1;	
			}
		}
		return 0;
	}
	
	public HashMap<String, Double> calculateTruthDegree(Set<OWLClass> clazzes, OWLNamedIndividual individu) {
		HashMap<String, Double> hashmap = new HashMap<String, Double>();
		for (OWLClass clazz : clazzes) {
			Set<OWLClassExpression> equivalentClasses = clazz.getEquivalentClasses(ontology);
			ArrayList<Double> array = new ArrayList<Double>();
			array.add(calculateTruthDegree(clazz, individu));
			for(OWLClassExpression clazzExpression: equivalentClasses) {
				array.add(calculateTruthDegree(clazzExpression, individu));
			}
			hashmap.put(clazz.getIRI().getFragment(), Collections.min(array));
		}
		return hashmap;
	}
	
	public HashMap<String, Double> calculateTruthDegree(OWLNamedIndividual individu) {
		return calculateTruthDegree(ontology.getClassesInSignature(), individu);
	}
	
	public Reasoner getCrispReasoner() {
		return this.crispReasoner;
	}
	
	
	private ArrayList<Double> getAcceptableDegreesForDatatype(OWLDataProperty dataProperty, OWLDatatype range, OWLNamedIndividual individu) {
		ArrayList<Double> degreesForDatatype = getDatatypeDegrees(dataProperty, range, individu);
		for (int i = 0; i < degreesForDatatype.size(); i++) {
			if( degreesForDatatype.get(i) == 0) degreesForDatatype.remove(i);
		}
		return degreesForDatatype;
	}
	
	private ArrayList<Double> getDatatypeDegrees(OWLDataProperty dataProperty, OWLDatatype range, OWLNamedIndividual individu) {
		ArrayList<Double> degreesArray = new ArrayList<Double>();
		if(range == null) { // This restriction is not qualified: R
			for(OWLAxiom axiom : this.ontology.getAxioms(individu)) {
	    		String type = axiom.getAxiomType().toString();
	    		if(type.equals("DataPropertyAssertion")) {
	    			OWLDataPropertyAssertionAxiom dataAxiom = (OWLDataPropertyAssertionAxiom) axiom;
        			OWLDataProperty attribut = dataAxiom.getProperty().asOWLDataProperty();
        			if(attribut.getIRI() == dataProperty.getIRI()) {
        				degreesArray.add(1.0);
        			}
        			
	    		}
	    	}
		} else { // This restriction is qualified: R.C
			Iterator<OWLAnnotation> annotations = range.getAnnotations(ontology, label).iterator();
			if(annotations.hasNext()) {
				OWLAnnotation annotation = annotations.next();
				Parser parser = new Parser(annotation.getValue().toString());
				
				for(OWLAxiom axiom : this.ontology.getAxioms(individu)) {
		    		String type = axiom.getAxiomType().toString();
		    		if(type.equals("DataPropertyAssertion")) {
		    			OWLDataPropertyAssertionAxiom dataAxiom = (OWLDataPropertyAssertionAxiom) axiom;
	        			OWLDataProperty attribut = dataAxiom.getProperty().asOWLDataProperty();
	        			if(attribut.getIRI() == dataProperty.getIRI()) {
	        				double value = dataAxiom.getObject().parseDouble();
	        				degreesArray.add(parser.getDegree(value));
	        			}
	        			
		    		}
		    	}
			}
		}
		return degreesArray;
	}
	
	private ArrayList<Double> getOperandsDegrees(Set<OWLClassExpression> operands, OWLNamedIndividual individu) {
		ArrayList<Double> degreesArray = new ArrayList<Double>();
		for(OWLClassExpression operand : operands ) {
			double degree = calculateTruthDegree(operand, individu);
			degreesArray.add(degree);
		}
		return degreesArray;
	}
	
 	private double moyenne(ArrayList<Double> array) {
		if(array.isEmpty()) return 0;
		double moy = 0;
		for(int i = 0; i < array.size(); i++) {
			moy += array.get(i);
		}
		return moy / array.size();
	}
}
