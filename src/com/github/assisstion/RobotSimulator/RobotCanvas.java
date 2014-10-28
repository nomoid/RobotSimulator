package com.github.assisstion.RobotSimulator;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.swing.JPanel;

public class RobotCanvas extends JPanel implements Runnable, Printable, KeyListener{

	protected float n = 0.50f;
	protected float multi = 10;
	protected Vector2 subPoint = new Vector2(n * multi, 0);
	protected Vector2 currentPoint = new Vector2(300, 300);
	//In radians; 0 is top, pi/2 is right
	protected float direction = (float)(Math.PI / 2);
	protected Set<Pair<Integer, Integer>> newPoints = new ConcurrentSkipListSet<Pair<Integer, Integer>>();
	protected Set<Pair<Integer, Integer>> points = new ConcurrentSkipListSet<Pair<Integer, Integer>>();

	protected float leftSpeed = 90f;
	protected float rightSpeed = 100f;

	//protected int speedMultiplier = 100;
	protected int updatesPerSecond = 300;
	protected int updatesPerPaint = 5;

	private Object pauseLock = new Object();
	protected boolean paused;
	protected boolean enabled;

	/**
	 *
	 */
	private static final long serialVersionUID = -9117978120156221878L;

	public RobotCanvas(){
		enabled = true;
		new Thread(this).start();
	}

	@Override
	public void setEnabled(boolean b){
		enabled = b;
	}

	public boolean getEnabled(){
		return enabled;
	}

	public void setPaused(boolean b){
		synchronized(pauseLock){
			if(paused == b){
				return;
			}
			paused = b;
			if(!b){
				notifyAll();
			}
		}
	}

	public boolean isPaused(){
		synchronized(pauseLock){
			return paused;
		}
	}



	@Override
	public int print(Graphics graphics, PageFormat pageFormat, int pageIndex)
			throws PrinterException{
		//Only one page is printed
		if(pageIndex > 0){
			return Printable.NO_SUCH_PAGE;
		}
		Graphics2D g2d = (Graphics2D) graphics;
		//Translate into imageable area
		g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
		draw(graphics);
		//Apply borders
		Color tc = g2d.getColor();
		g2d.setColor(Color.BLACK);
		g2d.drawRect(0, 0, (int) pageFormat.getImageableWidth(),
				(int) pageFormat.getImageableHeight());
		g2d.setColor(tc);
		return Printable.PAGE_EXISTS;
	}

	@Override
	public void paint(Graphics g){
		draw(g);
	}

	//Function for drawing called by paint and print
	public void draw(Graphics g){
		Graphics2D g2d = (Graphics2D) g;
		g2d.setBackground(Color.WHITE);
		g2d.clearRect(0, 0, getWidth(), getHeight());
		//g2d.setColor(Color.BLACK);
		//g2d.fillRect(0, 0, 10, 10);
		points.addAll(newPoints);
		newPoints.clear();
		for(Pair<Integer, Integer> point : points){
			g2d.drawLine(point.getValueOne(), point.getValueTwo(),
					point.getValueOne(), point.getValueTwo());
		}
		g2d.setColor(Color.RED);
		g2d.fillOval((int) currentPoint.x - 2, (int) currentPoint.y - 2,
				4, 4);
		g2d.setColor(Color.GREEN);
		int subX = (int) (currentPoint.x - Math.cos(direction) * subPoint.x
				+ Math.sin(direction) * subPoint.y);
		int subY = (int)(currentPoint.y - Math.cos(direction) * subPoint.y
				- Math.sin(direction) * subPoint.x);
		g2d.fillOval(subX - 2, subY - 2, 4, 4);

	}


	public void updateMotion(){
		float roc;
		if(leftSpeed == rightSpeed){
			roc = 0;
		}
		else{
			//if(outSpeed == 0){
			//	roc = 1000000000f;
			//movement = 0;
			//}
			//if(inSpeed < outSpeed){
			roc = (leftSpeed - rightSpeed) / n;
			////roc = 1/ (n / (inSpeed/outSpeed - 1.0f));
			//}
			//else{
			//	roc = -n / (outSpeed/inSpeed - 1.0f);
			//}
			//}
			//else{
			//	roc = -n / (outSpeed/inSpeed - 1);
			//}
		}
		direction += roc / updatesPerSecond / multi;
		//direction += roc / updatesPerSecond * outSpeed;
		float speed = rightSpeed / updatesPerSecond;
		currentPoint.y += -Math.cos(direction) * speed;
		currentPoint.x += Math.sin(direction) * speed;
		newPoints.add(Pair.make((int)currentPoint.x, (int)currentPoint.y));
		newPoints.add(Pair.make((int)(currentPoint.x - Math.cos(direction) * subPoint.x
				+ Math.sin(direction) * subPoint.y), (int)(currentPoint.y
						- Math.cos(direction) * subPoint.y
						- Math.sin(direction) * subPoint.x)));
	}

	protected int paintCounter = 0;

	@Override
	public void run(){
		while(enabled){
			while(paused){
				try{
					synchronized(pauseLock){
						wait();
					}
				}
				catch(InterruptedException e){
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			try{
				paintCounter--;
				if(paintCounter <= 0){
					repaint();
					paintCounter = updatesPerPaint;
				}
				Thread.sleep(1000 / updatesPerSecond);
			}
			catch(InterruptedException e){
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			updateMotion();
		}
	}





	@Override
	public void keyTyped(KeyEvent e){
		// TODO Auto-generated method stub

	}





	/**
	 * Keys
	 * p: pause / resume
	 * r: print
	 * q: terminate
	 * @param e
	 */
	@Override
	public void keyPressed(KeyEvent e){
		if(e.getKeyChar() == 'r'){
			try{
				boolean tempPaused;
				synchronized(pauseLock){
					tempPaused = isPaused();
					setPaused(true);
				}
				PrinterJob job = PrinterJob.getPrinterJob();
				job.setPrintable(this);
				boolean doPrint = job.printDialog();
				if(doPrint){
					job.print();
				}
				synchronized(pauseLock){
					setPaused(tempPaused);
				}
			}
			catch(PrinterException e1){
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		if(e.getKeyChar() == 'q'){
			setEnabled(false);
		}
		if(e.getKeyChar() == 'p'){
			synchronized(this){
				setPaused(!isPaused());
			}
		}
	}





	@Override
	public void keyReleased(KeyEvent e){
		// TODO Auto-generated method stub

	}
}
