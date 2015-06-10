package com.handi.floci.modules.individualclassification;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import com.handi.floci.modules.conceptclassification.HierarchyGenerator;

import fuzzydl.Concept;
import fuzzydl.Individual;
import fuzzydl.KnowledgeBase;
import fuzzydl.MinInstanceQuery;
import fuzzydl.Query;
import fuzzydl.milp.Solution;

public class IndividualClassificationDisplayer {
	private KnowledgeBase kb;
	private OWLOntology ontology;
	private OWLOntologyManager manager;
	private SimpleFuzzyReasoner fuzzyReasoner;
	
	public IndividualClassificationDisplayer(KnowledgeBase kb, OWLOntology ontology) {
		this.kb = kb;
		this.ontology = ontology;
	}
	
	@SuppressWarnings("unchecked")
	public void calculateMembershipFuzzyDL(OWLNamedIndividual individu) {
		Set<OWLClass> clazzes = ontology.getClassesInSignature();
		try {
			Individual i = kb.getIndividual(individu.getIRI().getFragment());
			
			JSONArray degrees = new JSONArray();
			
			for(OWLClass clazz: clazzes) {
				Concept c = kb.getConcept(clazz.getIRI().getFragment());
				Query query = new MinInstanceQuery(c,i);
				Solution result = query.solve(kb);
				if(result == null) System.out.println(" result null");
				else {
					JSONObject degre = new JSONObject();
					degre.put("name", c.toString());
					degre.put("degree", result.getSolution());
					degrees.add(degre);
					System.out.println(c.toString() + " : " + result.getSolution());
				}
			}
			
			FileWriter file = null;
			try {
				file = new FileWriter("D:\\Development\\Workspace\\Java\\FLOCI\\src\\com\\handi\\floci\\modules\\display\\degrees.json");
				file.write(degrees.toJSONString());
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				try {
					file.flush();
					file.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public IndividualClassificationDisplayer(HierarchyGenerator hierarchyGenerator) {
		manager = hierarchyGenerator.getOntologyManager();
		ontology = hierarchyGenerator.getOntology();
		// Initialize Fuzzy Reasoner
		fuzzyReasoner = new SimpleFuzzyReasoner(hierarchyGenerator.getReasoner(),
				hierarchyGenerator.getOntology(), manager);
		
	}
	
	@SuppressWarnings("unchecked")
	public void calculateMembershipSFR(OWLNamedIndividual individu) {
		HashMap<String, Double> hashmap = fuzzyReasoner.calculateTruthDegree(individu);
		Reasoner reasoner = fuzzyReasoner.getCrispReasoner();
		System.out.println(individu.getIRI().getFragment());
		
		for(String clazzName : hashmap.keySet()) {
			if(hashmap.get(clazzName) == 1.0) {
				
				ArrayList<Double> degrees = new ArrayList<Double>();
				ArrayList<String> concepts = new ArrayList<String>();
				OWLClass clazz = manager.getOWLDataFactory()
						.getOWLClass(IRI.create(ontology.getOntologyID().getOntologyIRI().toString() 
								+ "#" + clazzName));
				
				for (OWLClass child : reasoner.getSubClasses(clazz, true).getFlattened()) {
	            	if (!child.equals(clazz)) {
	            		String name = child.getIRI().getFragment();
	            		if(!name.equals("Nothing")) {
		            		degrees.add(hashmap.get(name));
		            		concepts.add(name);
	            		}
	                }
	            }
				
				if(!degrees.isEmpty()) {
					if(Collections.max(degrees) == 0.0) {
						hashmap.put(clazzName, 10.0);
						for(String concept: concepts) {
							hashmap.put(concept, 10.0);
						}
					}
				}
			}
		}
		
		JSONArray degrees = new JSONArray();
		for(String clazzName : hashmap.keySet()) {
			JSONObject degre = new JSONObject();
			degre.put("name", clazzName);
			degre.put("degree", hashmap.get(clazzName));
			degrees.add(degre);
			System.out.println("Class: " + clazzName + " , Degree: " + hashmap.get(clazzName));
		}
		
		
		try {
			FileWriter file = null;
			try {
				file = new FileWriter("D:\\Development\\Workspace\\Java\\FLOCI\\src\\com\\handi\\floci\\modules\\display\\degrees.json");
				file.write(degrees.toJSONString());
			} catch(Exception e) {
				e.printStackTrace();
			} finally {
				try {
					file.flush();
					file.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
