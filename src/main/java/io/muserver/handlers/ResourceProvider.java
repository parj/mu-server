package io.muserver.handlers;

import io.muserver.MuException;
import io.muserver.MuResponse;
import io.muserver.Mutils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.net.www.protocol.file.FileURLConnection;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.Instant;
import java.util.Date;

import static io.muserver.Mutils.htmlEncode;

interface ResourceProvider {
    boolean exists();

    boolean isDirectory();

    Long fileSize();

    Date lastModified();

    void sendTo(MuResponse response, boolean sendBody) throws IOException;

    boolean showDirectoryListing(MuResponse response, boolean sendBody) throws IOException;
}

interface ResourceProviderFactory {

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


class FileProvider implements ResourceProvider {
    private static final Logger log = LoggerFactory.getLogger(FileProvider.class);
    private final Path localPath;

    FileProvider(Path baseDirectory, String relativePath) {
        if (relativePath.startsWith("/")) {
            relativePath = "." + relativePath;
        }
        this.localPath = baseDirectory.resolve(relativePath);
    }

    public boolean exists() {
        return Files.exists(localPath);
    }

    @Override
    public boolean isDirectory() {
        return Files.isDirectory(localPath);
    }

    public Long fileSize() {
        try {
            return Files.size(localPath);
        } catch (IOException e) {
            log.error("Error finding file size: " + e.getMessage());
            return null;
        }
    }

    @Override
    public Date lastModified() {
        try {
            return new Date(Files.getLastModifiedTime(localPath).toMillis());
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void sendTo(MuResponse response, boolean sendBody) throws IOException {
        if (sendBody) {
            try (OutputStream os = response.outputStream()) {
                Files.copy(localPath, os);
            }
        } else {
            response.outputStream();
        }
    }

    @Override
    public boolean showDirectoryListing(MuResponse response, boolean sendBody) throws IOException {

        response.contentType("text/html; charset=utf-8");

        Path dir = localPath.getParent().normalize().toAbsolutePath();
        if (!Files.isDirectory(dir)) {
            return false;
        }

        String title = "Index of " + dir.getFileName();

        try (BufferedWriter writer = new BufferedWriter(response.writer(), 8192)) {
            writer.append("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head><title>" + htmlEncode(title) + "</title>\n" +
                "<style>\n" +
                "    th { text-align: left }\n" +
                "    .size { text-align: right; padding-right: 20px; padding-left: 20px; }\n" +
                "    footer { font-style: italic; font-size: smaller; margin: 30px 0 100px 0}\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<h1>" + htmlEncode(title) + "</h1>\n" +
                "<table>\n" +
                "    <thead>\n" +
                "    <tr>\n" +
                "        <th>Name</th>\n" +
                "        <th class=\"size\">Size</th>\n" +
                "        <th>Last Modified</th>\n" +
                "    </tr>\n" +
                "    </thead>\n" +
                "    <tbody>");

            try (
                DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
                for (Path child : ds) {
                    File file = child.toFile();
                    String size = NumberFormat.getIntegerInstance().format(file.length());
                    String update = htmlEncode(Instant.ofEpochMilli(file.lastModified()).toString());
                    writer.append("<tr><td>" + htmlEncode(file.getName()) + "</td><td class=\"size\">" + size + "</td><td>" + update + "</td></tr>");
                }
            }

            writer.append("</tbody>\n" +
                "</table>\n" +
                "<footer>\n" +
                "    Generated at \n" + Instant.now() +
                "</footer>\n" +
                "</body></html>");
        }
        return true;
    }

}

class ClasspathResourceProvider implements ResourceProvider {
    private static final Logger log = LoggerFactory.getLogger(ClasspathResourceProvider.class);
    private final URLConnection info;
    private final boolean isDir;

    ClasspathResourceProvider(String classpathBase, String relativePath) {
        URLConnection con;
        String path = Mutils.join(classpathBase, "/", relativePath);
        URL resource = ClasspathResourceProvider.class.getResource(path);
        if (resource == null) {
            con = null;
        } else {
            try {
                con = resource.openConnection();
            } catch (IOException e) {
                log.error("Error opening " + resource, e);
                con = null;
            }
        }
        this.info = con;
        boolean isDir = false;
        if (con != null) {
            if (con instanceof JarURLConnection) {
                JarURLConnection juc = (JarURLConnection) con;
                try {
                    isDir = juc.getJarEntry().isDirectory();
                } catch (IOException e) {
                    log.error("Error checking if " + resource + " is a directory", e);
                }
            } else if (con instanceof FileURLConnection) {
                FileURLConnection fuc = (FileURLConnection) con;
                isDir = new File(fuc.getURL().getFile()).isDirectory();
            } else {
                log.warn("Unexpected jar entry type for " + resource + ": " + con.getClass());
            }

        }
        this.isDir = isDir;
    }

    public boolean exists() {
        return info != null;
    }

    @Override
    public boolean isDirectory() {
        return isDir;
    }

    @Override
    public Long fileSize() {
        if (isDir) {
            return null;
        }
        long size = info.getContentLengthLong();
        return size >= 0 ? size : null;
    }

    @Override
    public Date lastModified() {
        long mod = info.getLastModified();
        return mod >= 0 ? new Date(mod) : null;
    }

    @Override
    public void sendTo(MuResponse response, boolean sendBody) throws IOException {
        if (sendBody) {
            try (OutputStream out = response.outputStream()) {
                Mutils.copy(info.getInputStream(), out, 8192);
            }
        } else {
            response.outputStream();
        }
    }

    @Override
    public boolean showDirectoryListing(MuResponse response, boolean sendBody) {
        return false;
    }

}