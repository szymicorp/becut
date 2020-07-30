package dk.bec.unittest.becut.ui.controller;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import dk.bec.unittest.becut.compilelist.model.DataType;
import dk.bec.unittest.becut.testcase.model.BecutTestCase;
import dk.bec.unittest.becut.testcase.model.ExternalCall;
import dk.bec.unittest.becut.testcase.model.ExternalCallIteration;
import dk.bec.unittest.becut.testcase.model.Parameter;
import dk.bec.unittest.becut.testcase.model.ParameterLiteral;
import dk.bec.unittest.becut.ui.model.BECutAppContext;
import dk.bec.unittest.becut.ui.model.ExternalCallDisplayable;
import dk.bec.unittest.becut.ui.model.ExternalCallIterationDisplayable;
import dk.bec.unittest.becut.ui.model.FileControlDisplayable;
import dk.bec.unittest.becut.ui.model.ParameterDisplayable;
import dk.bec.unittest.becut.ui.model.PostConditionDisplayable;
import dk.bec.unittest.becut.ui.model.PreConditionDisplayable;
import dk.bec.unittest.becut.ui.model.UnitTest;
import dk.bec.unittest.becut.ui.model.UnitTestTreeObject;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener.Change;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeTableCell;
import javafx.scene.control.TreeTableColumn;
import javafx.scene.control.TreeTableRow;
import javafx.scene.control.TreeTableView;
import javafx.scene.control.TreeTableView.TreeTableViewSelectionModel;
import javafx.scene.control.cell.TextFieldTreeTableCell;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;

public class BecutTestCaseController implements Initializable {

	@FXML
	private TreeTableView<UnitTestTreeObject> unitTestTreeTableView;

	@FXML
	private TreeTableColumn<UnitTestTreeObject, String> name;

	@FXML
	private TreeTableColumn<UnitTestTreeObject, String> type;

	@FXML
	private TreeTableColumn<UnitTestTreeObject, String> value;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		UnitTest currentUnitTest = BECutAppContext.getContext().getUnitTest();
		TreeItem<UnitTestTreeObject> root = new TreeItem<UnitTestTreeObject>(currentUnitTest);
		unitTestTreeTableView.setRoot(root);
		unitTestTreeTableView.setEditable(true);
		name.setCellValueFactory(param -> param.getValue().getValue().nameProperty());
		type.setCellValueFactory(param -> param.getValue().getValue().typeProperty());
		value.setCellValueFactory(param -> {
			return param.getValue().getValue().valueProperty();
		});

		BECutAppContext.getContext().getQueue().addListener((Change<? extends Integer> c) -> {
			if(c.next() && c.wasAdded()) {
				Integer line1 = c.getList().get(c.getList().size() - 1);
				//TODO clear
				//c.getList().clear();
				LinkedList<TreeItem<UnitTestTreeObject>> queue = new LinkedList<>();
				queue.add(unitTestTreeTableView.getRoot());
				while(!queue.isEmpty()) {
					TreeItem<UnitTestTreeObject> node = queue.pop();
					if(node.getValue() instanceof ExternalCallDisplayable) {
						Integer line2 = ((ExternalCallDisplayable)node.getValue()).getExternalCall().getLineNumber();
						if(line1.equals(line2)) {
							node.setExpanded(true);
							TreeItem<UnitTestTreeObject> parent = node.getParent();
							while(parent != null) {
								parent.setExpanded(true);
								parent = parent.getParent();
							}
							unitTestTreeTableView.getSelectionModel().select(node);
							return;
						}
					}
					queue.addAll(node.getChildren());
				}
			}
		});
		
		unitTestTreeTableView.setRowFactory(ttv -> {
			ContextMenu cmExternalCall = new ContextMenu();
			MenuItem dup = new MenuItem("Duplicate");
			MenuItem del = new MenuItem("Delete");
			cmExternalCall.getItems().addAll(dup, del);

			ContextMenu cmFileControl = new ContextMenu();
			MenuItem edit = new MenuItem("Edit");
			cmFileControl.getItems().addAll(edit);
			
			TreeTableRow<UnitTestTreeObject> row = new TreeTableRow<UnitTestTreeObject>() {
			   @Override
			   protected void updateItem(UnitTestTreeObject item, boolean empty) {
				   super.updateItem(item, empty);
				   if(item instanceof ExternalCallIterationDisplayable) {
					   this.setContextMenu(cmExternalCall);
				   } else if (item instanceof FileControlDisplayable) {
					   this.setContextMenu(cmFileControl);
				   } else {
					   //seems that children inherit context menu from parent; it is undesirable here
					   this.setContextMenu(null);
				   }
			   }				
			};

		    dup.setOnAction(event -> {
		    	ExternalCallIteration callIteration = ((ExternalCallIterationDisplayable) row.getItem()).getExternalCallIteration();
		    	ExternalCallIteration copy = ExternalCallIteration.mkCopy(callIteration);

		    	TreeTableViewSelectionModel<UnitTestTreeObject> sm = unitTestTreeTableView.getSelectionModel();
		    	int index = sm.getSelectedIndex();
		    	TreeItem<UnitTestTreeObject> item = sm.getModelItem(index);
		    	TreeItem<UnitTestTreeObject> parent = item.getParent();
		    	
		    	Map<String, ExternalCallIteration> iterations = 
		    			((ExternalCallDisplayable) parent.getValue()).getExternalCall().getIterations();
		    	String name = String.format("iteration_%s", iterations.size());
		    	copy.setName(name);
		    	copy.setNumericalOrder(iterations.size());
		    	iterations.put(name, copy);
	
				populateUnitTestParts(parent, copy);
		    });
			
		    del.setOnAction(event -> {
		    	TreeTableViewSelectionModel<UnitTestTreeObject> sm = unitTestTreeTableView.getSelectionModel();
		    	int index = sm.getSelectedIndex();
		    	TreeItem<UnitTestTreeObject> item = sm.getModelItem(index);
		    	TreeItem<UnitTestTreeObject> parent = item.getParent();
		    	List<TreeItem<UnitTestTreeObject>> items = parent.getChildren();
		    	//TODO when only iteration is removed call should have a context menu for adding one, 
		    	//for now let's just prevent deleting the only one 
		    	if(items.size() > 1) {
		    		parent.getChildren().remove(item);
		    	}
		    	
//		    	ExternalCallDisplayable ecd = (ExternalCallDisplayable) row.getItem();
//		    	currentUnitTest.getBecutTestCase().removeExternalCall(ecd.getExternalCall());
//		    	externalCallHeader.getChildren().remove(row.getItem());
		    });

		    edit.setOnAction(event -> {
		    	TreeTableViewSelectionModel<UnitTestTreeObject> sm = unitTestTreeTableView.getSelectionModel();
		    	int index = sm.getSelectedIndex();
		    	TreeItem<UnitTestTreeObject> item = sm.getModelItem(index);
		    	assert item.getValue() instanceof FileControlDisplayable;
		    	try {
		    		Path path = Paths.get(BECutAppContext.getContext().getUnitTestFolder().toString(), item.getValue().getValue() + ".txt");
		    		if (!Files.exists(path)) {
		    		    Files.createFile(path);
		    		}
					Desktop.getDesktop().open(path.toFile());
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
		    });
		    
		    return row;
		});
		
		value.setCellFactory(
				new Callback<TreeTableColumn<UnitTestTreeObject, String>, TreeTableCell<UnitTestTreeObject, String>>() {
					@Override
					public TreeTableCell<UnitTestTreeObject, String> call(
							TreeTableColumn<UnitTestTreeObject, String> param) {
						return new TextFieldTreeTableCell<UnitTestTreeObject, String>(new DefaultStringConverter()) {
							@Override
							public void updateItem(String item, boolean empty) {
								super.updateItem(item, empty);
								boolean isEditable = true;

								if (getTreeTableRow() != null) {
									UnitTestTreeObject currentItem = getTreeTableRow().getItem();
									// Check if cell should be editable or not
									if (currentItem instanceof ParameterDisplayable) {
										ParameterDisplayable parameterDisplayable = (ParameterDisplayable) getTreeTableRow()
												.getItem();
										if (parameterDisplayable.getParameter() instanceof ParameterLiteral
												|| DataType.EIGHTYEIGHT.equals(parameterDisplayable.getParameter().getDataType())
												|| DataType.GROUP.equals(parameterDisplayable.getParameter().getDataType())
												|| parameterDisplayable.getParameter().getIsSeventySeven()
												|| parameterDisplayable.getParameter().getName().equals("FILLER")
												) {
//									getTreeTableRow().setStyle("-fx-background-color:lightgrey");
											isEditable = false;
										}
									} else if (currentItem == null || currentItem instanceof ExternalCallDisplayable
											|| currentItem instanceof PreConditionDisplayable
											|| currentItem instanceof PostConditionDisplayable
											|| currentItem.getClass().isAnonymousClass()) {
//								getTreeTableRow().setStyle("-fx-background-color:lightgrey");
										isEditable = false;
									} else {
//								getTreeTableRow().setStyle("-fx-background-color:white");
										isEditable = true;
									}

								}
								getTableColumn().setOnEditCommit(event -> {
									event.getRowValue().getValue().updateValue(event.getNewValue());
								});
								setEditable(isEditable);
							};

						};
					}
				});

		currentUnitTest.becutTestCaseProperty().addListener(new ChangeListener<BecutTestCase>() {
			public void changed(ObservableValue<? extends BecutTestCase> observable, BecutTestCase oldValue,
					BecutTestCase newValue) {
				populateRoot(newValue);
			};
		});
	}

	private void populateRoot(BecutTestCase becutTestCase) {
		unitTestTreeTableView.getRoot().getValue().setName("Test Case: " + becutTestCase.getTestCaseName());
		unitTestTreeTableView.getRoot().getValue().setValue(becutTestCase.getTestCaseId());
		unitTestTreeTableView.getRoot().getChildren().clear();

		TreeItem<UnitTestTreeObject> fileControlHeader = new TreeItem<UnitTestTreeObject>(
				new UnitTestTreeObject("File Control", "", "") {
				});
		TreeItem<UnitTestTreeObject> preConditionHeader = new TreeItem<UnitTestTreeObject>(
				new UnitTestTreeObject("Preconditions", "", "") {
				});
		TreeItem<UnitTestTreeObject> externalCallHeader = new TreeItem<UnitTestTreeObject>(
				new UnitTestTreeObject("External Calls", "", "") {
				});
		TreeItem<UnitTestTreeObject> postConditionHeader = new TreeItem<UnitTestTreeObject>(
				new UnitTestTreeObject("Postconditions", "", "") {
				});

		unitTestTreeTableView.getRoot().getChildren().add(fileControlHeader);
		unitTestTreeTableView.getRoot().getChildren().add(preConditionHeader);
		unitTestTreeTableView.getRoot().getChildren().add(externalCallHeader);
		unitTestTreeTableView.getRoot().getChildren().add(postConditionHeader);

		populateUnitTestParts(fileControlHeader, becutTestCase.getFileControlAssignments());
		
		populateUnitTestParts(preConditionHeader, new PreConditionDisplayable("File Section"),
				becutTestCase.getPreCondition().getFileSection());
		populateUnitTestParts(preConditionHeader, new PreConditionDisplayable("Working Storage"),
				becutTestCase.getPreCondition().getWorkingStorage());
		populateUnitTestParts(preConditionHeader, new PreConditionDisplayable("Local Storage"),
				becutTestCase.getPreCondition().getLocalStorage());
		populateUnitTestParts(preConditionHeader, new PreConditionDisplayable("Linkage Section"),
				becutTestCase.getPreCondition().getLinkageSection());

		for (ExternalCall externalCall : becutTestCase.getExternalCalls()) {
			populateUnitTestParts(externalCallHeader, externalCall);
		}

		populateUnitTestParts(postConditionHeader, new PostConditionDisplayable("File Section"),
				becutTestCase.getPostCondition().getFileSection());
		populateUnitTestParts(postConditionHeader, new PostConditionDisplayable("Working Storage"),
				becutTestCase.getPostCondition().getWorkingStorage());
		populateUnitTestParts(postConditionHeader, new PostConditionDisplayable("Local Storage"),
				becutTestCase.getPostCondition().getLocalStorage());
		populateUnitTestParts(postConditionHeader, new PostConditionDisplayable("Linkage Section"),
				becutTestCase.getPostCondition().getLinkageSection());
	}

	private void populateUnitTestParts(TreeItem<UnitTestTreeObject> parent, Map<String, String> getFileControlAssignments) {
		getFileControlAssignments.forEach((k, v) ->
			parent.getChildren().add(
					new TreeItem<UnitTestTreeObject>(new FileControlDisplayable(k, v))));
	}	
	
	private void populateUnitTestParts(TreeItem<UnitTestTreeObject> parent, UnitTestTreeObject treeObject,
			List<Parameter> parameters) {
		TreeItem<UnitTestTreeObject> treeItem = new TreeItem<UnitTestTreeObject>(treeObject);
		for (Parameter parameter : parameters) {
			treeItem.getChildren().add(populateParameters(parameter));
		}
		parent.getChildren().add(treeItem);
	}

	private void populateUnitTestParts(TreeItem<UnitTestTreeObject> parent, ExternalCall externalCall) {
		TreeItem<UnitTestTreeObject> treeItem = new TreeItem<UnitTestTreeObject>(new ExternalCallDisplayable(externalCall));
		for (ExternalCallIteration callIteration : externalCall.getIterations().values()) {
			populateUnitTestParts(treeItem, callIteration);
		}
		parent.getChildren().add(treeItem);
	}

	private void populateUnitTestParts(TreeItem<UnitTestTreeObject> parent, ExternalCallIteration callIteration) {
		TreeItem<UnitTestTreeObject> treeItem = new TreeItem<UnitTestTreeObject>(new ExternalCallIterationDisplayable(callIteration));
		for (Parameter parameter : callIteration.getParameters()) {
			treeItem.getChildren().add(populateParameters(parameter));
		}
		parent.getChildren().add(treeItem);
	}
	
	private TreeItem<UnitTestTreeObject> populateParameters(Parameter parameter) {
		TreeItem<UnitTestTreeObject> treeItem = new TreeItem<UnitTestTreeObject>(new ParameterDisplayable(parameter));
		for (Parameter p : parameter.getSubStructure()) {
			treeItem.getChildren().add(populateParameters(p));
		}
		return treeItem;
	}
}
