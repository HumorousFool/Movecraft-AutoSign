package io.github.humorousfool.movecraftautosign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.util.MathUtils;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public class MovecraftAutoSign extends JavaPlugin implements Listener
{
    private final HashMap<Craft, List<SignData>> craftSigns = new HashMap<>();

    public void onEnable()
    {
        saveDefaultConfig();

        if(getServer().getPluginManager().getPlugin("Movecraft") == null || !getServer().getPluginManager().getPlugin("Movecraft").isEnabled())
        {
            getLogger().log(Level.SEVERE, "Movecraft not found! Disabling plugin!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onDetect(CraftDetectEvent event)
    {
        if(event.isCancelled() || event.getCraft().getNotificationPlayer() == null)
            return;

        List<SignData> signs = new ArrayList<>();

        for(MovecraftLocation loc : event.getCraft().getHitBox())
        {
            Block block = event.getCraft().getW().getBlockAt(loc.getX(), loc.getY(), loc.getZ());

            if(block.getType().name().endsWith("SIGN"))
                continue;

            Sign sign = (Sign) block.getState();
            String[] originalLines = sign.getLines().clone();
            if(sign.getLine(0).toLowerCase().contains("place pilot"))
            {
                if(!getConfig().getBoolean("enablePermanentPilot"))
                    continue;

                sign.setLine(0, "Pilot:");
                sign.setLine(1, event.getCraft().getNotificationPlayer().getName());
                sign.update();
                continue;
            }
            else if(sign.getLine(0).toLowerCase().contains("place crew"))
            {
                if(!getConfig().getBoolean("enablePermanentCrew"))
                    continue;

                sign.setLine(0, "Crew:");
                sign.setLine(1, event.getCraft().getNotificationPlayer().getName());
                sign.update();
                continue;
            }
            else if(sign.getLine(0).toLowerCase().contains("place private") || sign.getLine(0).toLowerCase().contains("place [private]"))
            {
                if(!getConfig().getBoolean("enablePermanentPrivate"))
                    continue;

                sign.setLine(0, "[Private]");
                sign.setLine(1, event.getCraft().getNotificationPlayer().getName());
                sign.update();
                continue;
            }

            if(!getConfig().getBoolean("enableAutoPilot"))
                continue;

            for(int i = 0; i < 4; i++)
            {
                if(sign.getLine(i).equalsIgnoreCase("[Pilot]"))
                {
                    sign.setLine(i, event.getCraft().getNotificationPlayer().getName());
                    sign.update();
                    MovecraftLocation relativeLocation = loc.subtract(event.getCraft().getHitBox().getMidPoint());
                    SignData data = new SignData(relativeLocation, originalLines);
                    signs.add(data);
                }
            }
        }

        craftSigns.put(event.getCraft(), signs);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRotate(CraftRotateEvent event)
    {
        if(event.isCancelled() || !craftSigns.containsKey(event.getCraft()))
            return;

        for(SignData d : craftSigns.get(event.getCraft()))
        {
            MovecraftLocation oldLocation = d.relativeLocation;
            MovecraftLocation newLocation = MathUtils.rotateVec(event.getRotation(), oldLocation).add(event.getOriginPoint());
            d.relativeLocation = newLocation.subtract(event.getNewHitBox().getMidPoint());
        }
    }

    @EventHandler
    public void onRelease(CraftReleaseEvent event)
    {
        if(event.isCancelled() || !craftSigns.containsKey(event.getCraft()))
            return;

        for (SignData d : craftSigns.get(event.getCraft()))
        {
            MovecraftLocation mLoc = d.relativeLocation.add(event.getCraft().getHitBox().getMidPoint());
            Block block = event.getCraft().getW().getBlockAt(mLoc.getX(), mLoc.getY(), mLoc.getZ());

            if(block.getType().name().endsWith("SIGN"))
                continue;

            Sign sign = (Sign) block.getState();
            for(int i = 0; i < d.content.length; i++)
            {
                sign.setLine(i, d.content[i]);
                sign.update();
            }
        }

        craftSigns.remove(event.getCraft());
    }
}
