package burger;

import java.util.List;

public class PointP{
    int[] value;
    int g = 0;
    PointP papa;
    PointP(int[] v){
        value = new int[]{v[0], v[1]};
    }
    
    PointP(int x, int y, PointP papa){
        value = new int[]{x, y};
        this.papa = papa;
        if(papa == null) {
        	this.g = 0;
        }else {
        	this.g = papa.g+1;
        }
        
    }
    
    public void modG(int g) {
        this.g = g;
    }
    
    public void modPapa(PointP papa) {
        this.papa = papa;
    }
    
    int getX() {
    	return value[0];
    }
    
    int getY() {
    	return value[1];
    }
    
    //fonction permettant de vérifier l'appartenance à une liste de PointsP
    int isIn(List<PointP> L) {
        int x = this.value[0];
        int y = this.value[1];
        boolean isIn = false;
        for(int i = 0; i < L.size(); i++) {
            PointP p = L.get(i);
            if (x == p.value[0] && y == p.value[1]) {
                return i;
            }
        }
        return -1;
    }
    
    @Override
    public boolean equals(Object o) {
        return ( (((Point)o).value[0] == value[0]) && (((Point)o).value[1] == value[1]) ) ;
    }
}
