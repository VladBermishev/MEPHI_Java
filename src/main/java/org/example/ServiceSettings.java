package org.example;
import java.io.File;
import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

public class ServiceSettings {
    private File repositoryPath;
    private String serviceAddress;
    private int codeLength;
    private int maxClics;
    private int ttl;

    public ServiceSettings() {
        repositoryPath = new File("/tmp/link-shortener");
        serviceAddress = "clck.ru";
        codeLength = 6;
        maxClics = 2;
        ttl = 60;
    }

    public void from(Properties props){
        repositoryPath = new File(props.getProperty("repository_path", repositoryPath.getAbsolutePath()));
        serviceAddress = props.getProperty("service_address", serviceAddress);
        codeLength = Integer.parseInt(props.getProperty("code_length", String.valueOf(codeLength)));
        maxClics = Integer.parseInt(props.getProperty("max_clics", String.valueOf(maxClics)));
        ttl = Integer.parseInt(props.getProperty("ttl", String.valueOf(ttl)));
    }

    public String getRepositoryPath() {
        return repositoryPath.getAbsolutePath();
    }
    public File getRepositoryFile() {
        return repositoryPath;
    }
    public Duration getTtl() {
        return Duration.ofSeconds(ttl);
    }
    public int getMaxClics() {
        return maxClics;
    }
    public String getServiceAddress() {
        return serviceAddress;
    }
    public int getCodeLength() {
        return codeLength;
    }
}
