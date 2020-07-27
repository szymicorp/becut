package dk.bec.unittest.becut.ui.controller;

import java.io.File;
import java.util.List;

import dk.bec.unittest.becut.compilelist.Parse;
import dk.bec.unittest.becut.compilelist.model.CompileListing;
import dk.bec.unittest.becut.testcase.BecutTestCaseManager;
import dk.bec.unittest.becut.testcase.model.BecutTestCase;
import dk.bec.unittest.becut.ui.model.BECutAppContext;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

public class LoadTestCaseController {
	@FXML
	private TextField testCasePath;
	
	private File testCaseFolder;

	@FXML
	private void browse() {
		DirectoryChooser chooser = new DirectoryChooser();
		testCaseFolder = chooser.showDialog(testCasePath.getScene().getWindow());
		if (testCaseFolder != null) {
			try {
				testCasePath.setText(testCaseFolder.getAbsolutePath());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	@FXML
	private void cancel() {
		testCasePath.getScene().getWindow().hide();
	}
	
	@FXML 
	private void ok() {
		BecutTestCase becutTestCase = BecutTestCaseManager.loadTestCase(testCaseFolder.toPath());
		BECutAppContext.getContext().setUnitTestFolder(testCaseFolder.toPath());
		BECutAppContext.getContext().getUnitTest().setBecutTestCase(becutTestCase);
		testCasePath.getScene().getWindow().hide();
	}
}
