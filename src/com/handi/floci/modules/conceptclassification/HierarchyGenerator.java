package com.handi.floci.modules.conceptclassification;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class HierarchyGenerator {
	private Reasoner m_reasoner;
	private OWLOntologyManager m_manager;
	private OWLOntology m_ontology;
	private String ontologyFilePath;
	
	public HierarchyGenerator(File ontologyFile) throws OWLOntologyCreationException {
		ontologyFilePath = ontologyFile.getAbsolutePath();
		// Get hold of an ontology manager
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    
		// Load the local copy
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(ontologyFile);
		
		m_reasoner = new Reasoner(ontology);
		m_manager = manager;
		m_ontology = ontology;
	}

	public void reload() throws OWLOntologyCreationException {
		// Get hold of an ontology manager
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
    
		// Load the local copy
		OWLOntology ontology = manager.loadOntologyFromOntologyDocument(new File(ontologyFilePath));
		
		m_reasoner = new Reasoner(ontology);
		m_manager = manager;
		m_ontology = ontology;
	}
	
	@SuppressWarnings("unchecked")
	public void getConceptsHierarchy(TreeView<String> hierarchyTree) {
		TreeItem<String> placeholderItem = new TreeItem<String> ("");
		JSONObject json = new JSONObject();
		JSONArray links = new JSONArray();
		
		JSONArray nodes = getConceptsJSONList();
		OWLClass thing = m_manager.getOWLDataFactory().getOWLThing();
		printHierarchy(thing, placeholderItem, links);
		
		json.put("nodes", nodes);
		json.put("links", links);
		FileWriter file = null;
		try {
			file = new FileWriter("D:\\Development\\Workspace\\Java\\FLOCI\\src\\com\\handi\\floci\\modules\\display\\data.json");
			file.write(json.toJSONString());
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
		
		TreeItem<String> rootItem = placeholderItem.getChildren().get(0);
		rootItem.setExpanded(true);
		hierarchyTree.setRoot(rootItem);
	}
	
	public void getAllProperties(ListView<OWLObjectProperty> propertiesList) {
		for (OWLObjectProperty property : m_ontology.getObjectPropertiesInSignature()) {
			propertiesList.getItems().add(property);
        }
	}
	
	public void getAllAttributes(ListView<OWLDataProperty> attributeList) {		
		for (OWLDataProperty attribute : m_ontology.getDataPropertiesInSignature()) {
			attributeList.getItems().add(attribute);
        }
	}
	
	public ObservableList<OWLNamedIndividual> getAllIndividuals() {
		ObservableList<OWLNamedIndividual> individuals = FXCollections.observableArrayList();
		for (OWLNamedIndividual individual : m_ontology.getIndividualsInSignature()) { // getFlattened returns the <E> of a Node<E>
			individuals.add(individual);
        }
		return individuals;
	}
	
	@SuppressWarnings("unchecked")
	private void printHierarchy(OWLClass clazz, TreeItem<String> parent, JSONArray links) {
        if(clazz != null) {
	    	//Only print satisfiable classes
	        if (m_reasoner.isSatisfiable(clazz)) {
	        	// Create the tree node and add it to its parent
	        	TreeItem<String> clazzItem = new TreeItem<String> (labelFor(clazz));
	        	parent.getChildren().add(clazzItem);
	            
	        	// Find the children and recurse 
	            for (OWLClass child : m_reasoner.getSubClasses(clazz, true).getFlattened()) { // getFlattened returns the <E> of a Node<E>
	            	if (!child.equals(clazz)) {
	            		String label = labelFor(child);
	            		if(!label.equals("Nothing")) {
		            		// Create the Json link:
		    	        	JSONObject link = new JSONObject();
		    	        	link.put("source", labelFor(clazz));
		    	        	link.put("target", label);
		    	        	links.add(link);
	            		}
	                    printHierarchy(child, clazzItem, links);
	                }
	            }
	        }
        }
    }
    
	@SuppressWarnings("unchecked")
	private JSONArray getConceptsJSONList() {
		JSONArray nodes = new JSONArray();
		for (OWLClass clazz: m_ontology.getClassesInSignature()) {
				JSONObject node = new JSONObject();
				node.put("name", labelFor(clazz));
				nodes.add(node);
		}
		return nodes;
	}
	
    private String labelFor(OWLClass clazz) {
    	if(clazz != null)
    	return clazz.getIRI().getFragment();
    	
    	return null;
    }
    
    // Getters
    public OWLOntology getOntology() {
    	return this.m_ontology;
    }
    public OWLOntologyManager getOntologyManager() {
    	return this.m_manager;
    }
    
    public Reasoner getReasoner(){
    	return this.m_reasoner;    	
    }
    
    public String getOntologyFilePath() {
    	return this.ontologyFilePath;
    }
}
