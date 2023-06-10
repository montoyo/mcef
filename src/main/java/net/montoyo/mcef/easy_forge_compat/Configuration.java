package net.montoyo.mcef.easy_forge_compat;

import com.electronwill.nightconfig.core.*;
import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.electronwill.nightconfig.core.io.WritingMode;
import com.electronwill.nightconfig.toml.TomlWriter;
import com.mojang.datafixers.util.Pair;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.montoyo.mcef.client.UnsafeUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Configuration {
	private final ForgeConfigSpec.Builder configBuilder = new ForgeConfigSpec.Builder();
	ForgeConfigSpec spec;
	private final ModConfig.Type type;
	private final String file;
	
	private final HashMap<String, Pair<Supplier<Object>, Consumer<Object>>> entries = new HashMap<>();
	
	HashMap<String, ForgeConfigSpec.Builder> sections = new HashMap<>();
	
	public Configuration(String file, ModConfig.Type type) {
		this.type = type;
		this.file = file;
	}
	
	public void getBoolean(String optName, String section, boolean defaultValue, String desc, Field field) {
		configBuilder.push(section);
		long handle = UnsafeUtil.getHandle(field);
		Object base = UnsafeUtil.getBase(field);
		ForgeConfigSpec.BooleanValue value = configBuilder
				.comment(desc.replace(". ", "\n"))
				.define(optName, defaultValue);
		entries.put(section + "." + optName, Pair.of(
				value::get,
				(v) -> UnsafeUtil.setBoolean(base, handle, (boolean) v)
		));
		UnsafeUtil.setBoolean(base, handle, defaultValue);
		configBuilder.pop();
	}
	
	public void getString(String optName, String section, String defaultValue, String desc, Consumer<String> valueConsumer) {
		configBuilder.push(section);
		ForgeConfigSpec.ConfigValue<String> value = configBuilder
				.comment(desc.replace(". ", "\n"))
				.define(optName, defaultValue);
		entries.put(section + "." + optName, Pair.of(
				value::get,
				(v) -> valueConsumer.accept((String) v)
		));
		valueConsumer.accept(defaultValue);
		configBuilder.pop();
	}
	
	public void getString(String optName, String section, String defaultValue, String desc, Field field) {
		long handle = UnsafeUtil.getHandle(field);
		Object base = UnsafeUtil.getBase(field);
		getString(optName, section, defaultValue, desc, (value) -> UnsafeUtil.setObject(base, handle, value));
	}
	
	protected void clone(UnmodifiableConfig src, CommentedConfig dst) {
		for (String s : src.valueMap().keySet()) {
			var entry = src.get(s);
			if (entry instanceof AbstractConfig cfg1) {
				CommentedConfig nested = CommentedConfig.inMemory();
				clone(cfg1, nested);
				dst.add(s, nested);
			} else if (entry instanceof ForgeConfigSpec.ValueSpec spec) {
				dst.setComment(s, spec.getComment());
				dst.add(s, spec.getDefault());
			} else {
				dst.add(s, entry);
			}
		}
	}
	
	public void save() {
		spec = configBuilder.build();
		
		File fl = new File(FMLPaths.CONFIGDIR.get().toFile().getAbsoluteFile() + "/" + file);
		try {
			if (fl.exists()) {
				CommentedFileConfig cfg = CommentedFileConfig.builder(fl.toString()).sync().
						preserveInsertionOrder().
						autosave().
						onFileNotFound((newfile, configFormat) -> setupConfigFile(newfile, configFormat)).
						writingMode(WritingMode.REPLACE).
						build();
				cfg.load();
				spec.acceptConfig(cfg);
				cfg.save();
				
				onConfigChange((Config)spec.getSpec());
			} else {
				CommentedConfig cfg = CommentedConfig.inMemory();
				
				clone(spec.getSpec(), cfg);
				
				FileOutputStream outputStream = new FileOutputStream(fl);
				TomlWriter writer = new TomlWriter();
				outputStream.write(writer.writeToString(cfg).getBytes(StandardCharsets.UTF_8));
				outputStream.flush();
				outputStream.close();
			}
		} catch (Throwable err) {
			err.printStackTrace();
		}
	}
	
	private boolean setupConfigFile(final Path file, final ConfigFormat<?> conf) throws IOException {
		Files.createDirectories(file.getParent());
		Files.createFile(file);
		conf.initEmptyFile(file);
		return true;
	}
	
	public void onConfigChange(Config config) {
		for (String s : entries.keySet()) {
			Pair<Supplier<Object>, Consumer<Object>> entry = entries.get(s);
			entry.getSecond().accept(entry.getFirst().get());
		}
	}
}
