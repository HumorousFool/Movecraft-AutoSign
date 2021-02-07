package io.github.humorousfool.movecraftautosign;

import net.countercraft.movecraft.MovecraftLocation;
import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.events.CraftDetectEvent;
import net.countercraft.movecraft.events.CraftReleaseEvent;
import net.countercraft.movecraft.events.CraftRotateEvent;
import net.countercraft.movecraft.utils.MathUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;

public class MovecraftAutoSign extends JavaPlugin implements Listener
{
    private final HashMap<Craft, List<SignData>> craftSigns = new HashMap<>();

    public void onEnable()
    {
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

            if(block.getType() != Material.WALL_SIGN && block.getType() != Material.SIGN_POST)
                continue;

            Sign sign = (Sign) block.getState();
            String[] originalLines = sign.getLines().clone();

            if(sign.getLine(0).equalsIgnoreCase("[Crew]"))
            {
                sign.setLine(0, "Crew:");
                sign.setLine(1, event.getCraft().getNotificationPlayer().getName());
                sign.update();
                MovecraftLocation relativeLocation = loc.subtract(event.getCraft().getHitBox().getMidPoint());
                SignData data = new SignData(relativeLocation, originalLines);
                signs.add(data);
                continue;
            }

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

    @EventHandler
    public void onRotate(CraftRotateEvent event)
    {
        if(event.isCancelled() || !craftSigns.containsKey(event.getCraft()))
            return;

        new BukkitRunnable()
        {
            @Override
            public void run()
            {
                if(!craftSigns.containsKey(event.getCraft()))
                    return;

                for(SignData d : craftSigns.get(event.getCraft()))
                {
                    MovecraftLocation oldLocation = d.relativeLocation;
                    MovecraftLocation newLocation = MathUtils.rotateVec(event.getRotation(), oldLocation).add(event.getOriginPoint());
                    d.relativeLocation = newLocation.subtract(event.getCraft().getHitBox().getMidPoint());
                }
            }
        }.runTaskLater(this, 2);
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

            if(block.getType() != Material.WALL_SIGN && block.getType() != Material.SIGN_POST)
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
