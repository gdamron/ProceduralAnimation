/* class WorldView
 * The OpenGL drawing component of the interface
 *
 * Doug DeCarlo
 */

import java.awt.Window;
import java.lang.Math;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;

public class WorldView extends SimpleGLCanvas
{
    Scene s;
    static boolean inited = false;

    public WorldView(Window parent, Scene sc)
    {
        super(parent);

        s = sc;
    }

    // Set animation status; call this method whenever animation status
    // changes to stop/start clock and regular animation callbacks to
    // redraw window
    public void setAnimation()
    {
        if (s.drawAnimation.value) {
            s.pauseClock(false);
            if (!isAnimated()) {
                setAnimation(true);
            }
        } else {
            if (isAnimated()) {
                setAnimation(false);
                s.pauseClock(true);
            }
        }
    }

    public void init(GL gl)
    {
        // Starts with animation turned off
        setAnimation(false);

        // --- OpenGL Initialization

	// Set background color to sky blue
	gl.glClearColor(0.58f, 0.74f, 0.98f, 0.0f);

        // Turn on Z buffer
        gl.glEnable(GL.GL_DEPTH_TEST);

        // Turn on Gouraud shaded polygons
        gl.glShadeModel(GL.GL_SMOOTH);

	// Turn on automatic normalization for normal vectors
	gl.glEnable(GL.GL_NORMALIZE);
    }

    // ------------------------------------------------------------

    // Method for handling window resizing
    public void projection(GL gl, int width, int height)
    {
        gl.glViewport(0, 0, width, height);

        double aspect = (double)width / height;
        double l, r, b, t, n;
        // Move near plane closer, but also shrink frustum so that
        // field-of-view stays the same -- this way not much stuff
        // gets clipped
        double zoom = 10;

        // Preserve aspect ratio
        if (aspect > 1) {
            r = aspect/zoom;
            t = 1/zoom;
        } else {
            r = 1/zoom;
            t = 1/(zoom*aspect);
        }
        // Window has (0,0) in center
        l = -r;
        b = -t;

        // Set near plane location
        // (making this smaller reduces the field-of-view)
        n = 5;

        // Set the world projection
        // if in bug cam, make field of vision a little bigger
        if (s.drawBugView.value) {
        	gl.glMatrixMode(GL.GL_PROJECTION);
        	gl.glLoadIdentity();
        	gl.glFrustum(l, r, b, t, n, 20);
        	gl.glMatrixMode(GL.GL_MODELVIEW);
        } else {
        	gl.glMatrixMode(GL.GL_PROJECTION);
        	gl.glLoadIdentity();
        	gl.glFrustum(l, r, b, t, n / zoom, 500);
        	gl.glMatrixMode(GL.GL_MODELVIEW);
        }
    }

    // Method for drawing the contents of the window
    public void draw(GL gl)
    {
        // Clear the window and depth buffer
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        if (!inited) {
            // Initialize scene objects (sphere/cylinder)
            Objs.initialize(gl, 16, true);
            inited = true;
        }

        // Draw the scene
        s.draw(gl, glut);
   }
}
