package evergoodteam.chassis.objects.resourcepacks;

import com.google.gson.JsonObject;
import evergoodteam.chassis.configs.ConfigBase;
import evergoodteam.chassis.configs.ConfigHandler;
import evergoodteam.chassis.objects.assets.BlockstateJson;
import evergoodteam.chassis.objects.assets.LootJson;
import evergoodteam.chassis.objects.assets.ModelJson;
import evergoodteam.chassis.objects.assets.TagJson;
import evergoodteam.chassis.util.StringUtils;
import evergoodteam.chassis.util.handlers.DirHandler;
import evergoodteam.chassis.util.handlers.FileHandler;
import evergoodteam.chassis.util.handlers.JsonHandler;
import lombok.extern.log4j.Log4j2;
import net.fabricmc.loader.impl.util.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static evergoodteam.chassis.util.Reference.LOGGER;

@Log4j2
public class ResourcePackBase {

    public static final Map<String, List<ResourcePackBase>> RESOURCE_PACKS = new HashMap<>(); // Used for identifying RPs

    public static final List<String> HIDDEN = new ArrayList<>(); // Requires capitalized namespaces
    public static final List<String> NO_ICON = new ArrayList<>();

    public String namespace;
    public Path path;

    private Path assetsDir;
    private Path dataDir;
    private Path namespaceAssetsDir;
    private Path namespaceDataDir;
    private Path blockstatesDir;
    private Path tagsDir;

    /**
     * Object from which a ResourcePack will be generated
     *
     * @param config    Config to which assign the ResourcePack, determines the Root Dir
     * @param namespace name of your ResourcePack
     * @param iconUrl   valid URL, can be null
     */
    public ResourcePackBase(@NotNull ConfigBase config, @NotNull String namespace, String iconUrl) {

        if (!RESOURCE_PACKS.containsKey(config.namespace)) {
            RESOURCE_PACKS.put(config.namespace, new ArrayList<>());
        }

        this.namespace = namespace;
        this.path = Paths.get(config.dirPath.toString(), "resourcepacks/" + namespace.toLowerCase()); // Root of every ResourcePack
        //log.info(this.path);

        RESOURCE_PACKS.get(config.namespace).add(this);
        //log.info(RESOURCE_PACKS);

        config.resourcesLocked.put(namespace + "ResourceLocked", false); // TODO: Possible duplicates overriding

        config.readProperties();
        //log.info(ConfigHandler.getBooleanOption(config, "chassisResourceLocked", false));

        if (!ConfigHandler.getBooleanOption(config, namespace + "ResourceLocked", false)) {
            //log.info("Attempting to generate Resources");
            config.cleanResources(this);
            createRoot();

            //log.info("Are resources locked? {}", config.resourcesLocked);
            config.resourcesLocked.put(namespace + "ResourceLocked", true);
            config.setupDefaultProperties();

            LOGGER.info("Generated Resources for \"{}\"", this.namespace);
        } else LOGGER.info("Resources for \"{}\" already exist, skipping generation", this.namespace);

        if (iconUrl == null) NO_ICON.add(namespace);
        else createPackIcon(config, namespace, iconUrl);

        this.assignDirs();
    }

    /**
     * Object from which a ResourcePack will be generated without an icon
     *
     * @param config    Config to which assign the ResourcePack, determines the Root Dir
     * @param namespace name of your ResourcePack
     */
    public ResourcePackBase(ConfigBase config, String namespace) {
        new ResourcePackBase(config, namespace, null);
    }

    /**
     * Gets the ResourcePack from the given Id, which corresponds to the Root Dir and Config Name to which the ResourcePack is assigned
     *
     * @param id   name of the Configs to which the ResourcePack is assigned
     * @param name name of the ResourcePack
     * @return ResourcePack from the Config {@code id} and named {@code name}
     */
    public static ResourcePackBase getResourcePack(String id, String name) {

        List<ResourcePackBase> baseList = ResourcePackBase.RESOURCE_PACKS.get(id);

        int index = 0;

        for (ResourcePackBase pack : baseList) {
            if (name.toLowerCase().equals(pack.namespace)) {
                index = baseList.indexOf(pack);
            }
        }

        return RESOURCE_PACKS.get(id).get(index);
    }


    private ResourcePackBase assignDirs() {

        this.assetsDir = this.path.resolve("resources/assets");
        this.dataDir = this.path.resolve("resources/data");

        this.namespaceAssetsDir = this.path.resolve("resources/assets/" + this.namespace);
        this.namespaceDataDir = this.path.resolve("resources/data/" + this.namespace);
        this.blockstatesDir = this.namespaceAssetsDir.resolve("blockstates");
        this.tagsDir = this.namespaceDataDir.resolve("tags");

        return this;
    }

    public ResourcePackBase createRoot() {

        DirHandler.createDir(this.path.resolve("resources"), new String[]{"assets", "data"}); // Without assets and data folder getPath from Builder dies
        return this;
    }

    public ResourcePackBase createBlockstate(String path) {

        DirHandler.createDir(this.blockstatesDir);

        createJsonFile(this.blockstatesDir.resolve(path))
                .writeJson(BlockstateJson.createBlockstateJson(this.namespace, path), this.blockstatesDir.resolve(path));

        return this;
    }

    // TODO: Fallback for null
    public ResourcePackBase createBlockModels(String path, String texture, String cubeType) {

        DirHandler.createDir(this.namespaceAssetsDir.resolve("models"), new String[]{"block", "item"});

        Path block = Paths.get(this.namespaceAssetsDir.toString(), "models/block");
        Path item = Paths.get(this.namespaceAssetsDir.toString(), "models/item");

        createJsonFile(block.resolve(path))
                .writeJson(ModelJson.createBlockModelJson(cubeType, this.namespace + ":block/" + texture), block.resolve(path));

        createJsonFile(item.resolve(path))
                .writeJson(ModelJson.createItemModelJson(this.namespace, "block", path), item.resolve(path));

        return this;
    }

    public ResourcePackBase createBlockDropLootTable(String path) {

        Path lootTables = Paths.get(this.namespaceDataDir.toString(), "loot_tables/blocks");

        DirHandler.createDir(lootTables);

        createJsonFile(lootTables.resolve(path))
                .writeJson(LootJson.createBlockBreakLootJson(this.namespace, path), lootTables.resolve(path));

        return this;
    }

    public ResourcePackBase createGlobalTag(String input) {

        Path commonTagsDir = this.dataDir.resolve("c/tags");
        DirHandler.createDir(commonTagsDir, new String[]{"blocks", "items"});

        Path blocks = Paths.get(commonTagsDir.toString(), "blocks");
        Path items = Paths.get(commonTagsDir.toString(), "items");

        createJsonFile(blocks.resolve(input))
                .writeJson(TagJson.createTagJson(this.namespace, new String[]{input}), blocks.resolve(input));

        createJsonFile(items.resolve(input))
                .writeJson(TagJson.createTagJson(this.namespace, new String[]{input}), items.resolve(input));

        return this;
    }

    public ResourcePackBase createRequiredToolTag(String tool, String[] inputs) {

        Path mineable = Paths.get(this.dataDir.toString(), "minecraft/tags/blocks/mineable"); // Has to be inside of Minecraft folder

        DirHandler.createDir(mineable);

        if (!Files.exists(mineable.resolve(tool)))
            createJsonFile(mineable.resolve(tool)); // Create once, write big Json

        writeJson(TagJson.createTagJson(this.namespace, inputs), mineable.resolve(tool));

        return this;
    }

    /**
     * @param miningLevel stone, iron, diamond, etc.
     * @param inputs      Block name
     * @return
     */
    public ResourcePackBase createMiningLevelTag(String miningLevel, String[] inputs) {

        String fileName = "needs_" + miningLevel + "_tool";

        Path tagBlocks = Paths.get(this.dataDir.toString(), "minecraft/tags/blocks");

        DirHandler.createDir(tagBlocks);

        if (!Files.exists(tagBlocks.resolve(fileName))) createJsonFile(tagBlocks.resolve(fileName));

        writeJson(TagJson.createTagJson(this.namespace, inputs), tagBlocks.resolve(fileName));

        return this;
    }

    /**
     * Add a Texture using a valid URL
     *
     * @param block       True to specify it's a Block Texture
     * @param textureURL  Direct Link to your .png Image <br> (eg. https://i.imgur.com/BAStRdD.png)
     * @param textureName Name of the Texture File
     * @return
     */
    public ResourcePackBase createTexture(Boolean block, String textureURL, String textureName) {

        String actual = StringUtils.checkMissingExtension(textureName, ".png");

        DirHandler.createDir(this.namespaceAssetsDir.resolve("textures"), new String[]{"block", "item"});

        Path blockDir = this.namespaceAssetsDir.resolve("textures/block");
        Path itemDir = this.namespaceAssetsDir.resolve("textures/item");

        try (InputStream in = new URL(textureURL).openStream()) {
            if (block) {
                if (!Files.exists(blockDir.resolve(actual))) Files.copy(in, blockDir.resolve(actual));
            } else {
                if (!Files.exists(itemDir.resolve(actual))) Files.copy(in, itemDir.resolve(actual));
            }

        } catch (IOException e) {
            log.warn("Error on creating Texture .png", (e));
        }

        return this;
    }


    public ResourcePackBase writeJson(JsonObject jsonObject, Path path) {

        JsonHandler.writeToJson(jsonObject, path);
        return this;
    }

    public ResourcePackBase writeJson(String json, Path path) {

        JsonHandler.writeToJson(json, path);
        return this;
    }

    public ResourcePackBase createJsonFile(Path path) {

        FileHandler.createJsonFile(path);
        return this;
    }


    /**
     * Hide ResourcePack from the GUI
     *
     * @return
     */
    public ResourcePackBase hide() {
        HIDDEN.add(StringUtil.capitalize(this.namespace));
        return this;
    }

    public void createPackIcon(@NotNull ConfigBase config, @NotNull String namespace, String iconURL) {

        Path iconPath = Paths.get(config.dirPath.toString(), "resourcepacks/" + namespace.toLowerCase() + "/resources/pack.png");

        try (InputStream in = new URL(iconURL).openStream()) {
            if (!Files.exists(iconPath)) Files.copy(in, iconPath);
        } catch (IOException e) {
            log.warn("Error on creating Pack Icon file, falling back to Unknown Icon", (e));
            this.hide();
        }
    }
}
