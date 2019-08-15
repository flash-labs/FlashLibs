package dev.flashlabs.flashlibs.text;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.gson.GsonConfigurationLoader;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader;
import sun.util.ResourceBundleEnumeration;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * Represents a {@link ResourceBundle} loaded from an {@link ConfigurationNode}.
 * All values are registered under the path to the node, consisting of all keys
 * joined with {@code '.'}.
 */
final class NodeResourceBundle extends ResourceBundle {

    static final Control CONTROL = new Control();

    private final Map<String, Object> map = Maps.newHashMap();

    private NodeResourceBundle(ConfigurationNode node) {
        loadValues(node);
    }

    private void loadValues(ConfigurationNode node) {
        if (node.hasListChildren()) {
            node.getChildrenList().forEach(this::loadValues);
        } else if (node.hasMapChildren()) {
            node.getChildrenMap().values().forEach(this::loadValues);
        } else {
            map.put(Joiner.on('.').join(node.getPath()), node.getValue());
        }
    }

    @Override
    protected Object handleGetObject(String key) {
        return map.get(key);
    }

    @Override
    public Enumeration<String> getKeys() {
        return new ResourceBundleEnumeration(map.keySet(), parent != null ? parent.getKeys() : null);
    }

    /**
     * The {@link ResourceBundle.Control} implementation supporting additional
     * formats with Configurate. Hocon (.conf), Json (.json) and Yaml (.yaml)
     * extensions are supported, as well as the default properties file.
     */
    private static final class Control extends ResourceBundle.Control {

        private static final ImmutableList<String> FORMATS = ImmutableList.of("conf", "json", "yaml", "properties");

        @Override
        public List<String> getFormats(String baseName) {
            return FORMATS;
        }

        @Override
        public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) throws IOException {
            URL url = loader.getResource(toResourceName(toBundleName(baseName, locale), format));
            if (url != null) {
                ConfigurationLoader config;
                switch (format) {
                    case "conf": config = HoconConfigurationLoader.builder().setURL(url).build(); break;
                    case "json": config = GsonConfigurationLoader.builder().setURL(url).build(); break;
                    case "yaml": config = YAMLConfigurationLoader.builder().setURL(url).build(); break;
                    default: return newBundle(baseName, locale, "java.properties", loader, reload);
                }
                return new NodeResourceBundle(config.load());
            }
            return null;
        }

    }

}
