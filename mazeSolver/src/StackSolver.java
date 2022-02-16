import java.util.Stack;
import javafx.geometry.Point2D;

/* *****************************************************************************
 *
 * Title:            StackSolver
 * Files:            StackSolver.java
 * Semester:         Spring 2021
 * 
 * Author:           Mickie Newman 
 * 
 * Description:		 A MazeSolver that uses a stack
 * 
 * Written:       	 (date)
 * 
 * Credits:          (anything that helped)
 **************************************************************************** */

/**
 * A MazeSolver that uses a stack to solve the maze. This implements a
 * DFS search.
 * 
 * @author Mickie Newman
 *
 */
public class StackSolver extends MazeSolver {
	
	// TODO: Create a stack of cells that will keep track of what cells should be visited
	Stack<Cell> Path = new Stack<Cell>();
	/**
	 * Create a StackSolver for a given maze
	 * 
	 * @param maze The maze the StackSolver will attempt to solve.
	 */
	public StackSolver(Maze maze)
	{
		super(maze); // Call the MazeSolver constructor to initialize the maze instance variable
		
		Path.push(maze.getStartCell());
		
		// TODO: initialize your stack and push the starting cell of the maze
		
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
		
		// TODO: If the stack of cells to visit is empty, set the solver status to no solution possible
		if(Path.empty()== true) {
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
		
		// TODO: Grab the top cell in the stack; this is the cell we are currently visiting
		Cell n = Path.peek();
		Point2D a = n.getCoordinates();
		int x = (int) a.getX();
		int y = (int) a.getY();
		
		// TODO: If the cell is not a wall and the cell's status is unexplored:
		if(n.getType() != Cell.Type.WALL && n.getStatus()== Cell.Status.UNEXPLORED){
			// TODO: If this cell is the goal, then we've solved the maze:
			if(n.getType()== Cell.Type.GOAL) {
				// TODO: Set status to solved and return true
				status = Status.SOLVED;
				return true;
			}
			// TODO: Mark this cell as visited so we don't revisit it later
			else {	
				n.setStatus(Cell.Status.EXPLORED);
				Path.pop();
			
			// TODO: Increment the numCellsVisited counter
				numCellsVisited ++;
		
			// TODO: Otherwise, push all adjacent (up/down/left/right) cells 
			//       that are both open and unexplored to the stack for future exploration
			
				Cell up = maze.getCell(x,y+1);
				if(up.getType() != Cell.Type.WALL && up.getStatus()== Cell.Status.UNEXPLORED) {
					Path.push(up);
				}
				Cell down = maze.getCell(x,y-1);
				if(down.getType() != Cell.Type.WALL && down.getStatus()== Cell.Status.UNEXPLORED) {
					Path.push(down);
				}
				Cell left = maze.getCell(x-1,y);
				if(left.getType() != Cell.Type.WALL && left.getStatus()== Cell.Status.UNEXPLORED) {
					Path.push(left);
				}
				Cell right = maze.getCell(x+1,y);
				if(right.getType() != Cell.Type.WALL && right.getStatus()== Cell.Status.UNEXPLORED) {
					Path.push(right);
				}
				
			}
			}
		
	
			
			
		
				
			
		
			
			return false; 
		// TODO: return false as we haven't found the goal yet

	}
}
