package de.st_ddt.crazyplugin;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import de.st_ddt.crazyplugin.exceptions.CrazyCommandException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandNoSuchException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandPermissionException;
import de.st_ddt.crazyplugin.exceptions.CrazyCommandUsageException;
import de.st_ddt.crazyplugin.exceptions.CrazyException;
import de.st_ddt.crazyutil.ChatHelper;
import de.st_ddt.crazyutil.Named;
import de.st_ddt.crazyutil.PairList;
import de.st_ddt.crazyutil.locales.CrazyLocale;

public abstract class CrazyPlugin extends JavaPlugin implements Named
{

	private String chatHeader = null;
	protected CrazyLocale locale = null;
	public final static SimpleDateFormat DateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
	private static final PairList<Class<? extends CrazyPlugin>, CrazyPlugin> plugins = new PairList<Class<? extends CrazyPlugin>, CrazyPlugin>();

	public final String getChatHeader()
	{
		if (chatHeader == null)
			chatHeader = ChatColor.RED + "[" + ChatColor.GREEN + getDescription().getName() + ChatColor.RED + "] " + ChatColor.WHITE;
		return chatHeader;
	}

	public static ArrayList<CrazyPlugin> getCrazyPlugins()
	{
		return plugins.getData2List();
	}

	public final static CrazyPlugin getPlugin(final Class<? extends CrazyPlugin> plugin)
	{
		return plugins.findDataVia1(plugin);
	}

	public final static CrazyPlugin getPlugin(final String name)
	{
		for (CrazyPlugin plugin : plugins.getData2List())
			if (plugin.getName().equalsIgnoreCase(name))
				return plugin;
		return null;
	}

	@Override
	public final boolean onCommand(final CommandSender sender, final Command command, final String commandLabel, final String[] args)
	{
		try
		{
			if (command(sender, commandLabel, args))
				return true;
			if (getDescription().getName().equalsIgnoreCase(commandLabel) || (commandLabel.equalsIgnoreCase(getShortPluginName())))
			{
				try
				{
					if (args.length == 0)
					{
						commandInfo(sender, new String[0]);
						return true;
					}
					String[] newArgs = ChatHelper.shiftArray(args, 1);
					if (commandMain(sender, args[0], newArgs))
						return true;
					if (args[0].equalsIgnoreCase("info"))
					{
						commandInfo(sender, newArgs);
						return true;
					}
					if (args[0].equalsIgnoreCase("reload"))
					{
						commandReload(sender, newArgs);
						return true;
					}
					if (args[0].equalsIgnoreCase("save"))
					{
						commandSave(sender, newArgs);
						return true;
					}
					if (args[0].equalsIgnoreCase("help"))
					{
						commandHelp(sender, newArgs);
						return true;
					}
					throw new CrazyCommandNoSuchException("Function", args[0]);
				}
				catch (CrazyCommandException e)
				{
					e.shiftCommandIndex();
					throw e;
				}
			}
		}
		catch (CrazyCommandException e)
		{
			e.setCommand(commandLabel, args);
			e.print(sender, getChatHeader());
			return true;
		}
		catch (CrazyException e)
		{
			e.print(sender, getChatHeader());
			return true;
		}
		return super.onCommand(sender, command, commandLabel, args);
	}

	public boolean command(final CommandSender sender, final String commandLabel, final String[] args) throws CrazyException
	{
		return false;
	}

	public boolean commandMain(final CommandSender sender, final String commandLabel, final String[] args) throws CrazyException
	{
		return false;
	}

	public void commandInfo(final CommandSender sender, final String[] newArgs)
	{
		sender.sendMessage(getChatHeader() + "Version " + getDescription().getVersion());
		sender.sendMessage(getChatHeader() + "Authors " + getDescription().getAuthors().toString());
	}

	public final void commandReload(final CommandSender sender, final String[] args) throws CrazyCommandException
	{
		if (!sender.hasPermission(getDescription().getName().toLowerCase() + ".reload"))
			throw new CrazyCommandPermissionException();
		if (args.length != 0)
			throw new CrazyCommandUsageException("/" + getDescription().getName().toLowerCase() + " reload");
		reloadConfig();
		load();
		sendLocaleMessage(CrazyLocale.getLocaleHead().getLanguageEntry("CRAZYPLUGIN.COMMAND.CONFIG.RELOADED"), sender);
	}

	private final void commandSave(final CommandSender sender, final String[] args) throws CrazyCommandException
	{
		if (!sender.hasPermission(getDescription().getName().toLowerCase() + ".save"))
			throw new CrazyCommandPermissionException();
		if (args.length != 0)
			throw new CrazyCommandUsageException("/" + getDescription().getName().toLowerCase() + " save");
		save();
		sendLocaleMessage(CrazyLocale.getLocaleHead().getLanguageEntry("CRAZYPLUGIN.COMMAND.CONFIG.SAVED"), sender);
	}

	public void commandHelp(final CommandSender sender, final String[] args)
	{
		sendLocaleMessage(CrazyLocale.getLocaleHead().getLanguageEntry("CRAZYPLUGIN.COMMAND.HELP.NOHELP"), sender);
	}

	protected String getShortPluginName()
	{
		return null;
	}

	@Override
	public void onLoad()
	{
		plugins.setDataVia1(this.getClass(), this);
		getDataFolder().mkdir();
		new File(getDataFolder().getPath() + "/lang").mkdirs();
		super.onLoad();
	}

	@Override
	public void onEnable()
	{
		for (String language : CrazyLocale.getLoadedLanguages())
			loadLanguage(language);
		checkLocale();
		load();
		consoleLog("Version " + getDescription().getVersion() + " enabled");
	}

	@Override
	public void onDisable()
	{
		save();
		consoleLog("disabled");
	}

	public void save()
	{
		saveConfig();
	}

	public void load()
	{
	}

	public void checkLocale()
	{
		locale = CrazyLocale.getLocaleHead().getLanguageEntry(getDescription().getName());
	}

	public final void consoleLog(String message)
	{
		getServer().getConsoleSender().sendMessage(getChatHeader() + message);
	}

	public final void sendLocaleMessage(final String localepath, final CommandSender target, final String... args)
	{
		sendLocaleMessage(getLocale().getLanguageEntry(localepath), target, args);
	}

	public final void sendLocaleMessage(final CrazyLocale locale, final CommandSender target, final String... args)
	{
		target.sendMessage(getChatHeader() + ChatHelper.putArgs(locale.getLanguageText(target), args));
	}

	public final void sendLocaleMessage(final String localepath, final CommandSender[] targets, final String... args)
	{
		sendLocaleMessage(getLocale().getLanguageEntry(localepath), targets, args);
	}

	public final void sendLocaleMessage(final CrazyLocale locale, final CommandSender[] targets, final String... args)
	{
		for (CommandSender target : targets)
			target.sendMessage(getChatHeader() + ChatHelper.putArgs(locale.getLanguageText(target), args));
	}

	public final void broadcastLocaleMessage(final String localepath, final String... args)
	{
		broadcastLocaleMessage(getLocale().getLanguageEntry(localepath), args);
	}

	public final void broadcastLocaleMessage(final CrazyLocale locale, final String... args)
	{
		sendLocaleMessage(locale, getServer().getConsoleSender(), args);
		sendLocaleMessage(locale, getServer().getOnlinePlayers(), args);
	}

	public final void broadcastLocaleMessage(final boolean console, final boolean op, final String permission, final String localepath, final String... args)
	{
		broadcastLocaleMessage(console, op, permission, getLocale().getLanguageEntry(localepath), args);
	}

	public final void broadcastLocaleMessage(final boolean console, final boolean op, final String permission, final CrazyLocale locale, final String... args)
	{
		if (console)
			sendLocaleMessage(locale, Bukkit.getConsoleSender(), args);
		for (Player player : Bukkit.getOnlinePlayers())
			if (player.isOp())
				sendLocaleMessage(locale, player, args);
			else if (permission != null)
				if (player.hasPermission(permission))
					sendLocaleMessage(locale, player, args);
	}

	public final CrazyLocale getLocale()
	{
		return locale;
	}

	public void loadLanguage(final String language)
	{
		loadLanguage(language, getServer().getConsoleSender());
	}

	public void loadLanguage(final String language, final CommandSender sender)
	{
		File file = new File(getDataFolder().getPath() + "/lang/" + language + ".lang");
		if (!file.exists())
		{
			downloadLanguage(language);
			if (!file.exists())
			{
				unpackLanguage(language);
				if (!file.exists())
				{
					sender.sendMessage("Language " + language + " not availiable for " + getDescription().getName() + "!");
					return;
				}
			}
		}
		try
		{
			InputStream stream = null;
			InputStreamReader reader = null;
			try
			{
				stream = new FileInputStream(file);
				reader = new InputStreamReader(stream, "UTF-8");
				CrazyLocale.readFile(language, reader);
			}
			finally
			{
				if (reader != null)
					reader.close();
				if (stream != null)
					stream.close();
			}
		}
		catch (IOException e)
		{
			sender.sendMessage("Failed reading " + language + " languagefile for " + getDescription().getName() + "!");
		}
	}

	public void unpackLanguage(final String language)
	{
		try
		{
			InputStream stream = null;
			InputStream in = null;
			OutputStream out = null;
			try
			{
				stream = getClass().getResourceAsStream("/resource/lang/" + language + ".lang");
				if (stream == null)
					return;
				in = new BufferedInputStream(stream);
				out = new BufferedOutputStream(new FileOutputStream(getDataFolder().getPath() + "/lang/" + language + ".lang"));
				byte data[] = new byte[1024];
				int count;
				while ((count = in.read(data, 0, 1024)) != -1)
					out.write(data, 0, count);
				out.flush();
			}
			finally
			{
				if (out != null)
					out.close();
				if (stream != null)
					stream.close();
				if (in != null)
					in.close();
			}
		}
		catch (IOException e)
		{
			System.err.println("Error exporting " + language + " language file");
			// e.printStackTrace();
		}
	}

	public String getMainDownloadLocation()
	{
		return "https://raw.github.com/ST-DDT/Crazy/master/" + getDescription().getName() + "/src/resource";
	}

	public void downloadLanguage(final String language)
	{
		try
		{
			InputStream stream = null;
			BufferedInputStream in = null;
			FileOutputStream out = null;
			try
			{
				stream = new URL(getMainDownloadLocation() + "/lang/" + language + ".lang").openStream();
				if (stream == null)
					return;
				in = new BufferedInputStream(stream);
				out = new FileOutputStream(getDataFolder().getPath() + "/lang/" + language + ".lang");
				byte data[] = new byte[1024];
				int count;
				while ((count = in.read(data, 0, 1024)) != -1)
					out.write(data, 0, count);
				out.flush();
			}
			finally
			{
				if (in != null)
					in.close();
				if (stream != null)
					stream.close();
				if (out != null)
					out.close();
			}
		}
		catch (IOException e)
		{
			System.err.println("Error downloading " + language + " language file");
			// System.err.println("from: "+getMainDownloadLocation() + "/resource/lang/" + language + ".lang");
			// e.printStackTrace();
		}
	}
}
