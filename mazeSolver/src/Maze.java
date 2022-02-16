/* *****************************************************************************
 * Title:            Maze
 * Files:            Maze.java
 * Semester:         Spring 2021
 * 
 * Author:           Daniel Szafir, daniel.szafir@colorado.edu
 * 
 * Description:		 A Maze consists of a 2D array of Cells
 * 
 * Written:       	 3/21/2020
 **************************************************************************** */

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Random;

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * A Maze is a 2D array of Cells.
 * 
 * @author Daniel Szafir
 */
public class Maze {
	
	/**
	 * Utility enum for representing the various directions in a maze (i.e., no diagonals)
	 * 
	 * @author Daniel Szafir
	 */
	public static enum Direction {
		RIGHT, LEFT, UP, DOWN;
	}

	// Default, max, and min values for maze size
	public static final int DEFAULT_ROWS = 20;
	public static final int DEFAULT_COLUMNS = 20;
	public static final int MIN_ROWS = 10;
	public static final int MIN_COLUMNS = 10;
	public static final int MAX_ROWS = 100;
	public static final int MAX_COLUMNS = 100;
	
	// The number of rows and columns of this maze
	private int rows, columns;
	
	// The core maze data structure: a 2D array of cells
	private Cell[][] cells;
	
	// Keep track of the starting cell (a MazeSolver will need this)
	private Cell startCell;
	
	// If generating a maze and ensuring it is solveable, this tracks
	// the maximum recursion depth of the calls to openCell so we can mark the goal cell
	private int maxRecursionDepthSeen; 
	
	// If generating a maze and ensuring it is solveable, this allows us to mark the goal cell
	private Point2D goalCellLocation;

	/**
	 * Create a new maze with a given number of rows and columns. Provides an option regarding whether 
	 * the generated maze is guaranteed to have a solution or not.
	 * 
	 * @param rows The number of rows for the maze
	 * @param columns The number of columns for the maze
	 * @param ensureSolveable Whether or not to ensure the maze has a solution
	 * @param mazeLock A lock to keep track of when the maze has finished generating
	 */
	public Maze(int rows, int columns, boolean ensureSolveable, Object mazeLock)
	{
		if (rows < MIN_ROWS) throw new IllegalArgumentException("Error - specified rows" + rows + " is less than "
				+ "minimum allowable number of rows " + MIN_ROWS);
		if (rows > MAX_ROWS) throw new IllegalArgumentException("Error - specified rows" + rows + " is greater than "
				+ "maximum allowable number of rows " + MAX_ROWS);
		if (columns < MIN_COLUMNS) throw new IllegalArgumentException("Error - specified columns" + columns + " is less than "
				+ "minimum allowable number of columns " + MIN_COLUMNS);
		if (columns > MAX_COLUMNS) throw new IllegalArgumentException("Error - specified columns" + columns + " is greater than "
				+ "maximum allowable number of columns " + MAX_COLUMNS);
		
		this.rows = rows;
		this.columns = columns;
		cells = new Cell[rows][columns];
		
		// Start with everything a wall
		for (int i=0; i<rows; ++i)
			for (int j=0; j<columns; ++j)
				cells[i][j] = new Cell(Cell.Type.WALL, new Point2D(j, i));

		// Carve out the maze based on whether the user requests to ensure there is a solution or not
		Thread t = new Thread() {
			public void run() {
				if (ensureSolveable) generateSolveableMaze();
				else generateRandomMaze();
				
				// Notify any waiting threads that we have finished generating the maze
				synchronized(mazeLock) {
					mazeLock.notifyAll();
				}
			}
		};
		t.setDaemon(true);
		t.start();
	}

	/**
	 * Generate a Maze from a file
	 * 
	 * @param file The file containing the maze. This file must provide the number of columns and rows on the
	 * 			   first line and then each subsequent line a series of characters encoding each cell of the maze
	 * 
	 * @throws IOException If an issue occurs loading the maze from the file
	 */
	public Maze(File file) throws IOException
	{
		if (file == null) throw new IllegalArgumentException("Error - cannot create maze, "
				+ "null file specified");
	
		BufferedReader inStream = new BufferedReader(new FileReader(file));
	
		try {
	
			// Get number of columns and rows from the first line of the file
			String line = inStream.readLine(); 
			String[] colsAndRows = line.split(" ");
	
			if (colsAndRows.length != 2) throw new IllegalArgumentException("Error - cannot parse " +
					"file "+ file + " - should start with columns and rows specification");
	
			try {
				columns = Integer.parseInt(colsAndRows[0]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Error - cannot parse " +
						"file "+ file + " - expecting number of columns, got " + colsAndRows[0]);
			}
			try {
				rows = Integer.parseInt(colsAndRows[1]);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("Error - cannot parse " +
						"file "+ file + " - expecting number of rows, got " + colsAndRows[1]);
			}
	
	
			// Read in each line of the file and create
			// the corresponding row of the maze, making sure that only
			// one start and one goal position are specified
			cells = new Cell[rows][columns];
			int row = 0;
			boolean startExists = false, goalExists = false;
			while ((line = inStream.readLine()) != null) {
				line = line.trim();
				for(int i = 0; i < line.length(); i++) {
					Cell square = new Cell(line.charAt(i), new Point2D(i, row));
	
					if (square.getType() == Cell.Type.START) {
						if (startExists) {
							throw new IllegalArgumentException("Error - cannot parse "
									+ "file " + file + " - more than one starting location specified.");
						}
						startExists = true;
						square.setType(Cell.Type.START);
						startCell = square;
					}
	
					if (square.getType() == Cell.Type.GOAL) {
						if (goalExists) {
							throw new IllegalArgumentException("Error - cannot parse "
									+ "file " + file + " - more than one goal location specified.");
						}
						goalExists = true;
						square.setType(Cell.Type.GOAL);
						goalCellLocation = square.getCoordinates();
					}
	
					cells[row][i] = square;
				}
				row++;
			}
			
			if (!startExists) throw new IllegalArgumentException("Error - cannot parse "
					+ "file " + file + " - no starting location specified.");
			
			if (!goalExists) throw new IllegalArgumentException("Error - cannot parse "
					+ "file " + file + " - no goal location specified.");
	
		} catch (Exception e) { throw e; } // pass the exception along if any arose
	
		finally { inStream.close(); } // closes the file
	}

	/**
	 * Generate a maze with a guaranteed solution by generating a random starting location
	 * and recursively carving out the maze from there.
	 */
	private void generateSolveableMaze()
	{
		// Choose a random start location that has a buffer
		// of at least 2 cells from the border in any direction
		// (i.e., range of starting row = [2, #rows-2], starting col = [2, #cols-2])
		Random rand = new Random();
		int startRow = rand.nextInt(rows-4) + 2;
		int startCol = rand.nextInt(columns-4) + 2;
		
		// Set the starting position
		startCell = new Cell(Cell.Type.START, new Point2D(startCol, startRow));
		cells[startRow][startCol] = startCell;

		// Generate the maze recursively, starting with the start position
		openCell(startRow, startCol, 0);

		// Set the goal location based whatever path had the greatest recursion depth
		cells[(int)goalCellLocation.getY()][(int)goalCellLocation.getX()] = 
				new Cell(Cell.Type.GOAL, goalCellLocation);
	}

	/**
	 * "Opens" (i.e., turns a wall cell to an open cell) a single cell and then 
	 * checks each neighbor of that cell and recursively opens it too if the neighboring cell
	 * has 3 wall cells around it. This allows us to recursively carve out a maze.
	 * 
	 * @param row The row of the cell to open
	 * @param col The column of the cell to open
	 * @param currentRecursionDepth Our current recursion depth (used to set the goal cell)
	 */
	private void openCell(int row, int col, int currentRecursionDepth) {
		
		// If this is the most recursion depth we've had so far, mark this cell to be the goal
		currentRecursionDepth++;
		if (currentRecursionDepth > maxRecursionDepthSeen) {
			maxRecursionDepthSeen = currentRecursionDepth;
			goalCellLocation = new Point2D(col, row);
		}
		
		// Open the cell (if its not the start)
		if (cells[row][col].getType() != Cell.Type.START) {
			cells[row][col] = new Cell(Cell.Type.OPEN, new Point2D(col, row));
		}
		
		// Sleep for a bit so that the drawing doesn't happen instantly and is instead visible to the user
		try {
			Thread.sleep(MazeRunner.DRAWING_SPEED);
		} catch (InterruptedException e) { /* do nothing */ }
		
		// Visit surrounding cells in random order to see if we should open them too
		Direction[] order = generateRandomDirections();
		for (int i = 0; i < order.length; ++i) { // Try going in the direction specified by order[i]	
			
			// Stay in the bounds of the maze (recursion base case)
			if (row <= 0 || col <= 0 || row >= cells.length-1 || col >= cells[0].length-1) return;
			
			// Open a neighbor cell if it has 3 wall cells around it through a recursive call
			switch (order[i])
			{
				case UP:  	  if (cells[row-1][col-1].getType().equals(Cell.Type.WALL) && 
								cells[row-1][col].getType().equals(Cell.Type.WALL) && 
								cells[row-1][col+1].getType().equals(Cell.Type.WALL))
									openCell(row-1, col, currentRecursionDepth);
				break;
	
				case DOWN: 	  if (cells[row+1][col-1].getType().equals(Cell.Type.WALL) && 
								cells[row+1][col].getType().equals(Cell.Type.WALL) && 
								cells[row+1][col+1].getType().equals(Cell.Type.WALL))
									openCell(row+1, col, currentRecursionDepth);
				break;
	
				case LEFT:    if (cells[row-1][col-1].getType().equals(Cell.Type.WALL) && 
								cells[row][col-1].getType().equals(Cell.Type.WALL) && 
								cells[row+1][col-1].getType().equals(Cell.Type.WALL))
									openCell(row, col-1, currentRecursionDepth);
				break;
	
				case RIGHT:  if(cells[row-1][col+1].getType().equals(Cell.Type.WALL) && 
								cells[row][col+1].getType().equals(Cell.Type.WALL) && 
								cells[row+1][col+1].getType().equals(Cell.Type.WALL))
									openCell(row, col+1, currentRecursionDepth);
				break;
			}
		}
	}

	/**
	 * Utility method to generate a random array of Directions
	 * 
	 * @return An array of Directions in random order
	 */
	private Direction[] generateRandomDirections() {
		Direction[] array = {Direction.UP, Direction.DOWN, Direction.LEFT, Direction.RIGHT};
		for (int i = array.length - 1; i > 0; --i) {
			int randIndex = (int)(Math.random()*(i+1));  // pick an element at random
			Direction temp = array[i];      // swap it with element i
			array[i] = array[randIndex];
			array[randIndex] = temp;
		}
		return array;
	}

	/**
	 * Generates a random maze that may or may not have a solution.
	 */
	private void generateRandomMaze()
	{
		// Iterate through all non-border cells and mark them as either a wall or open 
		//based on a certain probability (currently 70% chance of open cell, 30% chance of wall)
		Random rand = new Random();
		for (int r = 0; r < rows; ++r)
		{
			for (int c = 0; c < columns; ++c)
			{
				if (r == 0 || c == 0 || r == rows-1 || c == columns-1)
				{
					cells[r][c] = new Cell(Cell.Type.WALL, new Point2D(c, r)); // Leave border cells as walls
				}
				else
				{
					if (rand.nextDouble() <.3) cells[r][c] = new Cell(Cell.Type.WALL, new Point2D(c, r));
					else cells[r][c] = new Cell(Cell.Type.OPEN, new Point2D(c, r));
				}
				
				// Sleep for a bit so that the drawing doesn't happen instantly and is instead visible to the user
				try {
					Thread.sleep(MazeRunner.DRAWING_SPEED);
				} catch (InterruptedException e) { /* do nothing */}
			}
		}

		// Generate a random starting location
		int startRow = rand.nextInt(rows-1) + 1;
		int startCol = rand.nextInt(columns-1) + 1;
		startCell = new Cell(Cell.Type.START, new Point2D(startCol, startRow));
		cells[startRow][startCol] = startCell;
		
		// Generate a random goal location (ensuring it isn't the start)
		int goalRow = rand.nextInt(rows-1) + 1;
		while (goalRow == startRow) goalRow = rand.nextInt(rows-1) + 1;
		int goalCol = rand.nextInt(columns-1) + 1;
		goalCellLocation = new Point2D(goalCol, goalRow);
		cells[goalRow][goalCol] = new Cell(Cell.Type.GOAL, goalCellLocation);	
	}

	/**
	 * Get the number of rows in the maze
	 * 
	 * @return The number of rows
	 */
	public int numRows() {
		return rows;
	}

	/**
	 * Get the number of columns in the maze
	 * 
	 * @return The number of columns
	 */
	public int numColumns() {
		return columns;
	}
	
	/**
	 * Get the maze start cell
	 * 
	 * @return The starting cell of the maze
	 */
	public Cell getStartCell() {
		return startCell;
	}

	/**
	 * Get the cell at a specified row and column in the maze. Note that the row corresponds to the cell's
	 * y coordinate and the column corresponds to the cell's x coordinate.
	 * @param row The row (y coordinate) of the cell to get
	 * @param column The column (x coordinate) of the cell to get
	 * @return
	 */
	public Cell getCell(int row, int column) {
		if (row < 0) throw new IllegalArgumentException("Error - row must be >= 0");
		if (row > rows) throw new IllegalArgumentException("Error - row out of "
				+ "bounds (max " + rows + " )");
		if (column < 0) throw new IllegalArgumentException("Error - column must be >= 0");
		if (column > columns) throw new IllegalArgumentException("Error - column out of "
				+ "bounds (max " + columns + " )");

		return cells[row][column];
	}

	/**
	 * Clear a maze of any solution (i.e., set all cells to unexplored and reset the order visited for all cells).
	 */
	public void clear() {
		for(int i = 0; i < cells.length; i++) {
			for (int j = 0; j < cells[i].length; j++) {
				Cell c = cells[i][j];
				c.setStatus(Cell.Status.UNEXPLORED);
				c.setOrderVisited(0);
			}
		}
	}

	/**
	 * Return a string representation of the maze based on encoding each cell to a certain character value. See
	 * the Cell toString() method for details on this encoding.
	 */
	public String toString()
	{
		StringBuilder sB = new StringBuilder();
		for (int i = 0; i < cells.length; i++) {
			for (int j = 0; j < cells[i].length; j++) {
				sB.append(cells[i][j]);
			}
			sB.append("\n");
		}
		return sB.toString();
	}
	
	/**
	 * Save a maze to a text file. The format is that the first line of the file will encode the columns and
	 * rows of the maze and each subsequent line will correspond to one row of the maze.
	 * 
	 * @param file The file to save the maze to
	 * @return True if the maze was saved to the file successfully, false otherwise
	 */
	public boolean save(File file) {
		PrintWriter outStream = null;
		try {
			outStream = new PrintWriter(new BufferedOutputStream(new FileOutputStream(file)));
		} catch (FileNotFoundException e) {
			return false;
		}
		
		outStream.println(numColumns() + " " + numRows());
		outStream.print(this);
		
		outStream.close(); // closes file
		return true;
	}
	
	/**
	 * Draw the maze on a canvas based on a given graphics context
	 * 
	 * @param gc The graphics context linked to the canvas the maze should be drawn on
	 */
	public void draw(GraphicsContext gc) {

		// How much area on the canvas do we have to draw with
		double width = gc.getCanvas().getWidth();
		double height = gc.getCanvas().getHeight();
		
		// Clear the drawing
		gc.setFill(Color.WHITE);
		gc.fillRect(0, 0, width, height);
		
		// Calculate the drawing size of each cell
		double cellWidth = width / numColumns();
		double cellHeight = height / numRows();
		
		// Draw the cells
		for (int row = 0; row < numRows(); row++) {
			for (int col = 0; col < numColumns(); col++) {
				cells[row][col].draw(gc, cellWidth, cellHeight);
			}
		}
		
		// Draw a 1 pixel black border around the whole canvas
		gc.setStroke(Color.BLACK);
		gc.strokeRect(0, 0, width, height); 
	}
}
