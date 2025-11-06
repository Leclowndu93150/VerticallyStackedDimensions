package kmerrill285.stackeddimensions.networking;

import java.util.function.Supplier;

import kmerrill285.stackeddimensions.Util;
import kmerrill285.stackeddimensions.render.ChunkEncoder;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.fml.network.NetworkEvent;


public class SPacketSendChunk {
	
	public Chunk chunk;
	public int x, z;
	public PacketBuffer buf;
	public SPacketSendChunk(Chunk chunk, int x, int z) {
		this.chunk = chunk;
		this.x = x;
		this.z = z;
	}
	
	public void encode(PacketBuffer buf) {
		buf.writeInt(x);
		buf.writeInt(z);
		ChunkEncoder.encodeChunk(chunk, buf);
    }
	
	public SPacketSendChunk(PacketBuffer buf) {
		x = buf.readInt();
		z = buf.readInt();
		
		this.buf = buf;
	}
	
	public void handle(Supplier<NetworkEvent.Context> ctx) {
		
		
		ctx.get().enqueueWork(() -> {
			Util.chunksend.add(this);
        });
        ctx.get().setPacketHandled(true);
	}
}
