package burger;

import model.ComponentType;
import model.Situated;
import components.Turtlebot;
import model.EmptyCell;
import model.UnknownCell;
import model.Grid;

import mqtt.Message;
import rosbridgeConnection.RosbridgeClient;

import model.ObstacleDescriptor;
import model.RobotDescriptor;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.List;

public class RealTurtlebot extends SmartTurtlebot{
	protected Grid grid;
	protected RosbridgeClient clientRosbridge;
	public static int waitTimeCommunication = 300;
	public static int waitTimeAction= 3000;
	public static String ip = "10.200.3.101"; //"10.3.143.1";
	public static String port = "9090";
	public String prefix;
	public boolean myactionresult;

	public RealTurtlebot(int id, String name, int seed, int field, Message clientMqtt, int debug) {
		super(id, name, seed, field, clientMqtt, debug);
		myactionresult = false;
		clientRosbridge = new RosbridgeClient(RealTurtlebot.ip, RealTurtlebot.port, this);
		try{
			Thread.sleep(1000);
		}catch(Exception e){
			System.out.println(e);
		}
	}

	public void inform() {
		myactionresult = true;
	}
	
	public void handleMessage(String topic, JSONObject content){				
		super.handleMessage(topic, content);
        if (topic.contains(name+"/position/init")) {
        	prefix = (String)content.get("prefix");
   			JSONObject message = new JSONObject();
			message.put("topic", "/"+prefix+"/burger_move/result");
			message.put("op", "subscribe");
			message.put("type", "burger_move_action/Burger_moveActionResult");
			clientRosbridge.getWsc().send(message.toJSONString());
        }        
	}


	public String toString() {
		return "{" + name + "; " + id + "; " + x + "; " + y + "; " + orientation + "; " + prefix + "}";
	}

	public void moveForward() {
		try {
			/*JSONObject message = new JSONObject();
			message.put("topic", "/" + prefix + "/robot_command");
			JSONObject msg = new JSONObject();
			msg.put("data", "forward");
			message.put("msg", msg);
			message.put("op", "publish");*/
			JSONObject message = new JSONObject();
			message.put("topic", "/" + prefix + "/burger_move/goal");
			//message.put("type", "burger_move_action/Burger_moveActionGoal");
			JSONObject msg = new JSONObject();
			JSONObject jos = new JSONObject();			
			jos.put("secs",0);
			jos.put("nsecs",0);
			JSONObject joh = new JSONObject();			
			joh.put("seq",0);
			joh.put("stamp",jos);
			joh.put("frame_id","");
			msg.put("header", joh);
			JSONObject joi = new JSONObject();			
			joi.put("stamp",jos);
			joi.put("id","");
			msg.put("goal_id", joi);
			JSONObject jog = new JSONObject();			
			jog.put("distance",30);
			jog.put("direction","forward");
			msg.put("goal", jog);			
			message.put("msg", msg);
			message.put("op", "publish");
			clientRosbridge.getWsc().send(message.toJSONString());
			System.out.println("Message ROS" + message.toJSONString());
			while(!myactionresult){
				Thread.sleep(RealTurtlebot.waitTimeAction);
			}
			myactionresult = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.moveForward();
	}

	public void moveBackward() {
		try {
			JSONObject message = new JSONObject();
			message.put("topic", "/" + prefix + "/robot_command");
			JSONObject msg = new JSONObject();
			msg.put("data", "backward");
			message.put("msg", msg);
			message.put("op", "publish");
			clientRosbridge.getWsc().send(message.toJSONString());
			while(!myactionresult){
				Thread.sleep(RealTurtlebot.waitTimeAction);
			}
			myactionresult = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.moveBackward();
	}

	/* Rotate the robot to the left */
	public void moveLeft() {
		try {
			JSONObject message = new JSONObject();
			message.put("topic", "/" + prefix + "/burger_move/goal");
			//message.put("type", "burger_move_action/Burger_moveActionGoal");
			JSONObject msg = new JSONObject();
			JSONObject jos = new JSONObject();			
			jos.put("secs",0);
			jos.put("nsecs",0);
			JSONObject joh = new JSONObject();			
			joh.put("seq",0);
			joh.put("stamp",jos);
			joh.put("frame_id","");
			msg.put("header", joh);
			JSONObject joi = new JSONObject();			
			joi.put("stamp",jos);
			joi.put("id","");
			msg.put("goal_id", joi);
			JSONObject jog = new JSONObject();			
			jog.put("distance",155);
			jog.put("direction","left");
			msg.put("goal", jog);			
			message.put("msg", msg);
			message.put("op", "publish");
			clientRosbridge.getWsc().send(message.toJSONString());
			//Thread.sleep(RealTurtlebot.waitTimeAction);
			while(!myactionresult){
				Thread.sleep(RealTurtlebot.waitTimeAction);
			}
			myactionresult = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.moveLeft();
	}

	/* Rotate the robot to the right */
	public void moveRight() {
		try {
			JSONObject message = new JSONObject();
			message.put("topic", "/" + prefix + "/burger_move/goal");
			//message.put("type", "burger_move_action/Burger_moveActionGoal");
			JSONObject msg = new JSONObject();
			JSONObject jos = new JSONObject();			
			jos.put("secs",0);
			jos.put("nsecs",0);
			JSONObject joh = new JSONObject();			
			joh.put("seq",0);
			joh.put("stamp",jos);
			joh.put("frame_id","");
			msg.put("header", joh);
			JSONObject joi = new JSONObject();			
			joi.put("stamp",jos);
			joi.put("id","");
			msg.put("goal_id", joi);
			JSONObject jog = new JSONObject();			
			jog.put("distance",155);
			jog.put("direction","right");
			msg.put("goal", jog);			
			message.put("msg", msg);
			message.put("op", "publish");
			clientRosbridge.getWsc().send(message.toJSONString());		
			//Thread.sleep(RealTurtlebot.waitTimeAction);
			while(!myactionresult){
				Thread.sleep(RealTurtlebot.waitTimeAction);
			}
			myactionresult = false;
		} catch (Exception e) {
			e.printStackTrace();
		}
		super.moveRight();
	}


	/* Stop the robot */
	public void stopRobot() {
		JSONObject message = new JSONObject();
		message.put("topic", "/" + name + "/cmd_vel");
		JSONObject msg = new JSONObject();
		JSONObject linear = new JSONObject();
		linear.put("x", 0.0);
		linear.put("y", 0.0);
		linear.put("z", 0.0);
		JSONObject angular = new JSONObject();
		angular.put("x", 0.0);
		angular.put("y", 0.0);
		angular.put("z", 0.0);

		msg.put("linear", linear);
		msg.put("angular", angular);

		message.put("msg", msg);

		message.put("op", "publish");

		clientRosbridge.getWsc().send(message.toJSONString());
	}	

	/* Move the robot straight */
	public void moveRobot() {
		JSONObject message = new JSONObject();
		message.put("topic", "/" + name + "/cmd_vel");
		JSONObject msg = new JSONObject();
		JSONObject twist1 = new JSONObject();
		JSONObject linear = new JSONObject();
		linear.put("x", 0.040);
		linear.put("y", 0.0);
		linear.put("z", 0.0);
		JSONObject angular = new JSONObject();
		angular.put("x", 0.0);
		angular.put("y", 0.0);
		angular.put("z", 0.0);

		twist1.put("linear", linear);
		twist1.put("angular", angular);

		msg.put("linear", linear);
		msg.put("angular", angular);

		message.put("msg", msg);

		message.put("op", "publish");

		clientRosbridge.getWsc().send(message.toJSONString());

		try {
			Thread.sleep(RealTurtlebot.waitTimeAction);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		stopRobot();
	}

	/* Rotate the robot to the left */
	public void rotateLeft(int step) {
		for (int i = 0; i < step; i++) {
			JSONObject message = new JSONObject();
			message.put("topic", "/" + name + "/cmd_vel");
			JSONObject msg = new JSONObject();
			JSONObject linear = new JSONObject();
			linear.put("x", 0.0);
			linear.put("y", 0.0);
			linear.put("z", 0.0);
			JSONObject angular = new JSONObject();
			angular.put("x", 0.0);
			angular.put("y", 0.0);
			angular.put("z", 1.0);

			msg.put("linear", linear);
			msg.put("angular", angular);

			message.put("msg", msg);

			message.put("op", "publish");

			clientRosbridge.getWsc().send(message.toJSONString());

			/*
			 * Time required to rotate the robot 90 degrees while doing 1m/s is 1.6 seconds
			 */
			try {
				Thread.sleep(RealTurtlebot.waitTimeCommunication);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			stopRobot();
		}
	}

	/* Rotate the robot to the right */
	public void rotateRight(int step) {
		for (int i = 0; i < step; i++) {
			JSONObject message = new JSONObject();
			message.put("topic", "/" + name + "/cmd_vel");
			JSONObject msg = new JSONObject();
			JSONObject linear = new JSONObject();
			linear.put("x", 0.0);
			linear.put("y", 0.0);
			linear.put("z", 0.0);
			JSONObject angular = new JSONObject();
			angular.put("x", 0.0);
			angular.put("y", 0.0);
			angular.put("z", -1.0);

			msg.put("linear", linear);
			msg.put("angular", angular);

			message.put("msg", msg);

			message.put("op", "publish");

			clientRosbridge.getWsc().send(message.toJSONString());

			/*
			 * Time required to rotate the robot 90 degrees while doing 1m/s is 1.6 seconds
			 */
			try {
				Thread.sleep(RealTurtlebot.waitTimeCommunication);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			stopRobot();
		}
	}
}