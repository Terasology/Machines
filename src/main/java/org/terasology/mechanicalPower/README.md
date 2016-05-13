#Mechanical Power
This is a potential energy distribution system using the ```PotentialEnergyDevices``` module that evenly distributes energy to all connected entities without regard for available capacity.  This intends to make it a strategic choice how many entities you connect to one single power source.

There is built in functionality to accommodate rotating axles on the client side to provide visual indicators of power distribution.

##Connecting to the Mechanical Power Entity Network
Use the network id defined on ```MechanicalPowerAuthoritySystem.NETWORK_ID``` to connect to the mechanical power entity network.

Alternatively you can use the ```MechanicalPowerBlockNetworkComponent``` (which is just like the default ```BlockLocationNetworkNodeComponent```) or ```MechanicalPowerSidedBlockNetworkComponent``` (which is just like the default ```SidedBlockLocationNetworkNodeComponent``` which have the network Id baked in and composable with other networks.

###Minimal usage on a prefab
```
{
    "EntityNetwork": {},
    "MechanicalPowerBlockNetwork": {}
}
```

or

```
{
    "EntityNetwork": {},
    "MechanicalPowerSidedBlockNetwork": {
        "directions": [
            "FORWARD",
            "BACKWARD"
        ]
    }
}
```
