/* class Bug
 * This class implements methods for drawing a 6-legged walking bug
 * in a particular configuration, given by a set of angles that describe
 * the positions of its legs.
 *
 * Doug DeCarlo
 */

import java.util.*;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLDrawable;
import javax.vecmath.*;

class Bug extends Critter
{
    // Number of legs, and number of parameters per leg
    final int legNum = 6, pNum = 2;

    // Keyframes for bug:
    // - this array specifies one cycle of motion for the bug, which
    //   lasts from T=0 to T=1.
    // - T isn't the actual time, but rather is just a measure of
    //   progress through a single cycle
    // - keyFrames[j] is a vector of angles that describes the bug's
    //   configuration at the time keyT[j].
    static double[][] keyFrames = {
	{
	    30, 10,      30, 30, 
	    0,  30,       0, 10, 
	   -30, 10,     -30, 30, 	
	},
	{
	    20, 10,      40, 30, 
	    10, 30,     -10, 10, 
	   -40, 10,     -20, 30, 	
	},
	{
	    30, 30,      30, 10, 
	    0,  10,       0, 30, 
	   -30, 30,     -30, 10, 	
	},
	{
	    40, 30,      20, 10, 
	   -10, 10,      10, 30, 
	   -20, 30,     -40, 10, 	
	},
	{ // copy of first keyFrame -- cyclic motion
	    30, 10,      30, 30, 
	    0,  30,       0, 10, 
	   -30, 10,     -30, 30,	
	}
    };
    // "Timestamps" for keyframes, in [0,1]
    static double [] keyT = {
	0.0, 0.25, 0.5, 0.75, 1.0
    };
    // Distance traveled in one T-second of keyframes for bug
    // which has scale=1
    // (used to make leg speed match bug speed)
    static double stride = 1.3;

    // --------------------------------------------------------------

    // Current bug parameters/leg angles (computed from keyframes for
    // each moment in time) using keyframe(t) method
    double[] param;

    // Bug size
    double scale;

    // Tesselation resolution of bug parts 
    static int partDetail;
    
    // predators are red
    boolean isPredator = false;

    // ---------------------------------------------------------------

    // constructor
    public Bug(Random randomGen, double bugScale,
	       double bugPx, double bugPy, double bugVx, double bugVy)
    {
	super(randomGen);

	pos.set(bugPx, bugPy, 0);
	vel.set(bugVx, bugVy, 0);
	scale = bugScale;

	param = new double[keyFrames[0].length];
	keyframe(0);
    }

    // ---------------------------------------------------------------

    // Compute bug parameters by keyframing
    // Given t, compute the corresponding value of param[]
    public void keyframe(double t)
    {
        while (t>1.0) t /= (stride*scale);
        
        int index = 0;
        for (int i = 0; i < keyT.length; i++) {
        	if (t >= keyT[i]) {
        		index=i;
        	}
        }
        
        double[] key1 = keyFrames[index];
		double[] key2 = keyFrames[(index+1)%keyT.length];
		double t1 = keyT[index];
		double t2 = keyT[(index+1)%keyT.length];
		
		for (int j=0; j < param.length; j++) {
			param[j] = key1[j] + (key2[j]-key1[j])*(t-t1)/(t2-t1);
			
		}
    }

    // --------------------------------------------------------------------

    // Transformation to place bug in scene
    public void transform(GL gl)
    {
    	gl.glTranslated(pos.x, pos.y, pos.z);
    	double a = Math.toDegrees(Math.atan2(pos.y-prevPos.y, pos.x-prevPos.x));
        gl.glRotated(a, 0, 0, 1);
        gl.glScaled(scale, scale, scale);
    }

    // ---------------------------------------------------------------
    // Draw bug in scene using current set of parameters
    public void draw(GL gl)
    {
	// Bug transform (default bug faces +x direction)
	gl.glPushMatrix();
	transform(gl);

	// Body
	gl.glPushMatrix();
	{
	    gl.glTranslated(0, 0, 0.75);
	    
	    if (isPredator)
	    	gl.glColor3d(0.1, 0.1, 0.1);
	    else
	    	gl.glColor3d(0.5, 0.7, 0.75);
	    
	    gl.glPushMatrix();
	    {
		gl.glScaled(1.3, 1.1, 1);
                Objs.sphere(gl);
	    }
	    gl.glPopMatrix();
	    
	    // Head (relative to body)
	    gl.glPushMatrix();
	    {
		gl.glTranslated(0.7, 0.0, 0.0);
		gl.glScaled(0.5, 0.5, 0.5);
		if (isPredator)
	    	gl.glColor3d(0.5, 0.1, 0.1);
	    else
            gl.glColor3d(0.65, 0.55, 0.75);
                Objs.sphere(gl);
	    }
	    gl.glPopMatrix();

	    // Legs (relative to body)

	    double legThick = 0.15;
	    if (shadow)
	    	gl.glColor3d(0.0, 0.0, 0.0);
	    else
            gl.glColor3d(0.5, 0.4, 0.3);

	    for (int i = 0; i < legNum/2; i++) {
		// Left legs
		gl.glPushMatrix();
		{
		    gl.glRotated(param[2*i*pNum],      0, 0, 1);
		    gl.glRotated(90-param[2*i*pNum+1], 1, 0, 0);
		    
		    gl.glPushMatrix();
		    {
			gl.glScaled(legThick, legThick, 1.0);
                        Objs.cylinder(gl);
		    }
		    gl.glPopMatrix();

		    gl.glTranslated(0, 0, 1);
		    gl.glRotated(90, 1, 0, 0);
		    gl.glScaled(legThick, legThick, 1.0);
                    Objs.cylinder(gl);
		}
		gl.glPopMatrix();

		// Right legs	    
		gl.glPushMatrix();
		{
		    gl.glRotated(-param[(2*i+1)*pNum],      0, 0, 1);
		    gl.glRotated(-90+param[(2*i+1)*pNum+1], 1, 0, 0);
		    
		    gl.glPushMatrix();
		    {
			gl.glScaled(legThick, legThick, 1.0);
                        Objs.cylinder(gl);
		    }
		    gl.glPopMatrix();
		    
		    gl.glTranslated(0, 0, 1.0);
		    gl.glRotated(-90, 1, 0, 0);
		    gl.glScaled(legThick, legThick, 1.0);
                    Objs.cylinder(gl);
		}
		
		gl.glPopMatrix();
	    }
	}

	// Body
	gl.glPopMatrix();

	// Bug
	gl.glPopMatrix();
    }
}
