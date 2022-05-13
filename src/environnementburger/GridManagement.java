package environnementburger;

import components.SimulationComponent;
import model.ComponentType;
import components.Obstacle;
import model.EmptyCell;
import model.UnknownCell;
import model.RobotDescriptor;
import main.TestAppli;
import model.Grid;
import model.Goal;
import model.Situated;
import mqtt.Message;

import java.awt.Color;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


public class GridManagement implements SimulationComponent {

    protected Grid grid;
    private ArrayList<Goal> goals;
    private static final String turtlebotName = "burger_";
    protected int nbObstacles;
    protected int nbRobots;
    protected Message clientMqtt;
    protected String name;
    protected int debug;
    protected int rows;
    protected int columns;
    protected int display;
    protected int displaywidth;
    protected int displayheight;
    protected String displaytitle;
    protected Color colorrobot;
    protected Color colorobstacle;
    protected Color colorgoal;
    protected Color colorother;
    protected Color colorunknown;

    ColorGrid cg;

    protected int seed;

    public void initSubscribe() {
        clientMqtt.subscribe("robot/nextPosition");
        clientMqtt.subscribe("configuration/nbRobot");
        clientMqtt.subscribe("configuration/nbObstacle");
        //clientMqtt.subscribe("configuration/nbRobot");
        clientMqtt.subscribe("configuration/seed");
        clientMqtt.subscribe("configuration/display");
        clientMqtt.subscribe("configuration/debug");
        clientMqtt.subscribe("configuration/robot/grid");
        clientMqtt.subscribe("robot/grid");
        clientMqtt.subscribe("environment/grid");
    }

    public void publishState(JSONObject content) {
        String robotName = (String) ((JSONObject) content.get("robot")).get("id");
        //JSONObject state = giveState(content,robotName);
        //clientMqtt.publish(robotName+"/robot/state", state.toJSONString());
    }

    public int isGoal(int x, int y) {
        for (Goal g : goals) {
            if (g.getX() == x && g.getY() == y)
                return g.getRobot();
        }
        return 0;
    }

    public void createColorGrid(int width, int height, String title) {
        cg = new ColorGrid(width, height, grid.getColumns(), grid.getRows(), title);
        for (int i = 0; i < grid.getRows(); i++) {
            for (int j = 0; j < grid.getColumns(); j++) {
                Situated elt = grid.getCell(i, j);
                if (elt.getComponentType() == ComponentType.empty) {
                    if (isGoal(j, i) < 0) {
                        cg.setBlockColor(j, i, colorgoal);
                    } else {
                        cg.setBlockColor(j, i, colorother);
                    }
                } else if (elt.getComponentType() == ComponentType.robot)
                    cg.setBlockColor(j, i, colorrobot);
                else if (elt.getComponentType() == ComponentType.obstacle)
                    cg.setBlockColor(j, i, colorobstacle);
                else
                    cg.setBlockColor(j, i, colorunknown);
            }
        }
        cg.init();
    }

    public GridManagement() {
        this.name = "grid";
        this.debug = 0;
        this.display = 0;
        goals = new ArrayList<Goal>();
    }

    public void publishGridSize() {
        // size of the the complete grid
        JSONObject gridsize = new JSONObject();
        gridsize.put("rows", "" + grid.getRows());
        gridsize.put("columns", "" + grid.getColumns());
        clientMqtt.publish("inform/grid/init", gridsize.toJSONString());
    }

    public void publishObstacles() {
        JSONArray ja = new JSONArray();
        List<Situated> lo = grid.get(ComponentType.obstacle);
        for (Situated ob : lo) {
            JSONObject jo = new JSONObject();
            jo.put("x", ob.getX() + "");
            jo.put("y", ob.getY() + "");
            ja.add(jo);
        }
        JSONObject obst = new JSONObject();
        obst.put("obstacles", ja);
        clientMqtt.publish("inform/grid/obstacles", obst.toJSONString());
    }

    public void init(){
        boolean full = false;
        int x=-1, y = -1;
        int l_x=-1,l_y=-1;
        int i = 0, j=0;
        int len_mur=0;
        int [][] matrice_direction= {{0,1},{1,0},{0,-1},{-1,0}};
        int continue_wall_direction=(int)Math.ceil(Math.min(rows/10,columns/10));
        Random rnd = new Random(seed);
        int vs=rnd.nextInt(4);
        ArrayList<int[]> mur = new ArrayList<int[]>();
        System.out.println("initialisation obstacle");
        while((full == false)&(i<nbObstacles)) {
            int[] pos = grid.locate();
            x=pos[0];y=pos[1];
            boolean wall_continue=true;
            i++;
            len_mur=0;
            l_x=-1;l_y=-1;
            while(wall_continue) {
                //i++;
                System.out.println(i);
                System.out.println(x);
                System.out.println(y);
                System.out.println("____");
                for(int k=-1;k<2;k++) {
                    for(int l=-1;l<2;l++) {
                        if((x+k>=0 & y+l>=0)&(x+k<columns & y+l<rows)) {
                            if((grid.getCell(y+l,x+k).getComponentType()==ComponentType.obstacle)&(y+l!=l_y & x+k!=l_x)) {
                                wall_continue=false;
                            }
                        }
                        else {
                            if ((x<0 & y<0)||(x>=columns & y>=rows)) {
                                wall_continue=false;
                            }
                        }
                    }
                }
                System.out.println(wall_continue);
                System.out.println("____");
                if(wall_continue) {
                    int[] position= {x,y};
                    mur.add(position);
                    Obstacle obs = new Obstacle(position);
                    //i++;
                    grid.putSituatedComponent(obs);
                    j=rnd.nextInt(4+continue_wall_direction);
                    if(j>=4) {
                        j=vs;
                    }
                    l_x=x;l_y=y;
                    x+=matrice_direction[j][0];
                    y+=matrice_direction[j][1];
                    len_mur++;
                    vs=j;
                }
            }
            if(len_mur < 3){
                for(int k=0;k < len_mur;k++) {
                    grid.removeSituatedComponent(mur.get(k)[0],mur.get(k)[1]);
                }
            }
            mur.clear();
        }
        if(display == 1) {
            createColorGrid(displaywidth, displayheight, displaytitle);
        }
    }

    public String getName() {
        return name;
    }

    public void setMessage(Message mqtt) {
        clientMqtt = mqtt;
    }

    public void refresh() {
        cg.refresh();
    }

    public boolean moveRobot(int id, int x1, int y1, int x2, int y2) {
        Situated elt = grid.getCell(y1, x1);
        if (elt.getComponentType() == ComponentType.robot) {
            RobotDescriptor eltR = (RobotDescriptor) elt;
            if (eltR.getId() == id) {
                grid.moveSituatedComponent(x1, y1, x2, y2);
                if (display == 1) {
                    cg.setBlockColor(x1, y1, colorother);
                    cg.setBlockColor(x2, y2, colorrobot);
                }
                return true;
            }
        }
        return false;
    }

    public void displayGrid() {
        grid.display();
    }


    public JSONObject goalsToJSONObject(ArrayList<Goal> goals){
        JSONObject jo = new JSONObject();
        JSONArray ja = new JSONArray();

        for(Goal goal:goals){
            JSONObject jGoal = new JSONObject();
            jGoal.put("x",goal.getX());
            jGoal.put("y",goal.getY());
            ja.add(jGoal);
        }
        jo.put("goals",ja);
        return jo;
    }


    @SuppressWarnings("unchecked")
    public JSONObject gridToJSONObject(int x, int y, int field) {

        // size of the the complete grid
        JSONObject jsongrid = new JSONObject();

        // obstacles definition

        JSONArray gt = new JSONArray();
        int xm = Math.max(x - field, 0);
        int xM = Math.min(x + field, grid.getColumns() - 1);
        int ym = Math.max(y - field, 0);
        int yM = Math.min(y + field, grid.getRows() - 1);

        //System.out.println("field " + field + " x " + x + " y " + y);
        //System.out.println("xm " + xm + " xM " + xM + " ym " + ym + " yM "+ yM);

        for (int i = xm; i <= xM; i++) {
            for (int j = ym; j <= yM; j++) {
                if (i != x || j != y) {
                    Situated s = grid.getCell(j, i);
                    //System.out.println("j " + j + " i " + i + " cell " + s);
                    JSONObject jo = new JSONObject();
                    jo.put("type", s.getComponentType() + "");
                    if (s.getComponentType() == ComponentType.robot) {
                        RobotDescriptor rd = (RobotDescriptor) s;
                        jo.put("name", rd.getName());
                        jo.put("id", rd.getId() + "");
                    }
                    jo.put("x", s.getX() + "");
                    jo.put("y", s.getY() + "");
                    gt.add(jo);
                }
            }
        }

        jsongrid.put("x", x);
        jsongrid.put("y", y);
        jsongrid.put("field", field);
        jsongrid.put("cells", gt);

        return jsongrid;
    }

    public void publishInitRobot() {
        List<Situated> ls = grid.get(ComponentType.robot);
        for (Situated s : ls) {
            RobotDescriptor rb = (RobotDescriptor) s;
            JSONObject jo = new JSONObject();
            jo.put("name", rb.getName());
            jo.put("id", rb.getId() + "");
            jo.put("x", rb.getX() + "");
            jo.put("y", rb.getY() + "");
            clientMqtt.publish(rb.getName() + "/position/init", jo.toJSONString());
        }
    }

    public void handleMessage(String topic, JSONObject content) {
        //System.out.println("Message:"+ content.toJSONString());
        if (topic.contains("robot/nextPosition")) {
            //System.out.println("UPDATE ROBOT");
            String rn = (String) content.get("name");
            int idr = Integer.parseInt((String) content.get("id"));
            int xr = Integer.parseInt((String) content.get("x"));
            int yr = Integer.parseInt((String) content.get("y"));
            int xor = Integer.parseInt((String) content.get("xo"));
            int yor = Integer.parseInt((String) content.get("yo"));
            //System.out.println("MOVE MOVE " + xor + " " + yor + " --> " + xr + " " + yr);
            grid.moveSituatedComponent(xor, yor, xr, yr);
            if (display == 1) {
                //cg.setBlockColor(xor, yor, colorother);
                if (isGoal(xor, yor) < 0) {
                    cg.setBlockColor(xor, yor, colorgoal);
                } else {
                    cg.setBlockColor(xor, yor, colorother);
                }
                cg.setBlockColor(xr, yr, colorrobot);
                cg.refresh();
            }
            if (debug == 1) {
                grid.display();
            }
        } else if (topic.contains("configuration/nbRobot")) {
            nbRobots = Integer.parseInt((String) content.get("nbRobot"));
            for (int i = 2; i < nbRobots + 2; i++) {
                int[] pos = grid.locate();
                grid.putSituatedComponent(new RobotDescriptor(pos, i, GridManagement.turtlebotName + i));
                if (display == 1) {
                    cg.setBlockColor(pos[0], pos[1], colorrobot);
                    cg.refresh();
                }
            }
            for (int i = 2; i < nbRobots + 2; i++) {
                int[] pos = grid.locate();
                // ec = (EmptyCell)grid.getCell(pos[1], pos[0]);
                goals.add(new Goal(pos[0], pos[1], -1 * i));
                if (display == 1) {
                    cg.setBlockColor(pos[0], pos[1], colorgoal);
                    cg.refresh();
                }
            }
            if (debug == 1) {
                grid.display(goals);
            }
        } else if (topic.contains("configuration/robot/grid")) {
            String nameR = (String) content.get("name");
            int fieldr = Integer.parseInt((String) content.get("field"));
            int xr = Integer.parseInt((String) content.get("x"));
            int yr = Integer.parseInt((String) content.get("y"));
            JSONObject jo = gridToJSONObject(xr, yr, fieldr);
            JSONObject joG = goalsToJSONObject(goals);
            clientMqtt.publish(nameR + "/grid/init", jo.toJSONString());
            clientMqtt.publish(nameR+"/goals/init", joG.toJSONString());
        } else if (topic.contains("robot/grid")) {
            String nameR = (String) content.get("name");
            int fieldr = Integer.parseInt((String) content.get("field"));
            int xr = Integer.parseInt((String) content.get("x"));
            int yr = Integer.parseInt((String) content.get("y"));
            JSONObject jo = gridToJSONObject(xr, yr, fieldr);
            clientMqtt.publish(nameR + "/grid/update", jo.toJSONString());
        } else if (topic.contains("robot/nextPosition")) {
            publishState(content);
        } else if (topic.contains("configuration/debug")) {
            debug = Integer.parseInt((String) content.get("debug"));
        } else if (topic.contains("configuration/display")) {
            display = Integer.parseInt((String) content.get("display"));
            if (display == 1) {
                clientMqtt.subscribe("display/width");
                clientMqtt.subscribe("display/height");
                clientMqtt.subscribe("display/title");
                clientMqtt.subscribe("display/robot");
                clientMqtt.subscribe("display/goal");
                clientMqtt.subscribe("display/obstacle");
                clientMqtt.subscribe("display/other");
                clientMqtt.subscribe("display/unknown");
            }
        } else if (topic.contains("configuration/seed")) {
            seed = Integer.parseInt((String) content.get("seed"));
        } else if (topic.contains("configuration/nbObstacle")) {
            nbObstacles = Integer.parseInt((String) content.get("nbObstacle"));
        } else if (topic.contains("environment/grid")) {
            rows = Integer.parseInt((String) content.get("rows"));
            columns = Integer.parseInt((String) content.get("columns"));
            grid = new Grid(rows, columns, seed);
            grid.initEmpty();
            init();
        }
        /*else if(topic.contains("burger_5/position")) {
        	int x1 = Integer.parseInt((String)content.get("x1"));
        	int y1 = Integer.parseInt((String)content.get("y1"));
        	int x2 = Integer.parseInt((String)content.get("x2"));
        	int y2 = Integer.parseInt((String)content.get("y2"));
            moveRobot(5,x1,y1,x2,y2);
            if(display == 1)
				refresh();
        } */
        else if (display == 1) {
            if (topic.contains("display/width")) {
                displaywidth = Integer.parseInt((String) content.get("displaywidth"));
            } else if (topic.contains("display/height")) {
                displayheight = Integer.parseInt((String) content.get("displayheight"));
            } else if (topic.contains("display/title")) {
                displaytitle = (String) content.get("displaytitle");
            } else if (topic.contains("display/robot")) {
                colorrobot = new Color(Integer.parseInt((String) content.get("color")));
            } else if (topic.contains("display/goal")) {
                colorgoal = new Color(Integer.parseInt((String) content.get("color")));
            } else if (topic.contains("display/obstacle")) {
                colorobstacle = new Color(Integer.parseInt((String) content.get("color")));
            } else if (topic.contains("display/other")) {
                colorother = new Color(Integer.parseInt((String) content.get("color")));
            }
        }
    }
}
