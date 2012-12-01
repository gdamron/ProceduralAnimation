/* class Critter
 * This abstract class implements methods for keeping track of the position,
 * velocity and acceleration of a critter (such as a bug), for integrating
 * these quantities over time, and for computing accelerations that give
 * the bug wandering behavior
 *
 * Doug DeCarlo
 */

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.vecmath.*;
import java.util.*;

abstract class Critter
{
    // Position, velocity, acceleration
    Point3d pos;
    Point3d prevPos;
    Vector3d vel, acc;

    // Total distance traveled (used for keyframing)
    double dist;

    // Random number generator
    Random rgen;
    
    // for drawing shadows
    boolean shadow;

    // ---------------------------------------------------------------

    // Constructor
    public Critter(Random randomGen)
    {
	pos = new Point3d();
	prevPos = new Point3d();
	vel = new Vector3d();
	acc = new Vector3d();

	dist = 0;

	rgen = randomGen;
	shadow = false;
    }

    // Method to draw critter
    abstract void draw(GL gl);

    // Method to do keyframe animation
    abstract void keyframe(double t);

    // ---------------------------------------------------------------

    // Return location of critter
    public Point3d getLocation()
    {
	return pos;
    }

    // Method to integrate acc to get updated vel and pos;
    // also computes the distance traveled
    // (assumes acc is already computed)
    public void integrate(double dt)
    {
    	// Euler integration
    	Vector3d new_velocity = new Vector3d();
    	Point3d new_position = new Point3d();
    	Vector3d curr_distance = new Vector3d();

    	new_velocity.x = vel.x + acc.x * dt;
    	new_velocity.y = vel.y + acc.y * dt;
    	new_velocity.z = 0;

    	new_position.x = pos.x + vel.x * dt;
    	new_position.y = pos.y + vel.y * dt;
    	new_position.z = 0;
    	
    	curr_distance.x = new_position.x - pos.x;
    	curr_distance.y = new_position.y - pos.y;
    	curr_distance.z = 0;
    	
    	vel = new_velocity;
    	prevPos = pos;
    	pos = new_position;
    	dist += curr_distance.length();
    	
    	curr_distance = null;
    	
	
    }

    // Accessor for total distance traveled by bug
    public double distTraveled()
    {
	return dist;
    }

    // ---------------------------------------------------------------

    // Reset acceleration to zero
    public void accelReset()
    {
	acc.set(0,0,0);
    }

    // Add in viscous drag (assume mass of 1):  a += -k v   (k > 0)
    public void accelDrag(double k)
    {
        // Add viscous drag to acceleration acc
    	if (k>0) {
    		acc.x += -k * vel.x;
        	acc.y += -k * vel.y;
        	acc.z = 0;
    	}
    }

    // Add in attraction acceleration:  a+= direction * (k*dist^exp)
    // (negative values of k produce repulsion)
    public void accelAttract(Point3d p, double critterRadius, double obstacleRadius, double k, double exp)
    {
    	double maxVel = 40.0;
        Vector3d direction = new Vector3d(p.x-pos.x, p.y-pos.y, 0);
        double curr_dist = direction.length() - critterRadius - obstacleRadius;
        direction.normalize();
        
        acc.x += direction.x * (k*Math.pow(curr_dist, exp));
        acc.y += direction.y * (k*Math.pow(curr_dist, exp));
        
        if (acc.x>maxVel)
        	acc.x = maxVel;
        if (acc.x<-maxVel)
        	acc.x = -maxVel;
        
        if (acc.y>maxVel)
        	acc.y = maxVel;
        if (acc.y<-maxVel)
        	acc.y = -maxVel;
        
        acc.z = 0;
    }

}
