package io.muserver.handlers;

import io.muserver.MuException;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;

public interface ResourceProviderFactory {
    ResourceProvider get(String relativePath);
    static ResourceProviderFactory fileBased(Path baseDirectory) {
        if (!Files.isDirectory(baseDirectory, LinkOption.NOFOLLOW_LINKS)) {
            throw new MuException(baseDirectory + " is not a directory");
        }
        return relativePath -> new FileProvider(baseDirectory, relativePath);
    }
    static ResourceProviderFactory classpathBased(String classpathBase) {
        return relativePath -> new ClasspathResourceProvider(classpathBase, relativePath);
    }
}