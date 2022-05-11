package burger;

import components.Turtlebot;
import model.*;
import mqtt.Message;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Math.abs;

public class PacManBotV1 extends Turtlebot {

    protected Random rnd;
    protected Grid grid;

    protected ArrayList<Goal> goals;
    protected Goal goal;

    public PacManBotV1(int id, String name, int seed, int field, Message clientMqtt, int debug) {
        super(id, name, seed, field, clientMqtt, debug);
        rnd = new Random(seed);
        goals = new ArrayList<>();
    }

    protected void init() {
        clientMqtt.subscribe("inform/grid/init");
        clientMqtt.subscribe(name + "/position/init");
        clientMqtt.subscribe(name + "/grid/init");
        clientMqtt.subscribe(name + "/grid/update");
        clientMqtt.subscribe(name + "/action");
        clientMqtt.subscribe(name+"/goals/init");
    }

    public void handleMessage(String topic, JSONObject content) {
        if (topic.contains(name + "/grid/update")) {
            JSONArray ja = (JSONArray) content.get("cells");
            List<Situated> ls = grid.get(ComponentType.robot);
            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = (JSONObject) ja.get(i);
                String typeCell = (String) jo.get("type");
                int xo = Integer.parseInt((String) jo.get("x"));
                int yo = Integer.parseInt((String) jo.get("y"));
                int[] to = new int[]{xo, yo};
                if (typeCell.equals("robot")) {
                    int idr = Integer.parseInt((String) jo.get("id"));
                    boolean findr = false;
                    for (Situated sss : ls) {
                        if (sss != this) {
                            RobotDescriptor rd = (RobotDescriptor) sss;
                            if (rd.getId() == idr) {
                                grid.moveSituatedComponent(rd.getX(), rd.getY(), xo, yo);
                                findr = true;
                                break;
                            }
                        }
                    }
                    if (!findr) {
                        String namer = (String) jo.get("name");
                        grid.forceSituatedComponent(new RobotDescriptor(to, idr, namer));
                    }
                } else {
                    Situated sg = grid.getCell(yo, xo);
                    Situated s;
                    if (sg.getComponentType() == ComponentType.unknown) {
                        if (typeCell.equals("obstacle")) {
                            //System.out.println("Add ObstacleCell");
                            s = new ObstacleDescriptor(to);
                        } else {
                            //System.out.println("Add EmptyCell " + xo + ", " + yo);
                            s = new EmptyCell(xo, yo);
                        }
                        grid.forceSituatedComponent(s);
                    } else if (typeCell.equals("goal")) {
                        System.out.println("test122432432543253");
                        s = new Goal(xo, yo);
                    }
                }
            }
            if (debug == 1) {
                System.out.println("---- " + name + " ----");
                grid.display();
            }
        } else if (topic.contains(name + "/action")) {
            int stepr = Integer.parseInt((String) content.get("step"));
            move(stepr);
        } else if (topic.contains("inform/grid/init")) {
            int rows = Integer.parseInt((String) content.get("rows"));
            int columns = Integer.parseInt((String) content.get("columns"));
            grid = new Grid(rows, columns, seed);
            grid.initUnknown();
            grid.forceSituatedComponent(this);
        } else if (topic.contains(name + "/position/init")) {
            x = Integer.parseInt((String) content.get("x"));
            y = Integer.parseInt((String) content.get("y"));
        } else if (topic.contains(name + "/grid/init")) {
            JSONArray ja = (JSONArray) content.get("cells");
            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = (JSONObject) ja.get(i);
                String typeCell = (String) jo.get("type");
                int xo = Integer.parseInt((String) jo.get("x"));
                int yo = Integer.parseInt((String) jo.get("y"));
                int[] to = new int[]{xo, yo};
                Situated s;
                if (typeCell.equals("obstacle")) {
                    //System.out.println("Add ObstacleCell");
                    s = new ObstacleDescriptor(to);
                } else if (typeCell.equals("robot")) {
                    //System.out.println("Add RobotCell");
                    int idr = Integer.parseInt((String) jo.get("id"));
                    String namer = (String) jo.get("name");
                    s = new RobotDescriptor(to, idr, namer);
                } else {
                    //System.out.println("Add EmptyCell " + xo + ", " + yo);
                    s = new EmptyCell(xo, yo);
                }
                grid.forceSituatedComponent(s);
            }
        } else if (topic.contains(name+"/goals/init")) {
            JSONArray ja = (JSONArray) content.get("goals");
            for(int i=0; i<ja.size();i++){
                JSONObject jo = (JSONObject) ja.get(i);
                int xG = ((Long) jo.get("x")).intValue();
                int yG = ((Long) jo.get("y")).intValue();
                goals.add(new Goal(xG,yG));
            }
            System.out.println(goals);
        }
    }

    public void setLocation(int x, int y) {
        int xo = this.x;
        int yo = this.y;
        this.x = x;
        this.y = y;
    }

    public Grid getGrid() {
        return grid;
    }

    public void setGrid(Grid grid) {
        this.grid = grid;
    }

    public List<Orientation> getPath(Grid grid, Goal goal, int x, int y) {
        List<Orientation> path = new ArrayList<>();
        while (!(goal.getX() == x && goal.getY() == y)) {
            if (goal.getY() - y > 0) {
                if (goal.getY() - y > abs((goal.getX() - x))) {
                    path.add(Orientation.left);
                    y++;
                } else {
                    if (goal.getX() - x > 0) {
                        path.add(Orientation.up);
                        x++;
                    } else {
                        path.add(Orientation.down);
                        x--;
                    }
                }
            } else if (goal.getY() - y < 0) {
                if (abs(goal.getY() - y) > abs((goal.getX() - x))) {
                    path.add(Orientation.left);
                    y--;

                } else {
                    if (goal.getX() - x > 0) {
                        path.add(Orientation.up);
                        x++;

                    } else {
                        path.add(Orientation.down);
                        x--;
                    }
                }
            } else {
                if (goal.getX() - x > 0) {
                    path.add(Orientation.up);
                    x++;

                } else {
                    path.add(Orientation.down);
                    x--;
                }
            }
        }
        return path;
    }

    public void randomOrientation() {
        double d = Math.random();
        if (d < 0.25) {
            if (orientation != Orientation.up)
                orientation = Orientation.up;
            else
                orientation = Orientation.down;
        } else if (d < 0.5) {
            if (orientation != Orientation.down)
                orientation = Orientation.down;
            else
                orientation = Orientation.up;
        } else if (d < 0.75) {
            if (orientation != Orientation.left)
                orientation = Orientation.left;
            else
                orientation = Orientation.right;
        } else {
            if (orientation != Orientation.right)
                orientation = Orientation.right;
            else
                orientation = Orientation.left;
        }
    }

    public void move(int step) {
        System.out.println(grid.get(ComponentType.goal));

        String actionr = "move_forward";
        String result = x + "," + y + "," + orientation + "," + grid.getCellsToString(y, x) + ",";
        goal = new Goal(0, 0);
        for (int i = 0; i < step; i++) {
            String st = "[";
            EmptyCell[] ec = grid.getAdjacentEmptyCell(x, y);

            List<Orientation> path = getPath(grid, goal, x, y);

            if (goal.getX() == x && goal.getY() == y)
                return;

            if (path.get(0) == Orientation.up) {
                if (ec[3] != null) {
                    if (orientation == Orientation.up)
                        moveForward();
                    else if (orientation == Orientation.down) {
                        moveRight(1);
                        actionr = "turn_right";
                    } else if (orientation == Orientation.left) {
                        moveRight(1);
                        actionr = "turn_right";
                    } else {
                        moveLeft(1);
                        actionr = "turn_left";
                    }
                } else {
                    path = getPath(grid, goal, x, y);
                }
            } else if (path.get(0) == Orientation.down) {
                if (ec[2] != null) {
                    if (orientation == Orientation.up) {
                        moveRight(1);
                        actionr = "turn_right";
                    } else if (orientation == Orientation.down)
                        moveForward();
                    else if (orientation == Orientation.left) {
                        moveLeft(1);
                        actionr = "turn_left";
                    } else {
                        moveRight(1);
                        actionr = "turn_right";
                    }
                } else {
                    path = getPath(grid, goal, x, y);
                }
            } else if (path.get(0) == Orientation.left) {
                if (ec[0] != null) {
                    if (orientation == Orientation.up) {
                        moveLeft(1);
                        actionr = "turn_left";
                    } else if (orientation == Orientation.down) {
                        moveRight(1);
                        actionr = "turn_right";
                    } else if (orientation == Orientation.left)
                        moveForward();
                    else {
                        moveLeft(1);
                        actionr = "turn_left";
                    }
                } else {
                    path = getPath(grid, goal, x, y);
                }
            } else if (path.get(0) == Orientation.right) {
                if (ec[1] != null) {
                    if (orientation == Orientation.up) {
                        moveRight(1);
                        actionr = "turn_right";
                    } else if (orientation == Orientation.down) {
                        moveLeft(1);
                        actionr = "turn_left";
                    } else if (orientation == Orientation.left) {
                        moveRight(1);
                        actionr = "turn_right";
                    } else
                        moveForward();
                } else {
                    path = getPath(grid, goal, x, y);
                }
            }

        }
        if (debug == 2) {
            try {
                writer.write(result + actionr);
                writer.newLine();
                writer.flush();
            } catch (IOException ioe) {
                System.out.println(ioe);
            }
        }
    }

    public void moveLeft(int step) {
        Orientation oldo = orientation;
        for (int i = 0; i < step; i++) {
            if (orientation == Orientation.up) {
                orientation = Orientation.left;
            }
            if (orientation == Orientation.left) {
                orientation = Orientation.down;
            }
            if (orientation == Orientation.right) {
                orientation = Orientation.up;
            } else {
                orientation = Orientation.right;
            }
        }
    }

    public void moveRight(int step) {
        Orientation oldo = orientation;
        for (int i = 0; i < step; i++) {
            if (orientation == Orientation.up) {
                orientation = Orientation.right;
            }
            if (orientation == Orientation.left) {
                orientation = Orientation.up;
            }
            if (orientation == Orientation.right) {
                orientation = Orientation.down;
            } else {
                orientation = Orientation.left;
            }
        }
    }

    public void moveForward() {
        int xo = x;
        int yo = y;
        if (orientation == Orientation.up) {
            x += 1;
            x = Math.min(x, grid.getColumns() - 1);
        } else if (orientation == Orientation.left) {
            y -= 1;
            y = Math.max(y, 0);
        } else if (orientation == Orientation.right) {
            y += 1;
            y = Math.min(y, grid.getRows() - 1);
        } else {
            x -= 1;
            x = Math.max(x, 0);
        }
        JSONObject robotj = new JSONObject();
        robotj.put("name", name);
        robotj.put("id", "" + id);
        robotj.put("x", "" + x);
        robotj.put("y", "" + y);
        robotj.put("xo", "" + xo);
        robotj.put("yo", "" + yo);
        System.out.println("MOVE MOVE " + xo + " " + yo + " --> " + x + " " + y);
        clientMqtt.publish("robot/nextPosition", robotj.toJSONString());
    }

}
