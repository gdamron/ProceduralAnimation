/* class Rock
 * Represents a rock using a rectangular grid; given a particular level
 * of subdivision l, the rock will be a 2^l+1 X 2^l+1 height field
 * The rock is drawn a little below the surface to get a rough edge.
 *
 * Doug DeCarlo
 */

import java.util.*;

import javax.media.opengl.GL;
import javax.vecmath.*;

class Rock implements Obstacle
{
    // Location of rock
    private double xpos, ypos, scale;

    // -- Rock mesh: a height-field of rsize X rsize vertices
    int rsize;
    // Height field: z values
    private double[][] height;
    // Whether height value has been set (locked) already
    private boolean[][] locked;

    // Random number generator
    Random rgen;

    // ---------------------------------------------------------------

    public Rock(Random randGen, int level, 
		double xPosition, double yPosition, double scaling)
    {
        // Grid size of (2^level + 1)
        rsize = (1 << level) + 1;

        // Height field -- initially all zeros
        height = new double[rsize][rsize];
        locked = new boolean[rsize][rsize];
 
        rgen = randGen;

	// Set rock position in the world
	xpos = xPosition;
	ypos = yPosition;
	scale = scaling;

	compute();
    }

    // ----------------------------------------------------------------
    // Obstacle methods

    // Get rock location (as a scene element)
    public Point3d getLocation()
    {
	return new Point3d(xpos, ypos, 0);
    }

    // Draw rock in scene
    public void draw(GL gl)
    {
	gl.glPushMatrix();

        // Translate rock down (so it has an interesting boundary)
	gl.glTranslated(xpos, ypos, -0.15);

	gl.glScaled(scale, scale, scale);

        gl.glColor3d(0.6, 0.6, 0.6);

        // Create these outside the loops, so objects persist and
        // unnecessary GC is avoided
        Point3d p = new Point3d();
        Vector3d n = new Vector3d();

        // Draw polygon grid of rock as quad-strips
        for (int i = 0; i < rsize-1; i++) {
            gl.glBegin(GL.GL_QUAD_STRIP);
            for (int j = 0; j < rsize; j++) {
                getRockPoint(i, j, p);
                getRockNormal(i, j, n);
                gl.glNormal3d(n.x, n.y, n.z);
                gl.glVertex3d(p.x, p.y, p.z);
                
                getRockPoint(i+1, j, p);
                getRockNormal(i+1, j, n);
                gl.glNormal3d(n.x, n.y, n.z);
                gl.glVertex3d(p.x, p.y, p.z);
            }
            gl.glEnd();
        }

        // Make GC easy
        p = null;
        n = null;
    
	gl.glPopMatrix();
    }
    
    // ---------------------------------------------------------------

    // Point (i,j) on the rock -- point p gets filled in
    public void getRockPoint(int i, int j, Point3d p)
    {
        // Rock (x,y) locations are on the grid [-0.5, 0.5] x [-0.5, 0.5]
        p.x = (double)i / (rsize-1) - 0.5;
        p.y = (double)j / (rsize-1) - 0.5;
        // Rock z comes from height field
        p.z = height[i][j];
    }

    // Normal vector (i,j) on the rock -- vector n gets filled in
    public void getRockNormal(int i, int j, Vector3d n)
    {
        // This is the formula for a normal vector of a height field with
        // regularly spaced x and y values (assuming rock is zero on
        // its borders and outside of it too)

        // X component is zleft - zright (respecting boundaries)
        n.x = height[(i == 0) ? i : i-1][j] - 
              height[(i == rsize-1) ? i : i+1][j];

        // Y component is zbottom - ztop (respecting boundaries)
        n.y = height[i][(j == 0) ? j : j-1] - 
              height[i][(j == rsize-1) ? j : j+1];

        // Z component is twice the separation
        n.z = 2 / (rsize-1);

        n.normalize();
    }
    
    public double getRockScale() {
    	return scale;
    }

    // ---------------------------------------------------------------

    // Compute the geometry of the rock
    // (called when the rock is created)
    public void compute()
    {
    	// Initialize mesh
    	for (int i = 0; i < rsize; i++) {
    		for (int j = 0; j < rsize; j++) {
    			height[i][j] = 0;

    			// Lock sides...
    			locked[i][j] = (i == 0 || i == rsize-1 ||
    					j == 0 || j == rsize-1);
    		}
    	}

    	// Raise the middle point and lock it there
    	double center_height = 0.3;
    	height[rsize/2][rsize/2] = center_height;
    	locked[rsize/2][rsize/2] = true;

    	// Recursively compute fractal structure
    	computeFractal(new Point2d(0.0,0.0), new Point2d(rsize-1.0,rsize-1.0), 1);
    }

    // Recursively compute fractal rock geometry
    private void computeFractal(Point2d p1, Point2d p2, double level)
    {
    	if ((p2.x-p1.x)>1 && (p2.y-p1.y)>1) {
    		double displacement = Math.pow(2, level);
    		int mid_x = (int) ((p1.x+p2.x)/2.0);
    		int mid_y = (int) ((p1.y+p2.y)/2.0);
    		double a = height[(int)p1.x][(int)p1.y];
    		double b = height[(int)p1.x][(int)p2.y];
    		double c = height[(int)p2.x][(int)p2.y];
    		double d = height[(int)p2.x][(int)p1.y];
    		
    		// this combination of nextGaussian and nextDouble makes nice rocks
    		double e = (a+b+c+d)/4.0 + rgen.nextGaussian() / displacement;
    		double f = (a+b+e+e)/4.0 + (rgen.nextDouble()) / displacement;
    		double g = (b+c+e+e)/4.0 + (rgen.nextDouble()) / displacement;
    		double h = (c+d+e+e)/4.0 + (rgen.nextDouble()) / displacement;
    		double i = (d+a+e+e)/4.0 + (rgen.nextDouble()) / displacement;
    		
    		displaceAndLock(mid_x, mid_y, e);
    		displaceAndLock((int)p1.x, (int)p1.y, e);
    		displaceAndLock((int)p1.x, (int)p2.y, e);
    		displaceAndLock((int)p2.x, (int)p2.y, e);
    		displaceAndLock((int)p2.x, (int)p1.y, e);
    		displaceAndLock((int)p1.x, mid_y, f);
    		displaceAndLock(mid_x, (int)p2.y, g);
    		displaceAndLock((int)p2.x, mid_y, h);
    		displaceAndLock(mid_x, (int)p1.y, i);
    		
    		level += 1;
    		computeFractal(p1, new Point2d((double)mid_x, (double) mid_y), level);
    		computeFractal(new Point2d(p1.x, (double) mid_y), new Point2d((double)mid_x, p2.y), level);
    		computeFractal(new Point2d((double)mid_x, (double) mid_y), p2, level);
    		computeFractal(new Point2d((double)mid_x, p1.y), new Point2d(p2.x, (double) mid_y), level);
    		
    	}
    }
    
    private void displaceAndLock(int x, int y, double d) {
    	if (!locked[x][y]) {
    		height[x][y] = d;
    		locked[x][y] = true;
    	}
    }
}
