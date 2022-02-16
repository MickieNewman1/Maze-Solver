/* *****************************************************************************
 * Title:            Cell
 * Files:            Cell.java
 * Semester:         Spring 2021
 * 
 * Author:           Daniel Szafir, daniel.szafir@colorado.edu
 * 
 * Description:		 A Cell represents a single square in a maze
 * 
 * Written:       	 3/21/2020
 **************************************************************************** */

import javafx.geometry.Point2D;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * A cell represents a single square in a 2D maze.
 * 
 * @author Daniel Szafir
 */
public class Cell {
	
	/**
	 * The possible types of cells
	 * 
	 * @author Daniel Szafir
	 */
	public static enum Type {
		WALL, OPEN, START, GOAL;
	}
	
	/**
	 * The possible statuses of a cell
	 * 
	 * @author Daniel Szafir
	 */
	public static enum Status {
		UNEXPLORED, EXPLORED;
	}
		
	// This cell's type
	private Type type;
	
	// This cell's status
	private Status status;
	
	// The coordinates (x,y) of this cell in the 2D maze
	private Point2D coordinates;
	
	// In what order was this cell visited by a solver (needed to print out the numbers to the GUI)
	private int orderVisited;
	
	/**
	 * Create a single cell with a certain type at a given location in a maze.
	 * 
	 * @param type The type of cell to create
	 * @param coordinates Where the cell is located in the maze
	 */
	public Cell(Type type, Point2D coordinates) {
		this.type = type;
		this.coordinates = coordinates;
		this.status = Status.UNEXPLORED;
	}
	
	/**
	 * Create a single cell whose type is determined by a character (i.e., for use when a maze file is
	 * loaded) at a given location in a maze.
	 * 
	 * @param c The character that aligns with the cell type. Should be one of the following:
	 * 			# (wall cell)
	 * 			. (open cell)
	 * 			S (start cell)
	 * 			G (goal cell)
	 * @param coordinates Where the cell is located in the maze
	 */
	public Cell(char c, Point2D coordinates) {
		if (c == '#') this.type = Type.WALL;
		else if (c == '.') this.type = Type.OPEN;
		else if (c == 'S') this.type = Type.START;
		else if (c == 'G') this.type = Type.GOAL;
		else throw new IllegalArgumentException("Error - unknown square type specified: " + c);
		
		this.coordinates = coordinates;
		this.status = Status.UNEXPLORED;
	}
	
	/**
	 * Get the type of this cell
	 * 
	 * @return The cell's type
	 */
	public Type getType() {
		return type;
	}
	
	/**
	 * Set the type of this cell
	 * 
	 * @param type The new type this cell will be
	 */
	public void setType(Type type) {
		this.type = type;
	}
	
	/**
	 * Get the status of this cell
	 * 
	 * @return The cell's status
	 */
	public Status getStatus() {
		return status;
	}
	
	/**
	 * Set the status of this cell
	 * 
	 * @param status The new status this cell will have
	 */
	public void setStatus(Status status) {
		this.status = status;
	}
	
	/**
	 * Get the 2D coordinates (x,y position) of this cell within the maze
	 * 
	 * @return The cell's coordinates
	 */
	public Point2D getCoordinates() {
		return coordinates;
	}
	
	/**
	 * Get the order in which this cell was visited by a MazeSolver
	 * 
	 * @return The order in which this cell was visited
	 */
	public int getOrderVisited() {
		return orderVisited;
	}

	/**
	 * Set the order in which this cell was visited by a MazeSolver
	 * 
	 * @param orderVisited The order in which a solver visited this cell
	 */
	public void setOrderVisited(int orderVisited) {
		this.orderVisited = orderVisited;
	}
	
	/**
	 * Convert this cell to a String representation (i.e., for saving a maze to a file).
	 * This method will return a String based on the cell type:
	 *   Wall: #
	 *   Open: .
	 *   Start: S
	 *   Goal: G
	 */
	public String toString() {
		switch(type) {
			case WALL: return "#";
			case OPEN: return ".";
			case START: return "S";
			case GOAL: return "G";
			default: return ""; // shouldn't happen
		}
	}
	
	/**
	 * Draw this cell on a canvas using a given canvas graphics context
	 * 
	 * @param gc The graphics context linked to the canvas the cell should be drawn on
	 * @param cellWidth The width to draw the cell
	 * @param cellHeight The height to draw the cell
	 */
	public void draw(GraphicsContext gc, double cellWidth, double cellHeight)
	{
		if (type == Cell.Type.OPEN) {
			if (status == Status.UNEXPLORED) {
				gc.setFill(Color.WHITE);
				gc.setStroke(Color.WHITE);
			}
			else if (status == Status.EXPLORED){
				gc.setFill(Color.GRAY);
				gc.setStroke(Color.GRAY);
			}

		}
		else if (type == Cell.Type.GOAL) {
			gc.setFill(Color.GREEN);
			gc.setStroke(Color.GREEN);
		}
		else if (type == Cell.Type.START) {
			gc.setFill(Color.YELLOW);
			gc.setStroke(Color.YELLOW);
		}
		else if (type == Cell.Type.WALL) {
			gc.setFill(Color.BLACK);
			gc.setStroke(Color.BLACK);
		}
		
		gc.fillRect(coordinates.getX()*cellWidth, coordinates.getY()*cellHeight, cellWidth, cellHeight);
		gc.strokeRect(coordinates.getX()*cellWidth, coordinates.getY()*cellHeight, cellWidth, cellHeight);
		
		if (orderVisited > 0) {
			gc.setFill(Color.BLACK);
			gc.fillText(String.valueOf(orderVisited), coordinates.getX()*cellWidth + cellWidth/2, coordinates.getY()*cellHeight + 3*cellHeight/4);
		}
	}
}
