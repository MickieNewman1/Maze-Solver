/* *****************************************************************************
 * Title:            MazeSolver
 * Files:            MazeSolver.java
 * Semester:         Spring 2021
 * 
 * Author:           Daniel Szafir, daniel.szafir@colorado.edu
 * 
 * Description:		 A Maze Solver is an abstract class representing an object
 * 					 that can solve a maze. A particular solver (e.g., BFS, DFS,
 * 					 A*, etc.) should extend this class.
 * 
 * Written:       	 3/21/2020
 **************************************************************************** */

/**
 * A Maze Solver is the base abstract class for any algorithm that can solve a maze. It contains
 * some useful enums that list the type of possible solvers and the solution status. In addition,
 * it provides several instance variables for any solver to use.
 * 
 * @author Daniel Szafir
 *
 */
public abstract class MazeSolver {
	
	/**
	 * The type of solvers that are possible. Used to populate the combo box in the GUI that
	 * lets the user choose what type of solver they want. Any new solver should be added to this
	 * enum.
	 * 
	 * @author Daniel Szafir
	 *
	 */
	public enum Type {
		STACK, QUEUE;
	}
	
	/**
	 * The various possible solver statuses, corresponding to whether the maze has yet to be solved, 
	 * if a solution has been found, or if the solver has determined the maze is impossible to solve.
	 * 
	 * @author Daniel Szafir
	 *
	 */
	public static enum Status {
		UNSOLVED, SOLVED, NO_SOLUTION;
	}
	
	// The status of this particular solver
	protected Status status;
	
	// This solver's maze
	protected Maze maze;
	
	// Keep track of order we visit cells in
	protected int numCellsVisited;
	
	/**
	 * Create a MazeSolver for a given maze
	 * @param maze
	 */
	public MazeSolver(Maze maze) {
		if (maze == null) throw new IllegalArgumentException("Error - cannot create a solver based on a null maze");
		
		this.maze = maze;
		this.status = Status.UNSOLVED;
	}
	
	/**
	 * Get the status of this solver
	 * 
	 * @return The solver status (UNSOLVED, SOLVED, or NO_SOLUTION)
	 */
	public final Status getStatus() {
		return status;
	}
	
	/**
	 * Get how many cells this solver has visited so far (i.e., how many steps have been taken)
	 * 
	 * @return The number of cells this solver has visited
	 */
	public final int getCellsVisited()
	{
		return numCellsVisited;
	}
	
	/**
	 * Step explores a single cell. If the cell is the goal, then return true. If it is apparent
	 * that there is no possible solution, step should return false. Otherwise, step should mark the 
	 * cell as visited, determine which cells should be visited next, and return false.
	 * 
	 * @return true if this step resulted in the maze being solved (i.e., the goal was reached), 
	 * 		   false otherwise
	 */
	public abstract boolean step();
	
	/**
	 * Solve continually calls the step method until the step algorithm determines the maze is either
	 * solved or there is no possible solution, pausing a brief period in between each call to step
	 * to enable an animation of the solver.
	 */
	public final void solve() {
		while (status == Status.UNSOLVED) {
			step();
			
			try {
				Thread.sleep(MazeRunner.DRAWING_SPEED);
			} catch (InterruptedException e) {
				return;
			}
		}
	}
}
