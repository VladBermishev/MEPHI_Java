package org.example;
import org.apache.commons.cli.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

public class Main {
    public static void main(String[] args) {
        Properties properties = new Properties();
        FileInputStream fileInputStream = null;
        ServiceSettings settings = new ServiceSettings();
        try {
            InputStream in = Main.class.getResourceAsStream("/config.properties");
            properties.load(in);
            settings.from(properties);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        } finally {
            if (fileInputStream != null) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!settings.getRepositoryFile().exists())
            settings.getRepositoryFile().mkdir();

        ShortenerServiceCLI.run(settings, args);
    }
}