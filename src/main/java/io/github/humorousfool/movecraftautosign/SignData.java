package io.github.humorousfool.movecraftautosign;

import net.countercraft.movecraft.MovecraftLocation;

public class SignData
{
    public MovecraftLocation relativeLocation;
    String[] content;

    public SignData(MovecraftLocation relativeLocation, String[] content)
    {
        this.relativeLocation = relativeLocation;
        this.content = content;
    }
}
