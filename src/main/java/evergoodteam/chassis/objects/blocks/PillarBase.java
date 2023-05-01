package evergoodteam.chassis.objects.blocks;

import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.block.PillarBlock;
import net.minecraft.sound.BlockSoundGroup;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PillarBase extends PillarBlock implements BlockSettings {

    public PillarBase(Material material, Float hardness, Float resistance, BlockSoundGroup sound) {
        this(FabricBlockSettings.of(material).requiresTool().strength(hardness, resistance).sounds(sound));
    }

    public PillarBase(Material material, Float strength, BlockSoundGroup sound) {
        this(FabricBlockSettings.of(material).requiresTool().strength(strength).sounds(sound));
    }

    public PillarBase(Settings settings) {
        super(settings);
    }

    public PillarBase addTo(@NotNull List<Block> list) {
        list.add(this);
        return this;
    }
}
