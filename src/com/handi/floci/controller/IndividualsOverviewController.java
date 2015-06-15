package com.handi.floci.controller;

import java.util.Iterator;

import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.Dialogs;
import org.semanticweb.owlapi.io.StreamDocumentTarget;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyAssertionAxiom;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.OWLPropertyAxiom;

import com.handi.floci.modules.conceptclassification.HierarchyGenerator;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;

public class IndividualsOverviewController {
	private MainController parent;
	private HierarchyGenerator hierarchyGenerator;
	private OWLNamedIndividual namedIndividual;
	private boolean checkBoxHack = true;
	
	
	@FXML private ListView<OWLObjectProperty> propertiesList;
    @FXML private ListView<OWLDataProperty> attributesList;
    @FXML private ListView<String> domainList; 
    @FXML private ListView<String> rangeList; 
    
    @FXML private HBox existingIndividualHBox;
    @FXML private ComboBox<OWLNamedIndividual> existingIndividualCombo;
    
    @FXML private HBox valueHBox;
    @FXML private TextField valueField;
    @FXML private HBox individuCibleHBox;
    @FXML private ComboBox<OWLNamedIndividual> individuCibleCombo;
    
    @FXML private ListView<OWLAxiom> axiomsList;
    
    @FXML private CheckBox checkInconnu;
    
    public IndividualsOverviewController() {}
    
    @FXML private void initialize() {
    	domainList.setMouseTransparent(true);
    	domainList.setFocusTraversable(false);
    	rangeList.setMouseTransparent(true);
    	rangeList.setFocusTraversable(false);
    	
    	attributesList.setCellFactory(new Callback<ListView<OWLDataProperty>, ListCell<OWLDataProperty>>(){
			@Override
			public ListCell<OWLDataProperty> call(ListView<OWLDataProperty> param) {
				ListCell<OWLDataProperty> cell = new ListCell<OWLDataProperty>(){ 
                    @Override
                    protected void updateItem(OWLDataProperty attribut, boolean bln) {
                        super.updateItem(attribut, bln);
                        if (attribut != null) {
                            setText(attribut.getIRI().getFragment());
                        }
                    }
                }; 
                return cell;
			}
    	});
    	propertiesList.setCellFactory(new Callback<ListView<OWLObjectProperty>, ListCell<OWLObjectProperty>>(){
			@Override
			public ListCell<OWLObjectProperty> call(ListView<OWLObjectProperty> param) {
				ListCell<OWLObjectProperty> cell = new ListCell<OWLObjectProperty>(){ 
                    @Override
                    protected void updateItem(OWLObjectProperty property, boolean bln) {
                        super.updateItem(property, bln);
                        if (property != null) {
                            setText(property.getIRI().getFragment());
                        }
                    }
                }; 
                return cell;
			}
    	});
    	
    	individuCibleCombo.setCellFactory(new Callback<ListView<OWLNamedIndividual>, ListCell<OWLNamedIndividual>>(){
			@Override
			public ListCell<OWLNamedIndividual> call(ListView<OWLNamedIndividual> param) {
				ListCell<OWLNamedIndividual> cell = new ListCell<OWLNamedIndividual>(){ 
                    @Override
                    protected void updateItem(OWLNamedIndividual individu, boolean bln) {
                        super.updateItem(individu, bln);
                        if (individu != null) {
                            setText(individu.getIRI().getFragment());
                        }
                    }
                }; 
                return cell;
			}
    	});
    	individuCibleCombo.setButtonCell(new ListCell<OWLNamedIndividual>(){ 
            @Override
            protected void updateItem(OWLNamedIndividual individu, boolean bln) {
                super.updateItem(individu, bln);
                if (individu != null) {
                    setText(individu.getIRI().getFragment());
                }
            }
        });
    	
    	existingIndividualCombo.setCellFactory(new Callback<ListView<OWLNamedIndividual>, ListCell<OWLNamedIndividual>>(){
			@Override
			public ListCell<OWLNamedIndividual> call(ListView<OWLNamedIndividual> param) {
				ListCell<OWLNamedIndividual> cell = new ListCell<OWLNamedIndividual>(){ 
                    @Override
                    protected void updateItem(OWLNamedIndividual individu, boolean bln) {
                        super.updateItem(individu, bln);
                        if (individu != null) {
                            setText(individu.getIRI().getFragment());
                        }
                    }
                }; 
                return cell;
			}
    	});
    	existingIndividualCombo.setButtonCell(new ListCell<OWLNamedIndividual>(){ 
            @Override
            protected void updateItem(OWLNamedIndividual individu, boolean bln) {
                super.updateItem(individu, bln);
                if (individu != null) {
                    setText(individu.getIRI().getFragment());
                }
            }
        });
    	
    	existingIndividualCombo.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<OWLNamedIndividual>() {
			@Override
			public void changed(ObservableValue<? extends OWLNamedIndividual> observable,
					OWLNamedIndividual oldValue, OWLNamedIndividual newValue) {
				if(null == newValue) {
					return;
				}
				namedIndividual = newValue;
				showIndividualAxioms();
			}
    	});
    	
    	axiomsList.setCellFactory(new Callback<ListView<OWLAxiom>, ListCell<OWLAxiom>>(){
			@Override
			public ListCell<OWLAxiom> call(ListView<OWLAxiom> param) {
				ListCell<OWLAxiom> cell = new ListCell<OWLAxiom>(){ 
                    @Override
                    protected void updateItem(OWLAxiom axiom, boolean bln) {
                        super.updateItem(axiom, bln);
                        if (axiom != null) {
                        	String output = "";
                        	if(axiom.getAxiomType().toString().equals("ObjectPropertyAssertion")) {
                        		OWLObjectPropertyAssertionAxiom dataAxiom = (OWLObjectPropertyAssertionAxiom) axiom;
                    			OWLObjectProperty property = dataAxiom.getProperty().asOWLObjectProperty();
                    			OWLNamedIndividual target = dataAxiom.getObject().asOWLNamedIndividual();
                    			
                    			output = property.getIRI().getFragment() + "(" + target.getIRI().getFragment() + ")";
                        	} else {
                        		OWLDataPropertyAssertionAxiom dataAxiom = (OWLDataPropertyAssertionAxiom) axiom;
                    			OWLDataProperty attribut = dataAxiom.getProperty().asOWLDataProperty();                    			
                    			String value = dataAxiom.getObject().getLiteral();
                    			if(value.equals("1.0E9") || value.equals("1E9") || value.equals("1000000000") || value.equals("1000000000.0"))
                    				value = "Inconnu";
                    			output = attribut.getIRI().getFragment() + "(" + value + ")";
                        	}
                            setText(output);
                        }
                        else { // empty the cell if it has been reused
                        	setText(null);
                        }
                    }
                }; 
                return cell;
			}
    	});
    }
    
    public void setHierarchyGenerator(HierarchyGenerator hierarchyGenerator) {
    	this.hierarchyGenerator = hierarchyGenerator;
    }
    public void setNamedIndividual(OWLNamedIndividual namedIndividual) {
    	this.namedIndividual = namedIndividual;
    }
    
    public void setParent(MainController main) {
    	this.parent = main;
    }
    
    public void showAttributes() {
    	this.hierarchyGenerator.getAllAttributes(attributesList);
    	attributesList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<OWLDataProperty>() {
			@Override
			public void changed(ObservableValue<? extends OWLDataProperty> observable,
					OWLDataProperty oldValue, OWLDataProperty newValue) {
				if(null == newValue) {
					return;
				}
				// Disable the individu cible controls
				individuCibleCombo.getSelectionModel().clearSelection();
				individuCibleHBox.setDisable(true);
				individuCibleHBox.setVisible(false);
				propertiesList.getSelectionModel().clearSelection();
				// Enable the attribut value controls
				valueHBox.setDisable(false);
				valueHBox.setVisible(true);
				
				// Show the domains
				domainList.getItems().clear();
				for ( OWLClassExpression domain : newValue.getDomains(hierarchyGenerator.getOntology())) {
					if(null != domain.asOWLClass()) {
						domainList.getItems().add(domain.asOWLClass().getIRI().getFragment());
					}
				}
				// If no domain specified, show "Thing"
				if(domainList.getItems().isEmpty()) 
					domainList.getItems().add("Thing");
				// Show the ranges
				rangeList.getItems().clear();
				for ( OWLDataRange range : newValue.getRanges(hierarchyGenerator.getOntology())) {
					if(null != range) {
						String rangeString = range.toString();
						if (rangeString.contains("double"))
							rangeList.getItems().add("Double");
						else if(rangeString.contains("float"))
							rangeList.getItems().add("Float");
						else if(rangeString.contains("int"))
							rangeList.getItems().add("Entier");
						else
							rangeList.getItems().add("String");
					}
				}
				// If no range specified, show "Thing"
				if(rangeList.getItems().isEmpty()) 
					rangeList.getItems().add("Thing");
			}
    	});
    }
    
    public void showProperties() {
    	this.hierarchyGenerator.getAllProperties(propertiesList);
    	propertiesList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<OWLObjectProperty>() {
			@Override
			public void changed(ObservableValue<? extends OWLObjectProperty> observable,
					OWLObjectProperty oldValue, OWLObjectProperty newValue) {
				if(null == newValue) {
					return;
				}
				// Enable the individu cible controls
				individuCibleHBox.setDisable(false);
				individuCibleHBox.setVisible(true);
				attributesList.getSelectionModel().clearSelection();
				// Disable the attribut value controls
				valueField.setText("");
				valueHBox.setDisable(true);
				valueHBox.setVisible(false);
				
				// Show the domains
				domainList.getItems().clear();
				for ( OWLClassExpression domain : newValue.getDomains(hierarchyGenerator.getOntology())) {
					if(null != domain.asOWLClass()) {
						domainList.getItems().add(domain.asOWLClass().getIRI().getFragment());
					}
				}
				// If no domain specified, show "Thing"
				if(domainList.getItems().isEmpty()) 
					domainList.getItems().add("Thing");
				// Show the ranges
				rangeList.getItems().clear();
				for ( OWLClassExpression range : newValue.getRanges(hierarchyGenerator.getOntology())) {
					if(null != range) {
						rangeList.getItems().add(range.asOWLClass().getIRI().getFragment());
					}
				}
				// If no range specified, show "Thing"
				if(rangeList.getItems().isEmpty()) 
					rangeList.getItems().add("Thing");
			}
    	});
    }
    
    public void showIndividuals() {
    	ObservableList<OWLNamedIndividual> namedIndividuals = this.hierarchyGenerator.getAllIndividuals();
    	existingIndividualCombo.setItems(namedIndividuals);
    	existingIndividualCombo.getSelectionModel().select(this.namedIndividual);
    	individuCibleCombo.setItems(namedIndividuals);	
    }
    
    public void showIndividualAxioms() {
    	axiomsList.getItems().clear();
    	for(OWLAxiom axiom : this.hierarchyGenerator.getOntology().getAxioms(namedIndividual)) {
    		String type = axiom.getAxiomType().toString();
    		if(type.equals("ObjectPropertyAssertion") || type.equals("DataPropertyAssertion")) {
    			axiomsList.getItems().add(axiom);
    		}
    	}
    	
    }
    
    @SuppressWarnings("deprecation")
	@FXML protected void clickAddAxiom(ActionEvent event) {
    	// get a reference to the manager and factory
    	OWLOntologyManager manager = hierarchyGenerator.getOntologyManager();
    	OWLDataFactory factory = manager.getOWLDataFactory();
    	
    	if(valueHBox.isVisible()) { // Attribut
    		OWLDataProperty attribut = attributesList.getSelectionModel().getSelectedItem();
    		boolean dontAdd = true;
    		boolean notExist = true;
    		for(OWLAxiom axiom : this.hierarchyGenerator.getOntology().getAxioms(namedIndividual)) {
        		String type = axiom.getAxiomType().toString();
        		if(type.equals("DataPropertyAssertion")) {
        			OWLDataPropertyAssertionAxiom dataAxiom = (OWLDataPropertyAssertionAxiom) axiom;
        			OWLDataProperty attributExistant = dataAxiom.getProperty().asOWLDataProperty();
        			if(attribut.getIRI() == attributExistant.getIRI()) {
        				notExist = false;
        				Action response = Dialogs.create()
        		    	        .owner(((Node)event.getSource()).getScene().getWindow())
        		    	        .title("Confirmer la modification")
        		    	        .masthead("Une Valeur pour cet Attribut existe déja!")
        		    	        .message("Voulez vous modifier la valeur de cet Attribut?")
        		    	        .actions(Dialog.ACTION_OK, Dialog.ACTION_CANCEL)
        		    	        .showConfirm();

        		    	if (response == Dialog.ACTION_OK) {
        		        		manager.removeAxiom(hierarchyGenerator.getOntology(), axiom);
        		        		dontAdd = false;
        		    	}
        			}
        		}
        	}
    		if(!dontAdd || notExist) {
	    		String type = rangeList.getItems().get(0);
	    		
	    		if(type.equals("Double") || type.equals("Float")) {
	    			try {
	    				double doubleValue;
	    				if(valueField.isDisabled()) doubleValue = 1000000000; 
	    				else doubleValue = Double.parseDouble(valueField.getText());
	    				
	    				OWLLiteral value = factory.getOWLLiteral(doubleValue);
	    				OWLDataPropertyAssertionAxiom dataPropertyAssertion = factory.
	    	    				getOWLDataPropertyAssertionAxiom(attribut, namedIndividual, value);
	    				
	    				// add the axiom to the ontology
	    				manager.addAxiom(hierarchyGenerator.getOntology(), dataPropertyAssertion);
	    				
	    				// reafficher la liste d'axioms de l'individu
	    				showIndividualAxioms();
	    				
	    			} catch(Exception e) {
	    				Dialogs.create()
	                    .owner(((Node)event.getSource()).getScene().getWindow())
	                    .title("Erreur")
	                    .masthead(null)
	                    .message("Ooops, La valeur de l'attribut " + attribut.getIRI().getFragment() + " doit être un Double!")
	                    .showError();
	    			}
	    		} else if (type.equals("Entier")) {
	    			try {
	    				int integerValue;
	    				if(valueField.isDisabled()) integerValue = 1000000000; 
	    				else integerValue = Integer.parseInt(valueField.getText());
	    				
	    				OWLLiteral value = factory.getOWLLiteral(integerValue);
	    				OWLDataPropertyAssertionAxiom dataPropertyAssertion = factory.
	    	    				getOWLDataPropertyAssertionAxiom(attribut, namedIndividual, value);
	    				
	    				// add the axiom to the ontology
	    				manager.addAxiom(hierarchyGenerator.getOntology(), dataPropertyAssertion);
	    				
	    				// reafficher la liste d'axioms de l'individu
	    				showIndividualAxioms();
	    				
	    			} catch(Exception e) {
	    				Dialogs.create()
	                    .owner(((Node)event.getSource()).getScene().getWindow())
	                    .title("Erreur")
	                    .masthead(null)
	                    .message("Ooops, La valeur de l'attribut " + attribut.getIRI().getFragment() + " doit être un Entier!")
	                    .showError();
	    			}
	    		} else {
	    			String stringValue;
	    			if(valueField.isDisabled()) stringValue = " "; 
    				else stringValue = valueField.getText();
	    			
					OWLLiteral value = factory.getOWLLiteral(stringValue);
					OWLDataPropertyAssertionAxiom dataPropertyAssertion = factory.
		    				getOWLDataPropertyAssertionAxiom(attribut, namedIndividual, value);
					
					// add the axiom to the ontology
					manager.addAxiom(hierarchyGenerator.getOntology(), dataPropertyAssertion);
					
					// reafficher la liste d'axioms de l'individu
					showIndividualAxioms();
	    		}
	    		valueField.clear();
    		}
    	} else {
    		OWLObjectProperty property = propertiesList.getSelectionModel().getSelectedItem();
    		OWLNamedIndividual cible = individuCibleCombo.getSelectionModel().getSelectedItem();
    		if(null != cible) {
	    		// Create the Axiom:
	    		OWLObjectPropertyAssertionAxiom propertyAssertion = hierarchyGenerator.getOntologyManager().getOWLDataFactory()
	    		                .getOWLObjectPropertyAssertionAxiom(property, namedIndividual, cible);
	    		
	    		// add the axiom to the ontology
				hierarchyGenerator.getOntologyManager().addAxiom(hierarchyGenerator.getOntology(), propertyAssertion);
				
				// reafficher la liste d'axioms de l'individu
				showIndividualAxioms();
				
    		} else { // cible vide!
    			Dialogs.create()
                .owner(((Node)event.getSource()).getScene().getWindow())
                .title("Erreur")
                .masthead(null)
                .message("Ooops, Aucun individu cible n'est sélectionné!")
                .showError();
    		}
    	}
    }
    
    @SuppressWarnings("deprecation")
    @FXML protected void clickRemoveAxiom(ActionEvent event) {
		Action response = Dialogs.create()
    	        .owner(((Node)event.getSource()).getScene().getWindow())
    	        .title("Confirmer la suppression")
    	        .masthead("La suppression d'un Axiom est irrévocable!")
    	        .message("Etes vous sur de vouloir supprimer cet Axiom?")
    	        .actions(Dialog.ACTION_OK, Dialog.ACTION_CANCEL)
    	        .showConfirm();

    	if (response == Dialog.ACTION_OK) {
    		OWLAxiom axiom = axiomsList.getSelectionModel().getSelectedItem();
        	if(null != axiom) {
        		hierarchyGenerator.getOntologyManager().removeAxiom(hierarchyGenerator.getOntology(), axiom);
        	}
        	showIndividualAxioms();
    	}
    	
    }
    
    @FXML protected void saveOntologyToFile(ActionEvent event) {
    	try {
			hierarchyGenerator.getOntologyManager().saveOntology(hierarchyGenerator.getOntology());
			
			hierarchyGenerator.reload();
			
			parent.kb = null;
			parent.showIndividuals();
			
			// close the window
			Node  source = (Node)  event.getSource(); 
		    Stage stage  = (Stage) source.getScene().getWindow();
		    stage.close();
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
    }

    @FXML protected void checkInconnu(ActionEvent event) {
    	if(checkBoxHack) checkBoxHack = false;
    	else valueField.setDisable(checkInconnu.isSelected());
    }
}
