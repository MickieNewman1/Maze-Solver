/* *****************************************************************************
 * Title:            MazeRunner
 * Files:            MazeRunner.java
 * Semester:         Spring 2020
 * 
 * Author:           Daniel Szafir, daniel.szafir@colorado.edu
 * 
 * Description:		 The main maze application.
 * 
 * Written:       	 3/21/2021
 **************************************************************************** */

import java.io.File;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.function.UnaryOperator;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextFormatter;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import javafx.scene.control.Separator;

/**
 * This is the main maze application. It creates a JavaFX GUI that enables users to 
 * load, save, or generate maze as well as choose a particular MazeSolver and step
 * through cell-by-cell or hit play to auto-solve the maze.
 * 
 * @author Daniel Szafir
 */
public class MazeRunner extends Application {
	
	// How fast/slow the various maze generation and solving animations
	// play. Lower values will result in faster drawing speeds.
	public static final int DRAWING_SPEED = 5;

	// Application width and height
	public static final int WIDTH = 800;
	public static final int HEIGHT = 600;
	
	// The maze object
	private Maze maze;
	
	// The number of rows and columns for generating new mazes (users can edit these via the GUI)
	private int numRows = Maze.DEFAULT_ROWS, numCols = Maze.DEFAULT_COLUMNS;
	
	// Boolean to keep track of whether the user wants to generate a maze with
	// a guaranteed solution or not when they click the "Generate Maze" button
	private boolean guaranteeMazeSolution;
	
	// Keep track of what type of MazeSolver the user wants to use in solving the maze
	private MazeSolver.Type solverType;
	
	// The MazeSolver object
	private MazeSolver mazeSolver;
	
	// Some GUI elements
	private Button stepButton, playButton, clearButton;
	private ComboBox<MazeSolver.Type> solverComboBox;
	
	// A list of all GUI elements the user can interact with (used to temporarily disable/re-enable
	// user interaction, e.g., for use when generating a new maze)
	private ArrayList<Control> controls;
	
	// Text area for use in printing out status messages to the user
	private TextArea mazeTextArea;
	
	// Used for animating the maze
	private AnimationTimer animator;
	private Object mazeLock;

	// Threads used so that the application knows when the solver is running and/or finished
	private Thread solveThread;
	private Thread solveThreadFinished;
	
	/**
	 * Main entry point of the program. Simply launches the GUI
	 * as a standalone application.
	 *
	 * @param args No command-line arguments expected
	 */
	public static void main(String[] args) {
		launch(args);
	}

	/**
	 * This method is automatically called by JavaFX when the GUI 
	 * starts via the Application.launch call from the main method.
	 * As a result, this is the main entry point for all JavaFX
	 * applications.
	 * 
	 * @param stage The primary stage for this application, 
	 * 		  onto which the application scene can be set. 
	 */
	@Override
	public void start(Stage stage) throws Exception {
		
		// Set up the JavaFX stage
		stage.setTitle("MazeRunner with JavaFX");

		// Top level container
		Group root = new Group();
		
		// Border pane that will go into the root group. Allows us to organize components
		// on the top, center, and bottom of the application
		BorderPane borderPane = new BorderPane();
		
		// Set up a canvas for drawing the maze
		Canvas canvas = new Canvas(WIDTH-20, HEIGHT/2);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.strokeRect(0, 0, canvas.getWidth(), canvas.getHeight()); // draw a 1px black border around the canvas 
		
		// Start the animation timer for animating the maze
		mazeLock = new Object();
		animator = new AnimationTimer() {
			@Override public void handle(long arg0) {
				if (maze != null) maze.draw(gc);
			}
		};
		animator.start();
	
		// Instantiate our list to keep track of all GUI elements the user can interact with
		controls = new ArrayList<Control>();
		
		// Save Maze Button Setup
		Button saveButton = new Button("Save Maze");
		saveButton.setOnAction(actionEvent -> {
			FileChooser fileChooser = new FileChooser();
			fileChooser.setInitialDirectory(new File("."));

			// Set extension filter for text files
			FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("TXT files (*.txt)", "*.txt");
			fileChooser.getExtensionFilters().add(extFilter);

			// Show save file dialog
			File file = fileChooser.showSaveDialog(stage);

			if (file != null) {
				if (maze.save(file)) {
					mazeTextArea.setText("Saved board successfully to " + file.getName());
				} else {
					mazeTextArea.appendText("Error saving board to " + file.getName() + "\n");
				}
			}
		});
		
		// Disable save button at first till a maze is created
		saveButton.setDisable(true);
		
		// Setup Generate Maze Button and Various Generation Options
		Label generationLabel = new Label("Guarantee Solution Exists:");
		ComboBox<String> generationComboBox = new ComboBox<String>(
				FXCollections.observableArrayList(
				"Yes",
				"No"
				));
		generationComboBox.setValue("Yes");
		guaranteeMazeSolution = true;
		
		generationComboBox.setOnAction(actionEvent -> {
			guaranteeMazeSolution = generationComboBox.getValue().equals("Yes") ? true : false;
		});
		generationComboBox.setId("combobox");
		
		Label numRowsLabel = new Label("Num Rows:");
		Label numColsLabel = new Label("Num Cols:");
		
		// Set up spinners for choosing the number of rows and columns and make sure the spinners only
		// accept integer input
		Spinner<Integer> numRowsSpinner = new Spinner<Integer>(Maze.MIN_ROWS, Maze.MAX_ROWS, numRows);
		numRowsSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
			numRows = newValue;
		});
		numRowsSpinner.setEditable(true);
		NumberFormat format = NumberFormat.getIntegerInstance();
		UnaryOperator<TextFormatter.Change> filter = c -> {
		    if (c.isContentChange()) {
		        ParsePosition parsePosition = new ParsePosition(0);
		        // NumberFormat evaluates the beginning of the text
		        format.parse(c.getControlNewText(), parsePosition);
		        if (parsePosition.getIndex() == 0 || parsePosition.getIndex() < c.getControlNewText().length()) {
		            // reject parsing the complete text failed
		            return null;
		        }
		    }
		    return c;
		};
		TextFormatter<Integer> formatter = new TextFormatter<Integer>(new IntegerStringConverter(), numRows, filter);
		numRowsSpinner.getEditor().setTextFormatter(formatter);
		
		numRowsSpinner.setId("spinner");
		HBox rowSpinnerGroup = new HBox(numRowsLabel, numRowsSpinner);
		rowSpinnerGroup.setId("hbox");
		
		Spinner<Integer> numColsSpinner = new Spinner<Integer>(Maze.MIN_COLUMNS, Maze.MAX_COLUMNS, numCols);
		numColsSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
			numCols = newValue;
		});
		numColsSpinner.setEditable(true);
		TextFormatter<Integer> formatter2 = new TextFormatter<Integer>(new IntegerStringConverter(), numCols, filter);
		numColsSpinner.getEditor().setTextFormatter(formatter2);
		numColsSpinner.setId("spinner");
		HBox colSpinnerGroup = new HBox(numColsLabel, numColsSpinner);
		colSpinnerGroup.setId("hbox");
		
		Button generateButton = new Button("Generate New Maze");
		generateButton.setOnAction(actionEvent -> {
			
			Thread mazeGeneratorThread = new Thread() {
				public void run() {
					// Temporarily disable all controls while the maze is being generated
					for (Control c : controls) c.setDisable(true);
					
					mazeTextArea.setText("Generating maze...\n");
					
					maze = new Maze(numRows, numCols, guaranteeMazeSolution, mazeLock);
					
					// Wait for the maze to finish generating
					synchronized(mazeLock) {
						try {
							mazeLock.wait();
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
	
					// Re-enable all controls
					for (Control c : controls) c.setDisable(false);

					mazeTextArea.appendText("Maze generated!");
					
					// Create a new solver for the new maze
					createMazeSolver();
				}
			};
			mazeGeneratorThread.setDaemon(true);
			mazeGeneratorThread.start();
		});
		
		// Load Maze Button Setup
		Button loadButton = new Button("Load Maze");
		loadButton.setOnAction(actionEvent -> {

			FileChooser fileChooser = new FileChooser();
			fileChooser.setInitialDirectory(new File("."));
			File selectedFile = fileChooser.showOpenDialog(stage);

			if (selectedFile != null) {
				mazeTextArea.clear();
				try {
					maze = new Maze(selectedFile);
					mazeTextArea.appendText("File " + selectedFile.getName() + " loaded successfully!\n");
					
					// Enable the save button
					saveButton.setDisable(false);
					
					// Create a new solver for the new maze
					createMazeSolver();
					
				} catch (Exception e) {
					mazeTextArea.appendText("Error - could not load file " + selectedFile.getName() + ":\n");
					mazeTextArea.appendText(e.getMessage());
				}
			}
		});
		
		// Clear Maze Button Setup
		clearButton = new Button("Clear Maze");
		clearButton.setOnAction(actionEvent -> {
			mazeSolver = null;
			maze.clear();
			mazeTextArea.setText("Maze cleared");
		});
		
		// Disable clear button at first till a maze is created
		clearButton.setDisable(true);
		
		// Choose Solver Type Setup
		Label solverLabel = new Label("Solution Method:");
		solverComboBox = new ComboBox<MazeSolver.Type>(
				FXCollections.observableArrayList(
						MazeSolver.Type.values()
				));
//		solverComboBox.getItems().setAll(MazeSolver.Type.values());
		solverComboBox.setValue(MazeSolver.Type.STACK);
		solverType = solverComboBox.getValue();
		
		// When the solver type is changed, clear the maze and create a new solver based on the new type
		solverComboBox.setOnAction(actionEvent -> {
			solverType = solverComboBox.getValue();
			clearButton.fire();
			createMazeSolver();
		});	
		
		solverComboBox.setId("combobox");
		
		// Disable solver selection at first till a maze is created
		solverComboBox.setDisable(true);
		
		// Step Button Setup
		stepButton = new Button("Step");
		stepButton.setOnAction(actionEvent -> {
			if (maze != null) { // No need to do anything if the maze doesn't exist
				if (mazeSolver == null) { // If we don't have a solver, need to create one
					createMazeSolver();
				}
				
				switch (mazeSolver.getStatus())
				{		
					case UNSOLVED: 	  if (mazeSolver.step()) {
										mazeTextArea.setText("Maze Solved!\n");
										mazeTextArea.appendText("Found goal after visiting "+ mazeSolver.getCellsVisited() + " cells");
									  }
									  else {
										mazeTextArea.setText("Solving...\n");
										mazeTextArea.appendText("So far we have visited "+ mazeSolver.getCellsVisited() + " cells");
									  }
					break;
					
					case SOLVED: 	  mazeTextArea.setText("Maze Solved!\n");
									  mazeTextArea.appendText("Found goal after visiting "+ mazeSolver.getCellsVisited() + " cells");
					break;
						
					case NO_SOLUTION: mazeTextArea.setText("Maze is impossible to solve!\n");
									  mazeTextArea.appendText("Tried visiting "+ mazeSolver.getCellsVisited() + " cells");
					break;
				
				}
			}
		});
		
		// Disable Step Button at first till a maze is created
		stepButton.setDisable(true);
		
		// Play Button Setup
		playButton = new Button("Play");
		playButton.setOnAction(actionEvent -> {
			if (maze != null) {
				if (playButton.getText().equals("Play")) { // if we aren't already playing
					
					// If we have an old thread waiting for a solver to finish, then kill it
					if (solveThreadFinished != null && solveThreadFinished.isAlive()) {
						solveThreadFinished.interrupt();
						try {
							solveThreadFinished.join();
						} catch (InterruptedException e) {
							//e.printStackTrace(); //do nothing
						}
					}
					
					// If we have an old solver currently running, then kill it
					if (solveThread != null && solveThread.isAlive()) {
						solveThread.interrupt();
						try {
							solveThread.join();
						} catch (InterruptedException e) {
							//e.printStackTrace(); //do nothing
						}
					}
					
					// Ensure we have a solver
					if (mazeSolver == null) {
						createMazeSolver();
					}
					
					// Disable all interactive controls while solving (except the pause button)
					for (Control c : controls) c.setDisable(true);
					playButton.setText("Pause");
					playButton.setDisable(false);
					
					mazeTextArea.setText("Solving...\n");
		
					// Run the solve thread
					solveThread = new Thread() {
						@Override
						public void run() {
							mazeSolver.solve();
						}
					};
					solveThread.setDaemon(true);
					solveThread.start();
					
					// Create a thread as a callback that waits for the solver to finish and then updates the gui
					solveThreadFinished = new Thread() {
						public void run() {
							try {
								solveThread.join();
								if (mazeSolver.getStatus() == MazeSolver.Status.SOLVED) {
									mazeTextArea.setText("Maze Solved!\n");
									mazeTextArea.appendText("Found goal after visiting "+ mazeSolver.getCellsVisited() + " cells");
								}
								else if (mazeSolver.getStatus() == MazeSolver.Status.NO_SOLUTION) {
									mazeTextArea.setText("Maze is impossible to solve!\n");
									mazeTextArea.appendText("Tried visiting "+ mazeSolver.getCellsVisited() + " cells");
								}
								
								// If the user paused the solving process or the solver finished, re-enable
								// controls and give them the option to continue playing if they want
								if (playButton.getText().equals("Pause")) {
									Platform.runLater(new Runnable(){ 
										@Override public void run() { 
											playButton.setText("Play");
											
											// Re-enable all interactive controls
											for (Control c : controls) c.setDisable(false);
										}
									});
								}
							} catch (InterruptedException e) {
								return;
							}
						}
					};
					solveThreadFinished.setDaemon(true);
					solveThreadFinished.start();
					
				}
				else { // if we are currently playing, pause the solver and re-enable all controls
					
					// If we have an old thread waiting for the solver to finish, then kill it
					if (solveThreadFinished != null && solveThreadFinished.isAlive()) {
						solveThreadFinished.interrupt();
						try {
							solveThreadFinished.join();
						} catch (InterruptedException e) {
							//e.printStackTrace(); //do nothing
						}
					}
					
					// Pause the solving thread
					if (solveThread != null && solveThread.isAlive()) {
						solveThread.interrupt();
					
						try {
							solveThread.join();
						} catch (InterruptedException e) {
							//e.printStackTrace(); //do nothing
						}
					}
					
					mazeTextArea.setText("Paused!\n");
					mazeTextArea.appendText("So far solver has visited "+ mazeSolver.getCellsVisited() + " cells");
					
					playButton.setText("Play");
					
					// Re-enable all interactive controls
					for (Control c : controls) c.setDisable(false);
				}
			}
		});
		
		// Disable play button at first till a maze is created
		playButton.setDisable(true);
		
		// Add all the elements the user can interact with to the list of controls
		controls.add(generateButton);
		controls.add(generationComboBox);
		controls.add(loadButton);
		controls.add(saveButton);
		controls.add(clearButton);
		controls.add(stepButton);
		controls.add(playButton);
		controls.add(solverComboBox);
		controls.add(numRowsSpinner);
		controls.add(numColsSpinner);
		
		// Set up a grid to hold all the groups of buttons/labels/comboboxes along the top of the application
		GridPane topGrid = new GridPane();
		topGrid.setId("topGrid");
		
		// Top grid row (elements relevant to Maze Generation):
		topGrid.add(generateButton, 0, 0);
		topGrid.add(generationLabel, 1, 0);
		topGrid.add(generationComboBox, 2, 0);
		topGrid.add(rowSpinnerGroup, 3, 0);
		topGrid.add(colSpinnerGroup, 4, 0);
		
		Separator separator = new Separator(Orientation.HORIZONTAL);
		GridPane.setColumnSpan(separator, 5);
		topGrid.add(separator, 0, 1);
		
		// Second row (elements relevant to Loading, Saving, and Clearing the Maze):
		topGrid.add(loadButton, 0, 2);
		topGrid.add(saveButton, 1, 2);
		topGrid.add(clearButton, 2, 2);		
		
		Separator separator2 = new Separator(Orientation.HORIZONTAL);
		GridPane.setColumnSpan(separator2, 5);
		topGrid.add(separator2, 0, 3);
		
		// Third row (elements relevant to Maze Solving)
		topGrid.add(stepButton, 0, 4);
		topGrid.add(playButton, 1, 4);
		topGrid.add(solverLabel, 2, 4);
		topGrid.add(solverComboBox, 3, 4);

		// Add the grid to the top of the application
		borderPane.setTop(topGrid);
		Insets borderMargin = new Insets(10);
		BorderPane.setMargin(topGrid, borderMargin);
		
		// Add the canvas to the center of the application
		borderPane.setCenter(canvas);
		BorderPane.setMargin(canvas, borderMargin);
		
		// Add the text area in a scroll pane to the bottom of the application
		mazeTextArea = new TextArea();
		mazeTextArea.setEditable(false);
		mazeTextArea.setId("textarea");
		mazeTextArea.setText("Welcome to MazeRunner!\nClick \"Generate New Maze\" or \"Load Maze\" to get started.");
		ScrollPane textScrollPane = new ScrollPane();
		textScrollPane.setContent(mazeTextArea);
		textScrollPane.setHbarPolicy(ScrollBarPolicy.AS_NEEDED);
		textScrollPane.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
		textScrollPane.setPannable(true);
		textScrollPane.setFitToWidth(true);
		borderPane.setBottom(textScrollPane);
		BorderPane.setMargin(textScrollPane, borderMargin);
		
		// Package everything up and show the scene
		root.getChildren().add(borderPane);
		Scene scene = new Scene(root, WIDTH, HEIGHT);
		scene.getStylesheets().add("mazerunner.css");
		stage.setScene(scene);
		stage.show();
		stage.setResizable(false);
	}
	
	/**
	 * Utility method that creates a MazeSolver based on the current user specifications from the GUI.
	 * This also ensures that all the GUI elements related to interacting with the solver are enabled.
	 */
	private void createMazeSolver()
	{
		if (maze != null) {
			switch(solverType)
			{
			case STACK: mazeSolver = new StackSolver(maze);
			break;
			
			case QUEUE: mazeSolver = new QueueSolver(maze);
			break;
			
			default: mazeSolver = new QueueSolver(maze); //shouldn't happen, but default to queue solver
			break;
			}
			
			stepButton.setDisable(false);
			playButton.setDisable(false);
			solverComboBox.setDisable(false);
			clearButton.setDisable(false);
		}
	}
}
