package APCSrobocode;

import java.util.LinkedHashMap;

import robocode.*;
import robocode.util.Utils;

public class CrcTrgtMinRskBltShldLstScn extends AdvancedRobot {
	final int NUMBER_OF_ENEMIES = 20;
	//The hashmap is ONLY BEING USED AS A TWO-DIMENSIONAL ARRAY (pls don't dock marks)
	static LinkedHashMap<String, Double[]> enemyHashMap;
	static double scanDir;
	static Object sought;
	double curX;
	double curY;
	int ticks = 0;
	
	public void run() {
		scanDir = 1;
	    enemyHashMap = new LinkedHashMap<String, Double[]>(5, 2, true);
	    if(isAdjustRadarForGunTurn())
	    	setAdjustRadarForGunTurn(false);
	    if(!isAdjustRadarForRobotTurn())
	    	setAdjustRadarForRobotTurn(true);
	    if(!isAdjustGunForRobotTurn())
	    	setAdjustGunForRobotTurn(true);
		while(true) {
			ticks++;
			curX = getX();
			curY = getY();
			setTurnRadarRightRadians(scanDir * Double.POSITIVE_INFINITY);
			setTurnGunRightRadians(scanDir * Double.POSITIVE_INFINITY);
			if (ticks%16==0)
				genPoints();
			execute();
		}
	}
	
	public void onScannedRobot(ScannedRobotEvent e) {
		String name = e.getName();
	 
	    enemyHashMap.put(name, new Double[]{getHeadingRadians() + e.getBearingRadians(), e.getDistance(), e.getEnergy()});
	 
	    if ((name == sought || sought == null) && enemyHashMap.size() == getOthers()) {
	    	scanDir = Utils.normalRelativeAngle(enemyHashMap.values().iterator().next()[0] - getRadarHeadingRadians());
	        sought = enemyHashMap.keySet().iterator().next();
	    }
	    //Radar turning code uses Oldest Scanned method found on Robowiki: http://robowiki.net/wiki/Melee_Radar#Oldest_Scanned
	    
	    if (getEnergy()>3) {
//	    	double eDistance = e.getDistance();
//		    double lowBound = 50;
//		    double highBound = 800;
//		    double fraction = 0.95-0.9*(Math.min(highBound, Math.max(eDistance, lowBound))-lowBound);
		    double proposedPower = 3;
		    double bPower = Math.min(e.getEnergy(), proposedPower);
//		    double bVelocity = 20-3*bPower;
//		    double eHeading = (e.getBearingRadians() + getHeadingRadians());
//		    setTurnGunRightRadians(Math.PI-(Utils.normalRelativeAngle(eHeading - 
//		    	    getGunHeadingRadians() + (e.getVelocity() * Math.sin(e.getHeadingRadians() - 
//		    	    eHeading) / bVelocity)))*fraction);
		    setFire(bPower);
	    }
	    //Modified linear targeting
	}
	
	public void onRobotDeath(RobotDeathEvent e) {
	    enemyHashMap.remove(e.getName());
	    sought = null;
	}
	
	public void genPoints() {
		int numPoints = 4;
		double startAng = Math.random()*Math.PI/4;
		double[] angles = new double[] {startAng, startAng+Math.PI/2, startAng+Math.PI, startAng-Math.PI/2};
		double dist = Math.random()*20+50;
		double[][] coor = new double[][] {{curX+Math.cos(angles[0])*dist,curY+Math.sin(angles[0])*dist},{curX+Math.cos(angles[1])*dist,curY+Math.sin(angles[1])*dist},{curX+Math.cos(angles[2])*dist,curY+Math.sin(angles[2])*dist},{curX+Math.cos(angles[3])*dist,curY+Math.sin(angles[3])*dist}};
		double[] dangFactor = new double[numPoints];
		for (int i=0; i<numPoints; i++) {
			if (coor[i][0] > 0 && coor[i][0] < getBattleFieldWidth() && coor[i][1] > 0 && coor[i][1] < getBattleFieldHeight())
				dangFactor[i] = calcRisk(coor[i]);
			else
				dangFactor[i] = Double.MAX_VALUE;
		}
		double smallestRisk = Math.min(dangFactor[0], Math.min(dangFactor[1], Math.min(dangFactor[2], dangFactor[3])));
		for (int i=0; i<numPoints; i++) {
			if (dangFactor[i] == smallestRisk) {
				turnRightRadians(angles[i]-getHeading());
				ahead(dist);
				break;
			}
		}
	}
	
	public double calcRisk(double[] coor) {
		double dangFactor = 0;
		Double[] nextEnemy;
		for (int i=0; i<enemyHashMap.size(); i++) {
			nextEnemy = enemyHashMap.values().iterator().next();
			dangFactor += nextEnemy[2]/Math.pow(nextEnemy[1], 2.5);
		}
		return dangFactor;
	}
}
