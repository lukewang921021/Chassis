package evergoodteam.chassis.common.resourcepack;

import com.google.common.base.Charsets;
import evergoodteam.chassis.util.StringUtils;
import net.fabricmc.fabric.impl.resource.loader.ModNioResourcePack;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.resource.AbstractFileResourcePack;
import net.minecraft.resource.InputSupplier;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.metadata.PackResourceMetadata;
import net.minecraft.resource.metadata.ResourceMetadataReader;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Pattern;

import static evergoodteam.chassis.util.Reference.CMI;
import static org.slf4j.LoggerFactory.getLogger;

public class FileResourcePack extends AbstractFileResourcePack {

    private static final Logger LOGGER = getLogger(CMI + "/R/File");
    private static final Pattern RESOURCEPACK_PATH = Pattern.compile("[a-z0-9-_]+");
    public static final List<FileResourcePack> BUILT = new ArrayList<>();

    private final String id;
    private final ResourceType resourceType;
    private final PackResourceMetadata packMetadata;
    private final Path basePath;
    private final String separator;
    private Set<String> namespaces;

    public FileResourcePack(String namespace, Text description, ResourceType resourceType, Path basePath) {
        super(namespace, true);
        this.id = namespace;
        this.resourceType = resourceType;
        this.packMetadata = new PackResourceMetadata(description, SharedConstants.getGameVersion().getResourceVersion(ResourceType.CLIENT_RESOURCES), Optional.empty());
        this.basePath = basePath.resolve(namespace).resolve("resources").toAbsolutePath().normalize();
        this.separator = basePath.getFileSystem().getSeparator();

        BUILT.add(this);
    }

    private Path getPath(String filename) {
        Path childPath = basePath.resolve(filename.replace("/", separator));
        if (childPath.startsWith(basePath) && Files.exists(childPath)) return childPath;
        else return null;
    }

    /**
     * From {@link ModNioResourcePack#findResources}
     */
    @Override
    public void findResources(ResourceType type, String namespace, String path, ResultConsumer visitor) {

        String separator = basePath.getFileSystem().getSeparator();
        Path nsPath = basePath.resolve(type.getDirectory()).resolve(namespace);
        Path searchPath = nsPath.resolve(path.replace("/", separator)).normalize();
        if (!Files.exists(searchPath)) return;

        try {
            Files.walkFileTree(searchPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    String filename = nsPath.relativize(file).toString().replace(separator, "/");
                    Identifier identifier = Identifier.of(namespace, filename);

                    if (identifier == null) {
                        LOGGER.error("Invalid path in mod resource-pack {}: {}:{}, ignoring", id, namespace, filename);
                    } else {
                        visitor.accept(identifier, InputSupplier.create(file));
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            LOGGER.error("findResources failed at path {} in namespace {}", path, namespace, e);
        }
    }

    /**
     * From {@link net.fabricmc.fabric.impl.resource.loader.FabricModResourcePack#openRoot(java.lang.String...)}
     */
    @Nullable
    @Override
    public InputSupplier<InputStream> openRoot(String... segments) {
        String fileName = String.join("/", segments);

        if ("pack.mcmeta".equals(fileName)) {
            String description = id + ".metadata.description";
            String fallback = "Resources generated by Chassis";
            String pack = String.format("{\"pack\":{\"pack_format\":" + SharedConstants.getGameVersion().getResourceVersion(resourceType) + ",\"description\":{\"translate\":\"%s\",\"fallback\":\"%s.\"}}}", description, fallback);
            return () -> IOUtils.toInputStream(pack, Charsets.UTF_8);
        } else if ("pack.png".equals(fileName)) {
            // Account for provider-generated icon
            Path generated = basePath.resolve("pack.png");
            if(generated.toFile().exists()) return InputSupplier.create(generated);

            // If null, uses "textures/misc/unknown_pack.png"
            return FabricLoader.getInstance().getModContainer(id)
                    .flatMap(container -> container.findPath("pack.png"))
                    .map(path -> (InputSupplier<InputStream>) (() -> Files.newInputStream(path)))
                    .orElse(null);
        }

        return null;
    }

    @Nullable
    @Override
    public InputSupplier<InputStream> open(ResourceType type, Identifier id) {
        final Path path = getPath(getFilename(type, id));
        return path == null ? null : InputSupplier.create(path);
    }

    private static String getFilename(ResourceType type, Identifier id) {
        return String.format(Locale.ROOT, "%s/%s/%s", type.getDirectory(), id.getNamespace(), id.getPath());
    }

    @Override
    public Set<String> getNamespaces(ResourceType type) {
        if (this.namespaces == null) {
            Path file = getPath(type.getDirectory());

            if (file == null) LOGGER.error("Invalid Path");

            if (!Files.isDirectory(file)) {
                return Collections.emptySet();
            }

            Set<String> namespaces = new HashSet<>();
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(file, Files::isDirectory)) {
                for (Path path : stream) {
                    String s = path.getFileName().toString();

                    s = s.replace(separator, "");

                    if (RESOURCEPACK_PATH.matcher(s).matches()) {
                        namespaces.add(s);
                    } else {
                        LOGGER.error("Invalid namespace format at {}", path);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Could not get namespaces", e);
            }

            this.namespaces = namespaces;
        }

        return this.namespaces;
    }

    @Nullable
    @Override
    public <T> T parseMetadata(ResourceMetadataReader<T> metaReader) throws IOException {
        if (metaReader.getKey().equals("pack")) {
            return (T) packMetadata;
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return StringUtils.capitalize(this.id);
    }

    @Override
    public void close() {
    }
}