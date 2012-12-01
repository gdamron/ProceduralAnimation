/* class TreePart
 * Class for representing a subtree, describing the size of the part at
 * the transformation to get to this subtree from the parent, the
 * current tree node (length and width) and whether this is a leaf node
 *
 * Doug DeCarlo
 */

import java.util.*;

import javax.media.opengl.GL;
import javax.vecmath.*;

class TreePart
{
    // Transformation for this branch/leaf (relative to its parent)

    Point3d angle;
    double scale;
    int depth;
    Random rgen;
    int branches;

    // Leaf or trunk
    boolean leaf;

    // Size of part
    double length, width;

    // Children
    TreePart[] parts;
    
    // for drawing shadows
    boolean shadow;

    // ---------------------------------------------------------------

    // Constructor: recursively construct a treepart of a particular depth,
    // with specified branching factor, dimensions and transformation
    public TreePart(Random rgen,
		    int depth, int numBranch,
		    double partLen, double partWid,
		    double s, Point3d a
                    )
    {

        this.leaf = (depth==0);
        this.length = partLen;
        this.width = partWid;
        this.angle = new Point3d(a.x, a.y, a.z);
        this.scale = s;
        this.depth = depth;
        this.rgen = rgen;
        this.branches = numBranch;
        parts = new TreePart[numBranch];
        shadow = false;


        // Create branch or leaf (based on depth) and create children
        // branches/leaves recursively
        if (this.depth > 0) {
        	for (int i = 0; i < this.branches; i++) {
        		
        		double rx = (rgen.nextDouble()*25+20);
        		double ry = rgen.nextDouble()*10;
        		double rz = (rgen.nextDouble()*(180.0/branches)+90)*(i+1);
        		double new_scale = rgen.nextDouble()*0.7;
        		parts[i] = new TreePart(this.rgen,
        				this.depth-1, this.branches,
        				this.length*0.9, this.width*0.6,
        				new_scale, new Point3d(rx,ry,rz));
        		
        	}
        }
    }

    // Recursively draw a tree component
    //  - place the component using transformation for this subtree
    //  - draw leaf (if this is a leaf node)
    //  - draw subtree (if this is an interior node)
    //    (draw this component, recursively draw children)
    public void draw(GL gl)
    {
    	gl.glPushMatrix();
    	
    	// Place this component
    	gl.glRotated(this.angle.z,0,0,1);
    	gl.glRotated(this.angle.y,0,1,0);
    	gl.glRotated(this.angle.x,1,0,0);


    	if (leaf) {
    		// Draw a nice maple leaf
    		double s = 1.0/(this.branches*0.7);
    		gl.glColor3d(0.0, 1.0, 0.0);
    		gl.glBegin(GL.GL_POLYGON);
    		gl.glVertex3d(0,0,0);
    		gl.glVertex3d(-s*7/18, s/9, 0);
    		gl.glVertex3d(-s/2, s/3, 0);
    		gl.glVertex3d(-s*5/18, s/3, 0);
    		gl.glVertex3d(-s/3, s*2/3, 0);
    		gl.glVertex3d(-s/9, s*5/9, 0);
    		gl.glVertex3d(0, s, 0);
    		gl.glVertex3d(s/9, s*5/9, 0);
    		gl.glVertex3d(s/3, s*2/3, 0);
    		gl.glVertex3d(s*5/18, s/3, 0);
    		gl.glVertex3d(s/2, s/3, 0);
    		gl.glVertex3d(s*7/18, s/9, 0);
    		gl.glEnd();
    		//Objs.sphere(gl);
    	} else {
    		// Draw branch
    		gl.glScaled(this.width, this.width, this.length);
    		// make sure it stays brown
    		gl.glColor3d(0.5, 0.4, 0.3);
    		Objs.cylinder(gl);
    		gl.glScaled(1/this.width, 1/this.width, 1/this.length);
    		gl.glTranslated(0, 0, this.length);
    		
    		if (depth>0) {
    			for (int i = 0; i < this.branches; i++) {
    				this.parts[i].draw(gl);
    			}
    		}

    	}

    	gl.glPopMatrix();
	
    }
    
}
