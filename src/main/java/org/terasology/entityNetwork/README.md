#Entity Network
This module has components that let you join entities into a network of connections.  Built in connection types are:
1. Blocks that are positioned within a specified grid distance
2. Blocks that have a face that is positioned a specified grid distance away

Networks are uniquely distinguished from each other by network Id (just a unique string identifier).  If you want to join up to another module's network,  just use the same network Id. 

Different types of connections can be added by another module by extending ```NetworkNodeBuilder``` on your component.

Leaf nodes do not pass connectivity through themselves, kind of like a dead end for all incoming connections.

Allow for connectivity on more than one network by subclassing a built in component.

##Creating a Block Location Network
Add the ```BlockLocationNetworkNodeComponent``` to your entity to add an block entity (a placed block will have the required ```BlockComponent```) based on its distance from other nodes.  Once added and the conditions are met, this entity will be added to the in memory network that can be queried with ```EntityNetworkManager```.

###Minimal usage on a prefab
```
{
    "EntityNetwork": {},
    "BlockLocationNetworkNode": {
        "networkId": "SomeUniqueNameOfThisNetwork"
    }
}
```

###Defaults
```
{
    "EntityNetwork": {},
    "BlockLocationNetworkNode": {
        "networkId": <Null>,
        "isLeaf": false,
        "maximumGridDistance": 1
    }
}
```


##Creating a Sided Block Location Network
Add the ```SidedBlockLocationNetworkNodeComponent``` to your entity to add an block entity (a placed block will have the required ```BlockComponent```) based on its distance from the sides specified.  Once added and the conditions are met, this entity will be added to the in memory network that can be queried with ```EntityNetworkManager```.

The directions are one of ```org.terasology.math.Direction``` and will rotate themselves with the block's rotation if it's BlockFamily is a ```SideDefinedBlockFamily```.

###Minimal usage on a prefab
```
{
    "EntityNetwork": {},
    "SidedBlockLocationNetworkNode": {
        "networkId": "SomeUniqueNameOfThisNetwork",
        "directions": [
            "FORWARD",
            "BACKWARD"
        ]
    }
}
```

###Defaults
```
{
    "EntityNetwork": {},
    "SidedBlockLocationNetworkNode": {
        "networkId": <Null>,
        "directions": [],
        "isLeaf": false
    }
}
```
