package cc.silk.event.impl.level;

import cc.silk.event.types.Event;
import lombok.Getter;
import net.minecraft.client.multiplayer.ClientLevel;

@Getter
public class WorldChangeEvent implements Event {
    ClientLevel level;

    public WorldChangeEvent(ClientLevel level) {
        this.level = level;
    }
}
