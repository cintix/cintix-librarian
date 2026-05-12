package dk.cintix.librarian.sync.services;

import dk.cintix.librarian.LibrarianCore;
import dk.cintix.librarian.artifact.ArtifactContract;
import dk.cintix.librarian.artifact.ArtifactContract.DownloadedArtifact;
import dk.cintix.librarian.config.ConfigContract;
import dk.cintix.librarian.config.ConfigContract.ConfigData;
import dk.cintix.librarian.config.ConfigContract.DependencySpec;
import dk.cintix.librarian.config.ConfigContract.RepositoryDef;
import dk.cintix.librarian.git.GitContract;
import dk.cintix.librarian.git.GitContract.GitResolved;
import dk.cintix.librarian.lockfile.LockFileContract;
import dk.cintix.librarian.lockfile.LockFileContract.LockFileData;
import dk.cintix.librarian.resolution.ResolutionContract;
import dk.cintix.librarian.resolution.ResolutionContract.ResolvedDependency;
import dk.cintix.librarian.resolution.ResolutionContract.VersionSpec;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SyncManager implements LibrarianCore {
    private final ConfigContract config;
    private final ResolutionContract resolution;
    private final ArtifactContract artifact;
    private final LockFileContract lockFile;
    private final GitContract git;

    public SyncManager(ConfigContract config, ResolutionContract resolution,
                        ArtifactContract artifact, LockFileContract lockFile,
                        GitContract git) {
        this.config = config;
        this.resolution = resolution;
        this.artifact = artifact;
        this.lockFile = lockFile;
        this.git = git;
    }

    @Override
    public SyncResult sync(Path projectDir) throws IOException {
        ConfigData configData = config.read(projectDir);
        List<DependencySpec> deps = configData.dependencies();
        Map<String, RepositoryDef> repos = configData.repositories();
        String defaultRepo = configData.defaultRepository();
        Path libDir = projectDir.resolve(configData.libDir());

        artifact.ensureDirectory(libDir);

        List<ResolvedDependency> resolved = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (DependencySpec dep : deps) {
            try {
                if (dep.isGit()) {
                    resolved.add(resolveAndDownloadGit(dep, libDir));
                } else {
                    RepositoryDef repo = resolveRepo(dep, defaultRepo, repos);
                    resolved.add(resolveAndDownloadMaven(dep, repo, libDir));
                }
            } catch (Exception e) {
                errors.add(dep.coordinate() + ": " + e.getMessage());
            }
        }

        Set<String> expectedFiles = resolved.stream()
                .map(ResolvedDependency::fileName)
                .collect(Collectors.toSet());
        List<Path> removed = artifact.cleanOutdated(libDir, expectedFiles);

        LockFileData lockFileData = lockFile.generate(resolved);
        lockFile.write(lockFileData, projectDir);

        return new SyncResult(
                resolved.stream().map(this::toResolvedDepInfo).toList(),
                removed.stream().map(p -> p.getFileName().toString()).toList(),
                errors,
                toLockFileInfo(lockFileData));
    }

    private ResolvedDependency resolveAndDownloadMaven(DependencySpec dep, RepositoryDef repo, Path libDir)
            throws IOException {
        VersionSpec spec = VersionSpec.parse(dep.version());
        ResolvedDependency resolvedDep = resolution.resolve(spec, dep, repo);
        DownloadedArtifact downloaded = artifact.download(dep, resolvedDep.resolvedVersion(), repo, libDir);
        return new ResolvedDependency(
                dep.groupId(), dep.artifactId(), dep.coordinate(),
                dep.version(), resolvedDep.resolvedVersion(),
                repoNameInResult(dep), downloaded.checksum());
    }

    private ResolvedDependency resolveAndDownloadGit(DependencySpec dep, Path libDir) throws IOException {
        GitResolved gitResolved = git.resolve(dep.coordinate(), dep.version());
        var gitAsset = git.download(gitResolved, libDir);
        String repoName = extractRepoName(dep.coordinate());
        return new ResolvedDependency(
                dep.coordinate(), repoName, dep.coordinate(),
                dep.version(), gitResolved.version(),
                "git:" + dep.coordinate(), gitAsset.checksum());
    }

    @Override
    public LockFileInfo resolve(Path projectDir) throws IOException {
        ConfigData configData = config.read(projectDir);
        List<DependencySpec> deps = configData.dependencies();
        Map<String, RepositoryDef> repos = configData.repositories();
        String defaultRepo = configData.defaultRepository();

        List<ResolvedDependency> resolved = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        for (DependencySpec dep : deps) {
            try {
                if (dep.isGit()) {
                    GitResolved gitResolved = git.resolve(dep.coordinate(), dep.version());
                    String repoName = extractRepoName(dep.coordinate());
                    resolved.add(new ResolvedDependency(
                            dep.coordinate(), repoName, dep.coordinate(),
                            dep.version(), gitResolved.version(),
                            "git:" + dep.coordinate(), null));
                } else {
                    RepositoryDef repo = resolveRepo(dep, defaultRepo, repos);
                    VersionSpec spec = VersionSpec.parse(dep.version());
                    ResolvedDependency r = resolution.resolve(spec, dep, repo);
                    resolved.add(new ResolvedDependency(
                            dep.groupId(), dep.artifactId(), dep.coordinate(),
                            dep.version(), r.resolvedVersion(), repoNameInResult(dep), null));
                }
            } catch (Exception e) {
                errors.add(dep.coordinate() + ": " + e.getMessage());
            }
        }

        if (!errors.isEmpty()) {
            throw new IOException("Resolution errors: " + String.join("; ", errors));
        }

        LockFileData lockFileData = lockFile.generate(resolved);
        lockFile.write(lockFileData, projectDir);
        return toLockFileInfo(lockFileData);
    }

    @Override
    public LockFileInfo update(Path projectDir) throws IOException {
        return sync(projectDir).lockFile();
    }

    @Override
    public DoctorResult doctor(Path projectDir) throws IOException {
        boolean configValid;
        String configError = null;
        int dependencyCount = 0;
        List<String> reachableRepos = new ArrayList<>();
        List<String> unreachableRepos = new ArrayList<>();
        boolean lockFileExists = false;
        int lockEntryCount = 0;
        List<String> missingJars = new ArrayList<>();

        try {
            ConfigData configData = config.read(projectDir);
            configValid = true;
            dependencyCount = configData.dependencies().size();

            Map<String, RepositoryDef> repos = configData.repositories();
            for (Map.Entry<String, RepositoryDef> entry : repos.entrySet()) {
                try {
                    var client = new dk.cintix.librarian.resolution.services.persistence.MavenMetadataClient();
                    if (client.checkReachable(entry.getValue())) {
                        reachableRepos.add(entry.getKey());
                    } else {
                        unreachableRepos.add(entry.getKey());
                    }
                } catch (Exception e) {
                    unreachableRepos.add(entry.getKey());
                }
            }

            LockFileData lockFileData = lockFile.read(projectDir);
            if (lockFileData != null) {
                lockFileExists = true;
                lockEntryCount = lockFileData.dependencies().size();
                Path libDir = projectDir.resolve(configData.libDir());
                Set<String> existingJars = artifact.existingJars(libDir);
                for (var entry : lockFileData.dependencies().entrySet()) {
                    String coordinate = entry.getKey();
                    String artifactId;
                    if (coordinate.contains(":")) {
                        artifactId = coordinate.substring(coordinate.indexOf(":") + 1);
                    } else {
                        int slash = coordinate.lastIndexOf('/');
                        artifactId = slash >= 0 ? coordinate.substring(slash + 1) : coordinate;
                    }
                    String fileName = artifactId + "-" + entry.getValue().resolved() + ".jar";
                    if (!existingJars.contains(fileName)) {
                        missingJars.add(fileName);
                    }
                }
            }
        } catch (IOException e) {
            configValid = false;
            configError = e.getMessage();
        }

        return new DoctorResult(configValid, configError, dependencyCount,
                lockFileExists, lockEntryCount, reachableRepos, unreachableRepos, missingJars);
    }

    private RepositoryDef resolveRepo(DependencySpec dep, String defaultRepo,
                                       Map<String, RepositoryDef> repos) {
        String repoName = dep.repository() != null ? dep.repository() : defaultRepo;
        RepositoryDef repo = repos.get(repoName);
        if (repo == null)
            throw new IllegalArgumentException("Repository not found: " + repoName);
        return repo;
    }

    private String repoNameInResult(DependencySpec dep) {
        return dep.repository() != null ? dep.repository() : "maven-central";
    }

    private String extractRepoName(String repoUrl) {
        String path = repoUrl;
        if (path.startsWith("https://")) path = path.substring(8);
        if (path.startsWith("http://")) path = path.substring(7);
        if (path.endsWith(".git")) path = path.substring(0, path.length() - 4);
        if (path.endsWith("/")) path = path.substring(0, path.length() - 1);
        int slash = path.lastIndexOf('/');
        return slash >= 0 ? path.substring(slash + 1) : path;
    }

    private ResolvedDepInfo toResolvedDepInfo(ResolvedDependency dep) {
        return new ResolvedDepInfo(dep.coordinate(), dep.requestedVersion(),
                dep.resolvedVersion(), dep.repository(), dep.checksum());
    }

    private LockFileInfo toLockFileInfo(LockFileData data) {
        Map<String, LockEntryInfo> entries = new LinkedHashMap<>();
        for (var e : data.dependencies().entrySet()) {
            var v = e.getValue();
            entries.put(e.getKey(), new LockEntryInfo(v.requested(), v.resolved(),
                    v.repository(), v.checksum()));
        }
        return new LockFileInfo(entries);
    }
}
