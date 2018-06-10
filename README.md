# subway-sim
### CMPT440 Lab 2 — Group Project
### Sasha S and Erik H

## Building
To create a runnable JAR file, run:

On Windows: `gradlew.bat jar`

On \*nix: `./gradlew jar`

This will create the file `./build/libs/CMPT440Lab2SubwaySim.jar`.

## Running
The JAR file can be used:
`java -jar build/libs/CMPT440Lab2SubwaySim.jar <basename here>`

Alternatively, the Python run script can be used:
`python3 run.py <basename here>`

### Configuration File Parameters
```
station:<Station name> <next Station name in loop> <time to next Station in seconds> <average time between Passenger spawns>

train:<Train name> <starting Station>

passengerSpawnSeed <seed> # for Passenger's spawning at Stations

stationPickerSeed <seed> # for destination Stations of Passenger

trainWaitTime <time Trains will wait at Stations, in seconds>

boardTime <time it takes one Passenger to board a Train, in seconds>

timeStep <should be equal to boardTime (is automatically set)>

trainCapacity <max Passenger capacity of all Trains>
```
