package de.st_ddt.crazyutil.geo;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

import de.st_ddt.crazyutil.ObjectSaveLoadHelper;

public class Sphere extends Geo
{

	protected Location center;
	protected int radius;

	public Sphere(Location center, int radius)
	{
		super(center.getWorld());
		this.center = center;
		this.radius = radius;
	}

	public Sphere(ConfigurationSection config, World world)
	{
		super(config, world);
		center = ObjectSaveLoadHelper.loadLocation(config.getConfigurationSection("center"), world);
		radius = config.getInt("radius.", 1);
	}

	public Location getCenter()
	{
		return center;
	}

	public void setCenter(Location center)
	{
		if (center == null)
			return;
		this.center = center;
		this.world = center.getWorld();
	}

	public int getRadius()
	{
		return radius;
	}

	public void setRadius(int radius)
	{
		this.radius = radius;
	}

	@Override
	public boolean isInside(Location loc)
	{
		return world == loc.getWorld() && center.distance(loc) < radius;
	}

	@Override
	public void save(FileConfiguration config, String path)
	{
		ObjectSaveLoadHelper.saveLocation(config, path + "center.", center, false);
		config.set(path + "range", radius);
	}
}
