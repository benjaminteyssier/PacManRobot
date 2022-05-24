package burger;

import components.Turtlebot;
import model.*;
import mqtt.Message;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.*;

import static java.lang.Math.abs;
import static java.lang.Math.max;

public class PacManBotV1 extends Turtlebot {

    protected Random rnd;
    protected Grid grid;

    protected ArrayList<Goal> goals;
    protected Goal goal;

    protected Stack<Orientation> path;

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
        clientMqtt.subscribe(name + "/goals/init");
        clientMqtt.subscribe("/goal/update");
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
        } else if (topic.contains(name + "/goals/init")) {
            JSONArray ja = (JSONArray) content.get("goals");
            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = (JSONObject) ja.get(i);
                int xG = ((Long) jo.get("x")).intValue();
                int yG = ((Long) jo.get("y")).intValue();
                goals.add(new Goal(xG, yG));
            }
            chooseGoal(grid, goals);
            if (goal != null)
                path = getPath(goal, x, y);
        } else if (topic.contains("/goal/update")) {
            JSONArray ja = (JSONArray) content.get("goals");
            goals.clear();
            for (int i = 0; i < ja.size(); i++) {
                JSONObject jo = (JSONObject) ja.get(i);
                int xG = ((Long) jo.get("x")).intValue();
                int yG = ((Long) jo.get("y")).intValue();
                if (xG != this.goal.getX() || yG != this.goal.getY()) {
                    goals.add(new Goal(xG, yG));
                }
            }
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

    public void chooseGoal(Grid grid, ArrayList<Goal> goals) {
        List<Situated> robots = grid.get(ComponentType.robot);
        List<List<Goal>> repartition = new ArrayList<List<Goal>>();

        if (goals.isEmpty())
            return;


        for (int i = 0; i < robots.size(); i++) {
            repartition.add(new ArrayList<>());
            if (i < goals.size())
                repartition.get(i).add(goals.get(i));
        }

        int repartitionLength = getRepartitionLength(robots, repartition);

        int compteurMax = goals.size() * robots.size();
        int compteur = 0;
        while (compteur < compteurMax) {
            for (int i = 0; i < robots.size(); i++) {
                for (int j = 0; j < repartition.get(i).size(); j++) {
                    for (int k = 0; k < robots.size(); k++) {
                        if (i != k) {
                            if (!repartition.get(i).isEmpty() && !repartition.isEmpty() && i < repartition.size() - 1 && j < repartition.get(i).size()) {
                                Goal temporaryGoal = repartition.get(i).get(j);
                                repartition.get(k).add(temporaryGoal);
                                repartition.get(i).remove(temporaryGoal);
                                if (getRepartitionLength(robots, repartition) < repartitionLength) {
                                    repartitionLength = getRepartitionLength(robots, repartition);
                                    compteur = 0;
                                } else {
                                    repartition.get(k).remove(temporaryGoal);
                                    repartition.get(i).add(temporaryGoal);
                                }
                            }
                        }
                    }
                }
            }
            compteur++;
        }
        for (int i = 0; i < robots.size(); i++) {
            Situated robot = robots.get(i);
            if (robot.getX() == this.getX() && robot.getY() == this.getY()) {
                if (!repartition.get(i).isEmpty()) {
                    this.goal = repartition.get(i).get(0);
                    goals.remove(this.goal);
                }
                JSONObject joG = goalsToJSONObject(goals);
                clientMqtt.publish("/goals/update", joG.toJSONString());
            }
        }

    }

    public JSONObject goalsToJSONObject(ArrayList<Goal> goals) {
        JSONObject jo = new JSONObject();
        JSONArray ja = new JSONArray();

        for (Goal goal : goals) {
            if (goal != this.goal) {
                JSONObject jGoal = new JSONObject();
                jGoal.put("x", goal.getX());
                jGoal.put("y", goal.getY());
                ja.add(jGoal);
            }
        }
        jo.put("goals", ja);
        return jo;
    }

    public int getRepartitionLength(List<Situated> robots, List<List<Goal>> repartition) {
        int repartitionLength = 0;
        for (int i = 0; i < robots.size(); i++) {
            int robotXPos = robots.get(i).getX();
            int robotYPos = robots.get(i).getY();
            for (int j = 0; j < repartition.get(i).size(); j++) {
                repartitionLength = max(repartitionLength,getPath(repartition.get(i).get(j), robotXPos, robotYPos).size());
                robotXPos = repartition.get(i).get(j).getX();
                robotYPos = repartition.get(i).get(j).getY();
            }
        }
        return repartitionLength;
    }

    public void setGoal(Goal goal) {
        this.goal = goal;
    }

    // algorithme de pathfinding par A* - la classe PointP défini une structure d'arbre (chaque point est un noeud ayant un unique antécédent)
    public Stack<Orientation> getPath(Goal goal, int x, int y) {
        //initialisation
        Stack<Orientation> path = new Stack<Orientation>();
        List<PointP> ouverts = new ArrayList<>();
        List<PointP> fermes = new ArrayList<>();
        //Point origine
        PointP ei = new PointP(x, y, null);
        ouverts.add(ei);
        //Initialisation du dernier point du chemin 
        PointP finDuPath = new PointP(0, 0, null);
        while (!ouverts.isEmpty()) {
            //choix du meilleur noeud à explorer - celui ayant une fonction f = g + h minimale
            int m = Integer.MAX_VALUE;
            int Im = 0;
            for (int i = 0; i < ouverts.size(); i++) {
                int fi = ouverts.get(i).g + h(ouverts.get(i).getX(), ouverts.get(i).getY(), goal);
                if (fi < m) {
                    m = ouverts.get(i).g;
                    Im = i;
                }
            }
            //Traitement du noeud p
            PointP p = ouverts.get(Im);
            ouverts.remove(Im);
            fermes.add(p);
            //verification de la condition d'arrêt (C = "le but est atteint")
            if (p.getX() == goal.getX() && p.getY() == goal.getY()) {
                finDuPath = p;
                break;
            }
            //Traitement de voisins du noeud p
            List<PointP> Voisins = getVoisins(p);
            for (int i = 0; i < Voisins.size(); i++) {
                PointP pi = Voisins.get(i);
                int opi = pi.isIn(ouverts);
                int fpi = pi.isIn(fermes);
                //Si le voisin pi est un nouveau noeud jamais atteint
                if (opi == -1 && fpi == -1) {
                    ouverts.add(pi);
                } // Sinon, si le voisin pi est déjà dans l'ensemble des ouverts et que le chemin pour l'atteindre est plus court
                else if (opi != -1) {
                    if (ouverts.get(opi).g > pi.g) {
                        ouverts.get(opi).modG(pi.g);
                        ouverts.get(opi).modPapa(p);
                    }
                } // Sinon, si le voisin pi est déjà dans l'ensemble des fermes et que le chemin pour l'atteindre est plus court
                else if (fermes.get(fpi).g > pi.g) {
                    fermes.get(fpi).modG(pi.g);
                    fermes.get(fpi).modPapa(p);
                    // Il faut alors recalculer la distance des successeurs de pi au point de départ
                    traitementSuccesseur(fermes.get(fpi), fermes);
                }
            }

        }
        // Transformation du chemin en liste d'orientation
        while (finDuPath.papa != null) {
            PointP papa = finDuPath.papa;
            int dX = papa.getX() - finDuPath.getX();
            int dY = papa.getY() - finDuPath.getY();

            if (dX == 1) {
                path.push(Orientation.down);
            } else if (dX == -1) {
                path.push(Orientation.up);
            } else if (dY == 1) {
                path.push(Orientation.left);
            } else {
                path.push(Orientation.right);
            }
            finDuPath = papa;
        }
        return path;
    }

    //fonction permettant d'actualiser la distance au point de départ des successeurs d'un noeud donné
    private void traitementSuccesseur(PointP p, List<PointP> fermes) {
        Queue<PointP> Ouverts = new LinkedList<PointP>();
        Ouverts.add(p);
        while (!Ouverts.isEmpty()) {
            PointP s = Ouverts.poll();
            for (PointP pi : fermes) {
                if (pi.papa == s) {
                    pi.modG(s.g + 1);
                    Ouverts.add(pi);
                }
            }
        }
    }

    //fonction donnant les possibles positions suivantes 
    //cette fonction permet au chemin d'eviter de revenir sur ses pas et d'aller dans un mur
    private List<PointP> getVoisins(PointP p) {
        ArrayList<PointP> voisins = new ArrayList<PointP>();
        int x = p.getX();
        int y = p.getY();

        //Pour le cas de départ, le point considéré n'a pas d'antécédent, on le traite séparément
        if (p.papa == null) {
            if (x - 1 >= 0) {
                ComponentType cellType = grid.getCell(y, x - 1).getComponentType();
                if ((cellType == ComponentType.empty || cellType == ComponentType.goal || cellType == ComponentType.unknown))
                    voisins.add(new PointP(x - 1, y, p));
            }
            if (y - 1 >= 0) {
                ComponentType cellType = grid.getCell(y - 1, x).getComponentType();
                if ((cellType == ComponentType.empty || cellType == ComponentType.goal || cellType == ComponentType.unknown))
                    voisins.add(new PointP(x, y - 1, p));
            }
            if (x + 1 < grid.getColumns()) {
                ComponentType cellType = grid.getCell(y, x + 1).getComponentType();
                if ((cellType == ComponentType.empty || cellType == ComponentType.goal || cellType == ComponentType.unknown))
                    voisins.add(new PointP(x + 1, y, p));
            }
            if (y + 1 < grid.getRows()) {
                ComponentType cellType = grid.getCell(y + 1, x).getComponentType();
                if ((cellType == ComponentType.empty || cellType == ComponentType.goal || cellType == ComponentType.unknown))
                    voisins.add(new PointP(x, y + 1, p));
            }
        } else {

            int xpp = p.papa.getX();
            int ypp = p.papa.getY();

            if ((x - 1) >= 0) {
                ComponentType cellType = grid.getCell(y, x - 1).getComponentType();
                if (x - 1 != xpp && (cellType == ComponentType.empty || cellType == ComponentType.goal || cellType == ComponentType.unknown))
                    voisins.add(new PointP(x - 1, y, p));
            }

            if ((x + 1) < grid.getColumns()) {
                ComponentType cellType = grid.getCell(y, x + 1).getComponentType();
                if (x + 1 != xpp && (cellType == ComponentType.empty || cellType == ComponentType.goal || cellType == ComponentType.unknown))
                    voisins.add(new PointP(x + 1, y, p));
            }
            if ((y + 1) < grid.getRows()) {
                ComponentType cellType = grid.getCell(y + 1, x).getComponentType();
                if (y + 1 != ypp && (cellType == ComponentType.empty || cellType == ComponentType.goal || cellType == ComponentType.unknown))
                    voisins.add(new PointP(x, y + 1, p));
            }
            if ((y - 1) >= 0) {
                ComponentType cellType = grid.getCell(y - 1, x).getComponentType();
                if (y - 1 != ypp && (cellType == ComponentType.empty || cellType == ComponentType.goal || cellType == ComponentType.unknown))
                    voisins.add(new PointP(x, y - 1, p));
            }
        }

        return voisins;
    }

    //distance quadratique à l'objectif
    private int h(int x, int y, Goal goal) {
        if (goal == null)
            return 0;
        return ((x - goal.getX()) ^ 2 + (y - goal.getY()) ^ 2);
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

    public JSONObject goalReachedToJSON(Goal goal){
        JSONObject jo = new JSONObject();
        jo.put("x",goal.getX());
        jo.put("y",goal.getY());
        return jo;
    }

    public void move(int step) {
        String actionr = "move_forward";
        String result = x + "," + y + "," + orientation + "," + grid.getCellsToString(y, x) + ",";
        Orientation nextStep = null;


        if (goals.isEmpty()) {
            return;
        }

        if (path == null) {
            chooseGoal(grid, goals);
            if(goal==null)
                return;
            path = getPath(goal, x, y);
            return;
        }

        for (int i = 0; i < step; i++) {
            String st = "[";
            EmptyCell[] ec = grid.getAdjacentEmptyCell(x, y);

            if (goal.getX() == x && goal.getY() == y) {
                JSONObject jo = goalReachedToJSON(goal);
                clientMqtt.publish("goal/reached", jo.toJSONString());
                if (!goals.isEmpty()) {
                    chooseGoal(grid, goals);
                    path = getPath(goal, x, y);
                }
                return;
            }
            if (!path.isEmpty()) {
                nextStep = path.pop();

                if (nextStep == Orientation.up) {
                    if (ec[3] != null) {
                        if (orientation == Orientation.up)
                            moveForward();
                        else if (orientation == Orientation.down) {
                            moveRight(1);
                            path.add(nextStep);
                            actionr = "turn_right";
                        } else if (orientation == Orientation.left) {
                            moveRight(1);
                            path.add(nextStep);

                            actionr = "turn_right";
                        } else {
                            moveLeft(1);
                            path.add(nextStep);

                            actionr = "turn_left";
                        }
                    } else {
                        path = getPath(goal, x, y);
                    }
                } else if (nextStep == Orientation.down) {
                    if (ec[2] != null) {
                        if (orientation == Orientation.up) {
                            moveRight(1);
                            path.add(nextStep);

                            actionr = "turn_right";
                        } else if (orientation == Orientation.down)
                            moveForward();
                        else if (orientation == Orientation.left) {
                            moveLeft(1);
                            path.add(nextStep);

                            actionr = "turn_left";
                        } else {
                            moveRight(1);
                            path.add(nextStep);
                            actionr = "turn_right";
                        }
                    } else {
                        path = getPath(goal, x, y);
                    }
                } else if (nextStep == Orientation.left) {
                    if (ec[0] != null) {
                        if (orientation == Orientation.up) {
                            moveLeft(1);
                            path.add(nextStep);
                            actionr = "turn_left";
                        } else if (orientation == Orientation.down) {
                            moveRight(1);
                            path.add(nextStep);
                            actionr = "turn_right";
                        } else if (orientation == Orientation.left)
                            moveForward();
                        else {
                            moveLeft(1);
                            path.add(nextStep);
                            actionr = "turn_left";
                        }
                    } else {
                        path = getPath(goal, x, y);
                    }
                } else if (nextStep == Orientation.right) {
                    if (ec[1] != null) {
                        if (orientation == Orientation.up) {
                            moveRight(1);
                            path.add(nextStep);
                            actionr = "turn_right";
                        } else if (orientation == Orientation.down) {
                            moveLeft(1);
                            path.add(nextStep);
                            actionr = "turn_left";
                        } else if (orientation == Orientation.left) {
                            moveRight(1);
                            path.add(nextStep);
                            actionr = "turn_right";
                        } else
                            moveForward();
                    } else {
                        path = getPath(goal, x, y);
                    }
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
            } else if (orientation == Orientation.left) {
                orientation = Orientation.down;
            } else if (orientation == Orientation.right) {
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
            } else if (orientation == Orientation.left) {
                orientation = Orientation.up;
            } else if (orientation == Orientation.right) {
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
            y = max(y, 0);
        } else if (orientation == Orientation.right) {
            y += 1;
            y = Math.min(y, grid.getRows() - 1);
        } else {
            x -= 1;
            x = max(x, 0);
        }
        JSONObject robotj = new JSONObject();
        robotj.put("name", name);
        robotj.put("id", "" + id);
        robotj.put("x", "" + x);
        robotj.put("y", "" + y);
        robotj.put("xo", "" + xo);
        robotj.put("yo", "" + yo);
        //System.out.println("MOVE MOVE " + xo + " " + yo + " --> " + x + " " + y);
        clientMqtt.publish("robot/nextPosition", robotj.toJSONString());
    }

}
