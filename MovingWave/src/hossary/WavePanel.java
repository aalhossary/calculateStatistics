package hossary;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

public class WavePanel extends JPanel {
	
	public static final int xDistance = 23;//parameter
	public static final int yDistance = 23;//parameter
	public static final int r = 19;//parameter
	public static final int dotR = 3;//parameter
	
	/**gets phi for x and y (in radians)
	 * @param x number of line
	 * @param y number of raw
	 * @return phase, in radians
	 */
	private double getPhi(int x, int y) {
		return phiDownRight(x, y) /*direction*/
				*30.0        /*parameter*/
				*Math.PI/180.0/* degrees per unit*/;
	}

	int phiDownRight(int x, int y) {return -x- y;}
	int phiDownLeft (int x, int y) {return x - y;}
	int phiUpRight  (int x, int y) {return y - x;}
	int phiUpLeft   (int x, int y) {return x + y;}
	int phiRight(int x, int y) {return -x ;}
	int phiDown (int x, int y) {return -y ;}
	int phiLeft (int x, int y) {return x ;}
	int phiUp   (int x, int y) {return y ;}

	/**called every time step, to change the rotation angle
	 * (theta) internally by rotation segment
	 */
	public void tick() {
		theta=degree*Math.PI/180.0; //parameter
		degree = (degree+15) % 360;
		repaint();
	}
	////////////////////////////////End of parameters//////////////////////
	
	
	private static final long serialVersionUID = 1L;
	double degree = 0;
	double theta= 0;
	Ticker ticker = new Ticker();

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		int centerX, centerY;
		g.clearRect(0, 0, getWidth(), getHeight());
		for (int x = -1; x < ((getWidth()+xDistance)/xDistance)+1; x++) {
			for (int y = -1; y < ((getHeight()+yDistance)/yDistance)+1; y++) {
				centerX = x*xDistance + (int)(y % 2 *0.5 * xDistance); //to interlace
				centerY = y*yDistance;
				g.setColor(Color.black);
				g.drawOval(centerX-r, centerY-r, r*2, r*2);
				
				//draw dot
				double phi  = getPhi(x, y);
				g.setColor(Color.red);
				g.fillOval((int)(centerX+Math.cos(theta+phi)*r)-dotR, (int)(centerY+Math.sin(theta+phi)*r)-dotR, dotR*2, dotR*2);
				g.setColor(Color.black);
				g.drawOval((int)(centerX+Math.cos(theta+phi)*r)-dotR, (int)(centerY+Math.sin(theta+phi)*r)-dotR, dotR*2, dotR*2);
			}
		}
	}
	
	
	class Ticker extends Thread{
		public void run() {
			while (true) {
				tick();
				try {
					sleep(70);
				} catch (InterruptedException e) {
					e.printStackTrace();
				} 
			}
		};
	}
}
