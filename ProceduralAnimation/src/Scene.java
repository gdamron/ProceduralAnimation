/* class Scene
 * Methods to describe, process and display the scene which contains
 * rocks, trees and critters.
 *
 * Doug DeCarlo
 */

import java.util.*;
import java.text.*;

import javax.media.opengl.GL;
import javax.vecmath.*;

import com.sun.opengl.util.GLUT;

public class Scene
{
	// Parameters for specifying V; the 3D view
	private Vector<DoubleParameter> params;
	private DoubleParameter tH, tV, tZ, rAlt, rAzim;

	// Parameters for display options
	private Vector<BooleanParameter> options;
	private BooleanParameter drawTime;
	public  BooleanParameter drawAnimation, drawBugView;

	// ------------

	// Elements of the scene
	Vector<Critter> critters;
	Vector<Obstacle> obstacles;

	// Main character in scene (a reference to a bug stored in critters) */
	Bug mainBug, predator;

	// Random number generator
	Random rgen;

	// Clock reading at last computation
	double computeClock = 0;

	// Seed for random number generator
	long seed;

	// Rendering quality flag
	boolean nice;

	// Speed multiplier for clock (1.0 is default)
	double clockSpeed;
	int previousUpdate = 0;

	// File prefix used for file dumping (null if not dumping images)
	String dumpPrefix;

	// Center of the world
	Point3d origin = new Point3d(0,0,0);
	
	// Previous attraction point for Critters
	Point3d prevAttraction = new Point3d(0,0,0);
	
	// Constant for radius of trees
	static double treeRadius = 4.0;

	//-----------------------------------------------------------------------

	// Default constructor for scene
	public Scene(long seedVal, boolean niceVal, double clockSpeedVal, 
			String dumpPrefixVal)
	{
		seed = seedVal;
		nice = niceVal;
		clockSpeed = clockSpeedVal;
		dumpPrefix = dumpPrefixVal;

		// Allocate parameters
		params = new Vector<DoubleParameter>();
		options = new Vector<BooleanParameter>();

		tH = addParameter(new DoubleParameter("Left/Right", 0, -10, 10, 1));
		tV = addParameter(new DoubleParameter("Down/Up",   -2, -10, 10, 1));
		tZ = addParameter(new DoubleParameter("Out/In",     0, -30, 20, 1));

		rAlt  = addParameter(new DoubleParameter("Altitude", 15, -15, 80, 1));
		rAzim = addParameter(new DoubleParameter("Azimuth", 0, -180, 180, 1));

		drawAnimation = addOption(new BooleanParameter("Animation", 
				false, 2));
		drawTime      = addOption(new BooleanParameter("Show time",
				dumpPrefix == null, 
				1));
		drawBugView   = addOption(new BooleanParameter("Bug camera view", 
				false, 1));

		build();
	}

	// ----------------------------------------------------------------------

	// Keep track of list of all scene parameters/drawing options
	public DoubleParameter addParameter(DoubleParameter p)
	{
		params.add(p);
		return p;
	}
	public BooleanParameter addOption(BooleanParameter p)
	{
		options.add(p);
		return p;
	}

	// Accessors for parameters/options
	public Vector getParams()
	{
		return params;
	}
	public Vector getOptions()
	{
		return options;
	}

	// Reset all shape parameters to default values
	public void reset()
	{
		Iterator i;


		i = params.iterator(); 
		while (i.hasNext()) {
			((Parameter)i.next()).reset();
		}

		i = options.iterator(); 
		while (i.hasNext()) {
			((Parameter)i.next()).reset();
		}
	}

	// -----------------------------------------------------------------
	// -- Clock stuff

	// Starting time of program, and time of latest pause
	public long startTime, pauseTime;

	// Flag for determining if clock always reports 1/30 second
	// intervals each time it is polled
	public boolean frameByFrameClock = false;

	// Frame number (for frame-by-frame clock)
	private int frameNumber = 0;

	// Get current frame number
	public int getFrameNumber()
	{
		return frameNumber;
	}

	// Go to next frame number
	public void incrementFrameNumber()
	{
		frameNumber++;
	}

	// Make clock frame-by-frame (each frame has 1/30 second duration)
	public void setFrameByFrameClock()
	{
		frameByFrameClock = true;
	}

	// Record starting time of program and frame number
	public void resetClock()
	{
		startTime = System.currentTimeMillis();
		pauseTime = startTime;

		computeClock = 0;

		frameNumber = 0;
	}

	// Pause clock (time doesn't elapse)
	public void pauseClock(boolean stop)
	{
		long now = System.currentTimeMillis();

		if (stop) {
			pauseTime = now;
		} else {
			startTime += now - pauseTime;
		}
	}

	// Return current time (in seconds) since program started
	// (or if frameByFrameClock is true, then return time of current
	// frame number)
	public double readClock()
	{
		if (frameByFrameClock) {
			return frameNumber * (1/30.0f);
		} else {
			long elapsed;

			if (drawAnimation.value) {
				// Time during animation
				elapsed = System.currentTimeMillis() - startTime;
			} else {
				// Time during pause
				elapsed = pauseTime - startTime;
			}

			return elapsed / 1000.0f;
		}
	}

	// ----------------------------------------------------------------------

	// Build the contents of the scene
	// (no OpenGL calls are allowed in here, as it hasn't been
	//  initialized yet)
	public void build()
	{
		Point3d loc;

		computeFPS(0);

		// Make random number generator
		if (seed == -1) {
			seed = System.currentTimeMillis() % 10000;
			System.out.println("Seed value: " + seed);
		}
		rgen = new Random(seed);

		// Create empty scene
		obstacles = new Vector<Obstacle>();
		critters = new Vector<Critter>();

		// ---------------



		// The randomized version)
		// although it seems to work a little better if I constrain the number of elements pretty severely
		int numTrees = (int) (rgen.nextDouble()*2+1);
		for (int i = 0; i < numTrees; i++) {
			Point2d newObstacle = getSafeLocation(1.0, true, 7.5);
			if (numTrees > 1)
				obstacles.addElement(new Tree(rgen, 5, 4, 2.0f, 0.3f, newObstacle.x, newObstacle.y));
			else 
				obstacles.addElement(new Tree(rgen, 5, 5, 2.0f, 0.3f, newObstacle.x, newObstacle.y));
		}
		
		int numRocks = (int) (rgen.nextDouble()*4+1);
		for (int i = 0; i < numRocks; i++) {
			double scale = rgen.nextDouble()*3.0+1.0;
			Point2d newObstacle = getSafeLocation(scale, false, 5.5);
			// degree 3 rocks seem to have the best looks to efficiency ration
			obstacles.addElement(new Rock(rgen, 3, newObstacle.x, newObstacle.y, scale));
		}

		// Create the main bug
		double mainBugScale = 0.6;
		Point2d mainBugPos = getSafeLocation(mainBugScale, false, 5);
		mainBug = new Bug(rgen, mainBugScale,  mainBugPos.x, mainBugPos.y,  0.1f, 0.0f);
		critters.addElement(mainBug);
		
		double predatorScale = mainBugScale * 1.5;
		Point2d predatorPos = getSafeLocation(predatorScale, false, 5);
		predator = new Bug(rgen, predatorScale, predatorPos.x, predatorPos.y, 0.1f, 0.0f);
		predator.isPredator = true;
		critters.addElement(predator);

		// ---------------

		// Reset computation clock
		computeClock = 0;
	}

	// Perform computation for critter movement so they are updated to
	// the current time
	public void process()
	{
		// Get current time
		double t = readClock() * clockSpeed;
		double dTime = t - computeClock;
		double dtMax = 1/50.0f;

		// Set current time on display
		computeClock = t;

		// Only process if time has elapsed
		if (dTime <= 0)
			return;


		// ---------------

		// Compute accelerations, then integrate (using Critter methods)

		// This part advances the simulation forward by dTime seconds, but
		// using steps that are no larger than dtMax (this means it takes
		// more than one step when dTime > dtMax -- the number of steps
		// you need is stored in numSteps).
		
		
		int numSteps = 100;
		for (int i = 0; i < numSteps; i++) {
			for (int k = 0; k < critters.size(); k++) {
				Bug bug = (Bug) critters.get(k);
				bug.accelReset();
				double critterRadius = bug.scale+0.25;	

				// make sure the bug is afraid of rocks and trees
				for (int j = 0; j < obstacles.size(); j++) {
					Obstacle temp = obstacles.get(j);
					if (temp instanceof Rock) {
						Rock tempRock = (Rock) temp;
						double trScale = tempRock.getRockScale();
						double rockRadius = Math.sqrt((trScale*trScale)*2.0)/2.0;
						bug.accelAttract(tempRock.getLocation(), critterRadius, rockRadius,-trScale+0.5, -8);
					} else {
						Tree tempTree = (Tree) temp;
						double treeRadius = 0.1;
						bug.accelAttract(tempTree.getLocation(), critterRadius, treeRadius, -0.5, -8);
					}
				}

				
			}
			
			Point2d attractPoint2d;
			int currentSecond = (int) t;

			// generate an attraction point once every two seconds to facilitate wandering, but 
			// make sure it's not out of bounds or too close to an obstacle 
			if ((currentSecond%2)==0 && currentSecond > previousUpdate) {
				previousUpdate = currentSecond;
				attractPoint2d = getSafeLocation(mainBug.scale+0.25, false, 5);
			} else {
				attractPoint2d = new Point2d(prevAttraction.x, prevAttraction.y);
			}

			Point3d attractPoint = new Point3d(attractPoint2d.x, attractPoint2d.y, 0.0);
			mainBug.accelAttract(attractPoint, mainBug.scale+0.25, 0.0, 0.2, 2);
			mainBug.accelAttract(predator.getLocation(), predator.scale+0.25, 0.0, -10, -10);
			predator.accelAttract(mainBug.getLocation(), predator.scale+0.25, mainBug.scale+0.25, 0.2, 2);
			
			for (int k = 0; k < critters.size(); k++) {
				Bug bug = (Bug) critters.get(k);
				bug.accelDrag(2*mainBug.scale);
				bug.integrate(dTime/numSteps);
			}
			//System.out.println(mainBug.acc);
			prevAttraction = attractPoint;
			attractPoint=null;
		}
		
		// Keyframe motion for each critter
		for (int k = 0; k < critters.size(); k++) {
			Bug bug = (Bug) critters.get(k);
			double temp = bug.dist;
			double new_t = temp - (int) temp;
			bug.keyframe(new_t);
		}
		
	}

	// Draw scene
	public void draw(GL gl, GLUT glut)
	{
		// Light position
		float lt_posit[] = { 10, 5, 30, 0 };
		// Ground plane (for clipping)
		double ground[]  = { 0.0, 0.0, 1.0, 0.0 };

		// Do computation if animating
		if (drawAnimation.value) {
			process();
		}

		// ------------------------------------------------------------

		// Initialize materials
		materialSetup(gl);

		// Specify V for scene
		gl.glLoadIdentity();
		transformation(gl);

		// Position light wrt camera
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, lt_posit, 0);
		gl.glEnable(GL.GL_LIGHTING);

		// Draw ground plane (a circle at z=0 of radius 15)
		gl.glColor3d(0.4, 0.6, 0.35);
		gl.glBegin(GL.GL_POLYGON);
		gl.glNormal3d(0, 0, 1);
		int ncirc = 200;
		for (int i = 0; i < ncirc; i++) {
			double theta = 2*Math.PI * i / ncirc;
			gl.glVertex3d(15*Math.cos(theta), 15*Math.sin(theta), 0);
		}
		gl.glEnd();

		// Draw critters
		for (int i = 0; i < critters.size(); i++) {
			((Critter)(critters.elementAt(i))).draw(gl);
		}

		// Clip below ground (so rocks don't peek below ground)
		gl.glClipPlane(GL.GL_CLIP_PLANE0, ground, 0);

		// **** Once you get the rock working, enable this -- it can be
		//      difficult to debug the rock when this is on, as you can only
		//      see the top of it -- this way you'll see the entire rock if
		//      you peek below the ground plane...
		gl.glEnable(GL.GL_CLIP_PLANE0);

		// Draw obstacles
		for (int i = 0; i < obstacles.size(); i++) {
			((Obstacle)(obstacles.elementAt(i))).draw(gl);
		}
		gl.glDisable(GL.GL_CLIP_PLANE0);
		
		// Draw shadows for trees and bugs (rocks are too expensive)
		gl.glTranslated(0.0, 0.0, 0.001);
		double[] m = new double[16];
		for (int i = 0; i < 15; i++) m[i] = 0.0;
		m[0] = ground[1]*lt_posit[1]+ground[2]*lt_posit[2];
		m[1] = -ground[0]*lt_posit[1];
		m[2] = -ground[0]*lt_posit[2];
		m[3] = m[7] = m[11] = 0.0;
		m[4] = -ground[1]*lt_posit[0];
		m[5] = ground[0]*lt_posit[0]+ground[2]*lt_posit[2];
		m[6] = -ground[1]*lt_posit[2];
		m[8] = -ground[2]*lt_posit[0];
		m[9] = -ground[2]*lt_posit[1];
		m[10] = ground[0]*lt_posit[0]+ground[1]*lt_posit[1];
		m[12] = -ground[3]*lt_posit[0];
		m[13] = -ground[3]*lt_posit[1];
		m[14] = -ground[3]*lt_posit[2];
		m[15] = ground[0]*lt_posit[0]+ground[1]*lt_posit[1]+ground[2]*lt_posit[2];
		
		gl.glDisable(GL.GL_LIGHT0);
		
		gl.glPushMatrix();
		gl.glMultMatrixd(m, 0);
		
		for (int i = 0; i < critters.size(); i++) {
			Critter temp = critters.elementAt(i);
			temp.shadow = true;
			temp.draw(gl);
			temp.shadow = false;
		}
		
		for (int i = 0; i < obstacles.size(); i++) {
			Obstacle temp = obstacles.elementAt(i);
			if (temp instanceof Tree) {
				temp.draw(gl);
			}
		}
		
		gl.glPopMatrix();
		gl.glEnable(GL.GL_LIGHT0);
		
		// Draw text on top of display showing time
		if (drawTime.value) {
			drawText(gl, glut, computeClock / clockSpeed);
		} else {
			numPrevT = 0;
		}
	}

	// Transformation of scene based on GUI values
	// (also transform scene so Z is up, X is forward)
	private void transformation(GL gl)
	{
		// Make X axis face forward, Y right, Z up
		// (map ZXY to XYZ)
		gl.glRotated(-90, 1, 0, 0);
		gl.glRotated(-90, 0, 0, 1);

		if (drawBugView.value) {
			// ---- "Bug cam" transformation (for mainBug)
			// e's a spazzy little dude.

			gl.glTranslated(0, 0, -(1.15*mainBug.scale));

			Point3d bugPos = new Point3d(mainBug.pos);
			Point3d prevBugPos = new Point3d(mainBug.prevPos);
			double a = Math.toDegrees(Math.atan2(bugPos.y-prevBugPos.y, bugPos.x-prevBugPos.x))-180;

			// Translate by Zoom/Horiz/Vert
			gl.glRotated(-a, 0, 0, 1);
			gl.glTranslated(-bugPos.x, -bugPos.y, 0.0);


		} else {
			// ---- Ordinary scene transformation

			// Move camera back so that scene is visible
			gl.glTranslated(-20, 0, 0);

			// Translate by Zoom/Horiz/Vert
			gl.glTranslated(tZ.value, tH.value, tV.value);

			// Rotate by Alt/Azim
			gl.glRotated(rAlt.value,  0, 1, 0);
			gl.glRotated(rAzim.value, 0, 0, 1);
		}
	}

	// Define materials and lights
	private void materialSetup(GL gl)
	{
		float white[]  = {   1.0f,   1.0f,   1.0f, 1.0f };
		float black[]  = {   0.0f,   0.0f,   0.0f, 1.0f };
		float dim[]    = {   0.1f,   0.1f,   0.1f, 1.0f };

		// Set up material and light
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT,  dim, 0);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE,  white, 0);
		gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_SPECULAR, dim, 0);
		gl.glMaterialf(GL.GL_FRONT_AND_BACK, GL.GL_SHININESS, 5);

		// Set light color
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, dim, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, white, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, black, 0);

		// Turn on light and lighting
		gl.glEnable(GL.GL_LIGHT0);
		gl.glEnable(GL.GL_LIGHTING);

		// Allow glColor() to affect current diffuse material
		gl.glColorMaterial(GL.GL_FRONT_AND_BACK, GL.GL_DIFFUSE);
		gl.glEnable(GL.GL_COLOR_MATERIAL);
	}

	// Draw text info on display
	private void drawText(GL gl, GLUT glut, double t)
	{
		String message;
		DecimalFormat twodigit = new DecimalFormat("00");

		// Put orthographic matrix on projection stack
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPushMatrix();
		gl.glLoadIdentity();
		gl.glOrtho(0, 1, 0, 1, -1, 1);
		gl.glMatrixMode(GL.GL_MODELVIEW);

		// Form text
		message = new String((int)t/60 + ":" + 
				twodigit.format((int)t % 60) + "." +
				twodigit.format((int)(100 * (t - (int)t))));

		// Add on frame rate to message if it has a valid value
		double fps = computeFPS(t);
		if (fps != 0) {
			DecimalFormat fpsFormat = new DecimalFormat("0.0");

			message = message + "  (" + fpsFormat.format(fps) + " fps)";

			fpsFormat = null;
		}

		gl.glDisable(GL.GL_LIGHTING);
		gl.glDisable(GL.GL_DEPTH_TEST);

		gl.glPushMatrix();
		gl.glLoadIdentity();

		// Draw text 
		gl.glColor3d(0.8, 0.2, 0.2);
		gl.glRasterPos2d(0.01, 0.01);
		glut.glutBitmapString(glut.BITMAP_HELVETICA_18, message);
		message = null;

		// Draw bug cam label 
		if (drawBugView.value) {
			message = new String("BUG CAM");
			gl.glRasterPos2d(0.45, 0.01);
			gl.glColor3d(1.0, 1.0, 1.0);
			glut.glutBitmapString(glut.BITMAP_HELVETICA_18, message);
			message = null;
		}

		gl.glPopMatrix();

		gl.glEnable(GL.GL_DEPTH_TEST);

		// Put back original viewing matrix
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glPopMatrix();
		gl.glMatrixMode(GL.GL_MODELVIEW);
	}

	// ----------------------------------------------------------------------

	// Compute average frame rate (0.0 indicates not computed yet)
	private double[] prevT = new double[10];
	private int numPrevT = 0;

	private double computeFPS(double t)
	{
		// Restart average when animation stops
		if (t == 0 || !drawAnimation.value) {
			numPrevT = 0;
			return 0;
		}

		int which = numPrevT % prevT.length;
		double tdiff = t - prevT[which];

		prevT[which] = t;
		numPrevT++;

		// Only compute frame rate when valid
		if (numPrevT <= prevT.length || tdiff <= 0) {
			return 0;
		}

		return prevT.length / tdiff;
	}
	
	private Point2d getSafeLocation(double scale, boolean isTree, double worldRadius) {
		Point2d pos = getNewLocation(scale, isTree, worldRadius);
		double r = scale;
		if (isTree)
			r = treeRadius;
		
		for (int i=0; i < obstacles.size(); i++) {
			Obstacle obstacle = obstacles.elementAt(i);
			Point3d obPos = obstacle.getLocation();
			double obR = 1.0;
			// if the obstacle is a tree, the radius is constant
			if (obstacle instanceof Tree) {
				obR = treeRadius;
			// if it's a rock, it is equivalent to scale
			} else if (obstacle instanceof Rock) {
				Rock temp = (Rock) obstacle;
				obR = temp.getRockScale();
			}
			
			if (collisionDetected(pos, new Point2d(obPos.x, obPos.y), r, obR)) {
				pos = null;
				pos = getNewLocation(scale, isTree, worldRadius);
				i = -1;
			}
			// for attraction point, make sure it's not too close to the predator
			if (predator!=null) {
				Point3d predLoc = predator.getLocation();
				if (collisionDetected(pos, new Point2d(predLoc.x, predLoc.y), r, predator.scale+0.25)) {
					pos = null;
					pos = getNewLocation(scale, isTree, worldRadius);
					i = -1;
				}
				predLoc = null;
			}
			// likewise for mainBug
			if (mainBug!=null) {
				Point3d mainLoc = mainBug.getLocation();
				if (collisionDetected(pos, new Point2d(mainLoc.x, mainLoc.y), r, mainBug.scale+0.25)) {
					pos = null;
					pos = getNewLocation(scale, isTree, worldRadius);
					i = -1;
				}
				mainLoc = null;
			}
		}
		return pos;
	}
	
	private Point2d getNewLocation(double scale, boolean isTree, double worldRadius) {
		double newR;
		if (isTree)
			newR = rgen.nextGaussian() * (worldRadius - treeRadius);
		else
			newR = rgen.nextGaussian() * (worldRadius - scale);
		double newA = rgen.nextDouble() * 360.0;
		
		return new Point2d(newR * Math.cos(newA), newR * Math.sin(newA));
	}
	
	private boolean collisionDetected(Point2d p1, Point2d p2, double r1, double r2) {
		Vector2d d = new Vector2d(p2.x-p1.x, p2.y-p1.y);
		double distance = d.length() - (r1 + r2);
		return distance <= 0;
	}
}
