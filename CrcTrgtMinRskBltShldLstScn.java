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
	double curNRG, curTime;
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
			curTime = getTime();
			setTurnRadarRightRadians(scanDir * Double.POSITIVE_INFINITY);
			if (target != null) {
				distToTarget = Math.hypot(target.enX-curX, target.enY-curY);
				shoot();
			}
			if (curTime > 9)
				move();
			execute();
		}
	}
	
	public void shoot() {
		System.out.println(target.enName);
		if (curTime > target.lastScanned + 9) {
			target = null;
			return;
		}
		System.out.println(target.enName);
		double fraction;
		double lowBound = 50;
		double highBound = 1000;
		if (target.enDist > highBound)
			fraction = 0;
		else if (target.enDist < lowBound)
			fraction = 1;
		else
			fraction = (target.enDist-lowBound)/(highBound-lowBound);
		double firePower = Math.min(3, target.enNRG);
		double bVelocity = 20-3*firePower;
		if (getGunTurnRemaining() == 0 && curNRG > firePower) {
		    setFire(firePower);
		}
		double ang = (Utils.normalRelativeAngle(target.enHeading - getGunHeadingRadians() + Math.asin((target.enVel * Math.sin(target.eventHeading - target.enHeading) / bVelocity)))*fraction);
		setTurnGunRightRadians(ang);
		System.out.println(ang);
		//Modified linear targeting
	}
	
	public void move() {
		double distToDest = Math.hypot(nextX-curX, nextY-curY);
		if (distToDest < 50) {
			double[] testPoint;
			double lowestRisk = Double.MAX_VALUE;
			for (int i=0; i<500; i++) {
				if (target != null) {
					testPoint = pickPoint(curX, curY, 2*Math.PI*Math.random(), Math.min(0.9*target.enDist, 200+Math.random()*400));
					System.out.println(testPoint[0]+" "+testPoint[1]);
					if (testPoint[0] > 30 && testPoint[0] < fieldX-30 && testPoint[1] > 30 && testPoint[1] < fieldY-30) {
						double curRisk = calcRisk(testPoint);
						if (curRisk < lowestRisk) {
							nextX = testPoint[0];
							nextY = testPoint[1];
							lowestRisk = curRisk;
						}
					}
				} else {
					testPoint = pickPoint(curX, curY, 2*Math.PI*Math.random(), 400+Math.random()*400);
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
		//Minimum risk movement
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		String name = e.getName();
		enemy justScanned;
		
		if (enHashtable.containsKey(name)) {
			justScanned = enHashtable.get(name);
			justScanned.enDist = e.getDistance();
			justScanned.enNRG = e.getEnergy();
			justScanned.enHeading = e.getBearingRadians() + getHeadingRadians();
			justScanned.enVel = e.getVelocity();
			justScanned.eventHeading = e.getHeading();
			justScanned.enX = curX + Math.sin(justScanned.enHeading)*justScanned.enDist;
			justScanned.enY = curY + Math.cos(justScanned.enHeading)*justScanned.enDist;
			justScanned.lastScanned = curTime;
		} else {
			enHashtable.put(name, new enemy(e.getEnergy(), e.getDistance(), e.getBearingRadians() + getHeadingRadians(), e.getVelocity(), e.getHeading(), getTime()));
			justScanned = enHashtable.get(name);
			target = justScanned;
			justScanned.enName = name;
		}
	    
	    if ((name == sought || sought == null) && enHashtable.size() == getOthers()) {
	    	scanDir = Utils.normalRelativeAngle(enHashtable.values().iterator().next().enHeading - getRadarHeadingRadians());
	    	sought = enHashtable.keys().nextElement();
	    }
	    
	    if (target == null) {
	    	double lowNRG = Double.MAX_VALUE;
	    	String tarName = "";
	    	for (int i=0; i<enHashtable.size(); i++) {
	    		enemy nextEn = enHashtable.values().iterator().next();
	    		if (nextEn.enNRG < lowNRG) {
	    			lowNRG = nextEn.enNRG; 
	    			tarName = nextEn.enName;
	    		}
	    	}
	    	target = enHashtable.get(tarName);
	    }
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
			if (target != null)
				dangFactor += Math.min(nextEn.enNRG, 2)*(1+Math.abs(Math.cos(calcAngle(coor[0], coor[1], curX, curY)-calcAngle(coor[0], coor[1], target.enX, target.enY))))/Math.pow(nextEn.enDist, 2);
			else
				dangFactor += nextEn.enNRG/Math.pow(nextEn.enDist, 2);
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
		public double enVel;
		public double eventHeading;
		public String enName;
		public double lastScanned;
		public enemy(double aNRG, double aDist, double aHeading, double aVel, double aEHeading, double aScanned) {
			enNRG = aNRG;
			enDist = aDist;
			enHeading = aHeading;
			enVel = aVel;
			eventHeading = aEHeading;
			lastScanned = aScanned;
			enX = curX + Math.sin(aHeading)*aDist;
			enY = curY + Math.cos(aHeading)*aDist;
		}
	}
}
