/* class Tree
 * Class for representing a tree, providing methods to create and draw
 * the tree in terms of TreeParts (a recursive data structure)
 * 
 * Doug DeCarlo
 */

import java.util.*;

import javax.media.opengl.GL;
import javax.vecmath.*;

class Tree implements Obstacle
{
    // Location of tree
    private double xpos, ypos;

    // Base of tree
    TreePart tree;
    
    // for drawing shadows
    boolean shadow;

    // ---------------------------------------------------------------

    // constructor
    public Tree(Random rgen, int level, int branching,
		double trunkLen, double trunkDiam,
		double xPosition, double yPosition)
    {
	super();
	
	shadow = false;
	
	// Set tree position
	xpos = xPosition;
	ypos = yPosition;

	// Construct tree
	tree = new TreePart(rgen, level, branching, trunkLen, trunkDiam, 1.0, new Point3d(0.0,0.0,0.0));
    }

    // ---------------------------------------------------------------
    // Obstacle methods

    // Get tree location (as a scene element)
    public Point3d getLocation()
    {
	return new Point3d(xpos, ypos, 0);
    }

    // Draw tree in scene
    public void draw(GL gl)
    {
	gl.glPushMatrix();
	gl.glTranslated(xpos, ypos, 0);
	tree.draw(gl);
	gl.glPopMatrix();
    }
}
