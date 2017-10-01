package APCSrobocode;

import java.util.Enumeration;
import java.util.Hashtable;

import robocode.*;
import robocode.util.Utils;

public class CrcTrgtMinRskBltShldLstScn extends AdvancedRobot {
	final int NUMBER_OF_ENEMIES = 20;
	static Hashtable<String, enemy> enHashtable = new Hashtable<String, enemy>();
	static enemy target;
	static double scanDir;
	static Object sought;
	double curX, curY, nextX, nextY, lastX, lastY;
	double curNRG;
	double fieldX, fieldY;
	double distToTarget;
	
	public void run() {
		scanDir = 1;
	    setAdjustRadarForGunTurn(true);
	    setAdjustRadarForRobotTurn(true);
	    setAdjustGunForRobotTurn(true);
	    fieldX = getBattleFieldWidth();
	    fieldY = getBattleFieldHeight();
	    nextX = lastX = curX = getX();
	    nextY = lastY = curY = getY();
		while(true) {
			curX = getX();
			curY = getY();
			curNRG = getEnergy();
			setTurnRadarRightRadians(scanDir * Double.POSITIVE_INFINITY);
			if (target != null) {
				distToTarget = Math.hypot(target.enX-curX, target.enY-curY);
				shoot();
			}
			if (getTime() > 9)
				move();
			execute();
		}
	}
	
	public void shoot() {
		double firePower = Math.min(target.enNRG/3, Math.min(curNRG/6, 1300/target.enDist));
		if (getGunTurnRemaining() == 0 && curNRG > firePower) {
		    setFire(firePower);
		}
		setTurnGunRightRadians(Utils.normalRelativeAngle(calcAngle(curX, curY, target.enX, target.enY) - getGunHeadingRadians()));
		//Head-on targeting
	}
	
	public void move() {
		double distToDest = Math.hypot(nextX-curX, nextY-curY);
		if (distToDest < 15) {
			double[] testPoint;
			double lowestRisk = Double.MAX_VALUE;
			for (int i=0; i<100; i++) {
				testPoint = pickPoint(curX, curY, 2*Math.PI*Math.random(), 200+Math.random()*400);
				System.out.println(testPoint[0]+" "+testPoint[1]);
				if (testPoint[0] > 30 && testPoint[0] < fieldX-30 && testPoint[1] > 30 && testPoint[1] < fieldY-30) {
					double curRisk = calcRisk(testPoint);
					if (curRisk < lowestRisk) {
						nextX = testPoint[0];
						nextY = testPoint[1];
						lowestRisk = curRisk;
					}
				}
			}
			System.out.println("curX: "+curX+" curY: "+curY+" nextX: "+nextX+" nextY: "+nextY);
			lastX = curX;
			lastY = curY;
		} else {
			double ang = calcAngle(curX, curY, nextX, nextY)-getHeadingRadians();
			double dir = 1;
			
			if (Math.cos(ang) < 0) {
				dir = -1;
				ang += Math.PI;
			}
			
			setAhead(distToDest*dir);
			setTurnRightRadians(ang = Utils.normalRelativeAngle(ang));
		}
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		String name = e.getName();
		enemy justScanned;
		
		if (enHashtable.containsKey(name)) {
			justScanned = enHashtable.get(name);
			justScanned.enDist = e.getDistance();
			justScanned.enNRG = e.getEnergy();
			justScanned.enHeading = e.getBearingRadians() + getHeadingRadians();
			justScanned.enX = curX + Math.sin(justScanned.enHeading)*justScanned.enDist;
			justScanned.enY = curY + Math.cos(justScanned.enHeading)*justScanned.enDist;
		} else {
			enHashtable.put(name, new enemy(e.getEnergy(), e.getDistance(), e.getBearingRadians() + getHeadingRadians()));
			justScanned = enHashtable.get(name);
			target = justScanned;
		}
	    
	    if ((name == sought || sought == null) && enHashtable.size() == getOthers()) {
	    	scanDir = Utils.normalRelativeAngle(enHashtable.values().iterator().next().enHeading - getRadarHeadingRadians());
	    	sought = enHashtable.keys().nextElement();
	    }
	    
	    if (target == null)
	    	target = justScanned;
	    //Radar turning code derived from Oldest Scanned method found on Robowiki: http://robowiki.net/wiki/Melee_Radar#Oldest_Scanned
	}
	
	public void onRobotDeath(RobotDeathEvent e) {
		enHashtable.remove(e.getName());
	    sought = null;
	    target = null;
	}
	
	public double calcRisk(double[] coor) {
		double dangFactor = Math.pow(Math.hypot(coor[0]-lastX, coor[1]-lastY), 2);
		Enumeration<enemy> enEnum = enHashtable.elements();
		
		while(enEnum.hasMoreElements()) {
			enemy nextEn = (enemy) enEnum.nextElement();
			dangFactor += nextEn.enNRG*(1+Math.abs(Math.cos(calcAngle(coor[0], coor[1], curX, curY)-calcAngle(coor[0], coor[1], target.enX, target.enY))))/Math.pow(nextEn.enDist, 2.5);
		}
		
		return dangFactor;
	}
	
	//Util functions
	public double calcAngle(double x1, double y1, double x2, double y2) {
		return Math.atan2(x2-x1, y2-y1);
	}
	
	public double[] pickPoint(double x1, double y1, double angle, double dist) {
		return new double[]{x1+dist*Math.sin(angle), y1+dist*Math.cos(angle)};
	}
	
	//Store values of enemies
	public class enemy {
		public double enX;
		public double enY;
		public double enNRG;
		public double enDist;
		public double enHeading;
		public enemy(double aNRG, double aDist, double aHeading) {
			enNRG = aNRG;
			enDist = aDist;
			enHeading = aHeading;
			enX = curX + Math.sin(aHeading)*aDist;
			enY = curY + Math.cos(aHeading)*aDist;
		}
	}
}
