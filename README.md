# robot_simulator

## How to use 

+ compilation with maven
```
mvn compile 
```
+ compilation & execution with maven
```
mvn compile exec:java -Dexec.mainClass="main.TestAppli"
```
## Functional requirements

This project simulates a PacMan (in yellow) having 2 cells of sight and needing to reach a goal (in red). To reduce the size of the
 calculations the PacMan only calculates its path using the A* search algorithm at the beginning. Then, if ihe encouters an obstacle,
he will deviate from his path trying to catch up with it depending on its environment. This project uses MQTT communication between the different
components. It can also work with multiple agents communicating to split the goals. 