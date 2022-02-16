/* *****************************************************************************
 * Title:            QueueSolver
 * Files:            QueueSolver.java
 * Semester:         Spring 2021
 * 
 * Author:           Mickie Newman 
 * 
 * Description:		 A MazeSolver that uses a queue
 * 
 * Written:       	 
 * 
 * Credits:          (anything that helped)
 **************************************************************************** */
import java.util.LinkedList;
import java.util.Queue;

import javafx.geometry.Point2D;
/**
 * A MazeSolver that uses a queue to solve the maze. In effect, this implements a
 * BFS search (we will talk about this more in class).
 * 
 * @author Mickie Newman
 *
 */
public class QueueSolver extends MazeSolver {

	// TODO: Create a queue of cells that will keep track of what cells should be visited
	Queue<Cell> path = new LinkedList<Cell>();
	/**
	 * Create a QueueSolver for a given maze
	 * 
	 * @param maze The maze the QueueSolver will attempt to solve.
	 */
	public QueueSolver(Maze maze)
	{
		super(maze); // Call the MazeSolver constructor to initialize the maze instance variable
		
		// TODO: initialize your queue and enqueue the starting cell of the maze
		path.add(maze.getStartCell());
		
		
	}
	
	/**
	 * Step explores a single cell. If the cell is the goal, then return true. If it is apparent
	 * that there is no possible solution, step should return false. Otherwise, step should mark the 
	 * cell as visited, determine which cells should be visited next, and return false.
	 * 
	 * @return true if this step resulted in the maze being solved (i.e., the goal was reached), 
	 * 		   false otherwise
	 */
	@Override
	public boolean step() {
	 // TODO: Delete this line (it is temporary just so you can run and see the GUI)
		
		// TODO: If the queue of cells to visit is empty, set the solver status to no solution possible
		if(path.isEmpty()== true) {
			status = Status.UNSOLVED;
		}
		
		// TODO: If the status is no solution, return false
		if(status == Status.NO_SOLUTION) {
			return false; 
		}

		// TODO: If the status is solved, return true
		if(status == Status.SOLVED) {
			return true; 
		}

		// TODO: Grab the first cell in the queue; this is the cell we are currently visiting
		Cell n = path.peek();
		Point2D a = n.getCoordinates();
		int x = (int) a.getX();
		int y = (int) a.getY();
		
		// TODO: If the cell is not a wall and the cell's status is unexplored:
		if(n.getType() != Cell.Type.WALL && n.getStatus()== Cell.Status.UNEXPLORED){
		
			// TODO: Mark this cell as visited so we don't revisit it later
			n.setStatus(Cell.Status.EXPLORED);
			
			// TODO: Increment the numCellsVisited counter
			numCellsVisited ++;
			

			// TODO: If this cell is the goal, then we've solved the maze:
			if(n.getType()== Cell.Type.GOAL) {
				// TODO: Set status to solved and return true
				status = Status.SOLVED;
				return true;
			}
			
			// TODO: Otherwise, enqueue all adjacent (up/down/left/right) cells 
			//       that are both open and unexplored to the queue for future exploration
			Cell up = maze.getCell(x,y+1);
			if(up.getType() != Cell.Type.WALL && up.getStatus()== Cell.Status.UNEXPLORED) {
				path.add(up);
			}
			Cell down = maze.getCell(x,y-1);
			if(down.getType() != Cell.Type.WALL && down.getStatus()== Cell.Status.UNEXPLORED) {
				path.add(down);
			}
			Cell left = maze.getCell(x-1,y);
			if(left.getType() != Cell.Type.WALL && left.getStatus()== Cell.Status.UNEXPLORED) {
				path.add(left);
			}
			Cell right = maze.getCell(x+1,y);
			if(right.getType() != Cell.Type.WALL && right.getStatus()== Cell.Status.UNEXPLORED) {
				path.add(right);
			}
			
		
		// TODO: return false as we haven't found the goal yet
		
	}
		return false;
	
}
}
