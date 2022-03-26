package evergoodteam.chassis.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;;

import static evergoodteam.chassis.util.Reference.*;

@Environment(EnvType.CLIENT)
public class ChassisClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {

        LOGGER.info("Found " + BLOCKS.size() + " blocks to add, of which " + COLUMNS.size() + " types are columns");
        LOGGER.info("Found " + RECIPES.size() + " recipes to add");
        LOGGER.info("Found " + LOOT.size() + " loot tables to add");

    }
}
