package dk.cintix.librarian.config.services;

import dk.cintix.librarian.config.ConfigContract;
import dk.cintix.librarian.config.ConfigContract.ConfigData;
import dk.cintix.librarian.config.ConfigContract.DependencySpec;
import dk.cintix.librarian.config.ConfigContract.RepositoryDef;
import dk.cintix.librarian.config.services.domain.models.Configuration;
import dk.cintix.librarian.config.services.persistence.ConfigReader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ConfigService implements ConfigContract {
    private final ConfigReader configReader;

    public ConfigService() {
        this.configReader = new ConfigReader();
    }

    @Override
    public ConfigData read(Path projectDir) throws IOException {
        Configuration config = configReader.read(projectDir);
        List<DependencySpec> deps = configReader.parseDependencies(config);
        Map<String, RepositoryDef> repos = new LinkedHashMap<>(config.repositories());
        return new ConfigData(config.libDir(), config.defaultRepository(), repos, deps);
    }
}
