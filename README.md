# Bone Shard Helper
A plugin providing a set of helpful utilities for Prayer training in Varlamore using blessed bone shards. 

# How It Works
When you first open the plugin, you'll see a panel with two tabs: "Goal Mode" and "Resource Mode". 

Goal Mode is for calculating the resources you'll need to reach a specific Prayer level or experience milestone. Resource Mode is used for calculating the value of all materials currently held in your inventory.

Both of these modes have a "Refresh Current Stats" button at the top to read your current Prayer XP. In Goal Mode, this button also sets your Target Level to your next Prayer level. 

These values can be overwritten by the user, and restored by pressing the refresh button again.

# Features

### Goal Mode
- Calculates required resources to achieve a Prayer goal
- Accounts for multipliers on training speed, such as the use of Sunfire Wines and Zealot's robes
- Can scan your inventory and calculate the total bone shard value of any bones you're holding (including noted and blessed versions)
- Has a "Resource Planning" table which can tell you how many more bones you should get to reach your goal
- Debug setting: Allows you to look up a player by name to populate the starting XP, and automatically sets a reasonable target (current level + 1). 

### Resource Mode
- Can scan your inventory to detect the bone shard value of whatever Prayer resources you're holding
- Calculates the XP value of your inventory and the total number of wines needed for training
- Displays the final Prayer level you will have earned after using all the resources in your inventory
- Contains a "Bone Shard Sources" table showing the shard value of each different type of bone that can be used for training
- Debug setting: Allows you to enter a custom number of bone shards and will calculate the XP value and number of wines needed to process that number of shards.

### General Features
- Highlights the Exposed Altar, Shrine of Ralos, and Libation Bowl in Ralos' Rise. This feature can be configured in the plugin's settings.

# Planned Features
- If the player is in Ralos' Rise with bone shards and unblessed wines in their inventory, draw a line from the player's current position to the Exposed Altar to bless the wines. 
- Calculate number of trips to achieve goal / process the held bone shards
- Calculate number of Prayer potions or Moonlight moths needed, for players who choose not to run to the altar to restore prayer points
- Calculate estimated processing time for materials in the inventory (time spent breaking down bones)

### Under consideration
- Add option for user to include banked material as part of calculations
- Add support for an incomplete Zealot's robe set (i.e. 1-3 pieces, not just the full set)

# Screenshots
![bone-shard-helper-plugin](/assets/Bone%20Shard%20Plugin%20info_2.png "bone-shard-plugin-info")

