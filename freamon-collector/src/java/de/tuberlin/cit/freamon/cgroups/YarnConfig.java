package de.tuberlin.cit.freamon.cgroups;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper for retrieving certain settings from yarn-site.xml
 */
public class YarnConfig {

	public final Map<String, String> configMap = new HashMap<String, String>();
	public String cgroupsMountPath;
	public String cgroupsHierarchy;
	public String configPath;

	public YarnConfig(String configPath) throws IOException {
		// TODO infer configPath from `HADOOP_CONF_DIR` or `HADOOP_PREFIX`
		this.configPath = configPath;

		try {
			ConfigurationUtils.load(configMap, this.configPath);
		} catch (IOException e) {
			throw new IOException("Could not load " + this.configPath);
		}

		this.cgroupsMountPath = getOrDefault("yarn.nodemanager.linux-container-executor.cgroups.mount-path", "/sys/fs/cgroup");
		this.cgroupsHierarchy = getOrDefault("yarn.nodemanager.linux-container-executor.cgroups.hierarchy", "hadoop-yarn");
	}

	public String getOrDefault(String key, String defaultValue) {
		// Java 8 Map has this method built in
		String value = configMap.get(key);
		if (value == null) {
			return defaultValue;
		}
		return value;
	}

}
