package com.ai.example.demo.service;

import dev.langchain4j.internal.Utils;
import org.springframework.stereotype.Component;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

@Component
public class DataEmbededService {

    public static PathMatcher glob(final String glob){
        return FileSystems.getDefault()
                .getPathMatcher("glob:" + glob);
    }
    public static Path getPath(final String relativePath){
        try{
            URL fileUrl = Utils.class.getClassLoader().getResource(relativePath);
            return Paths.get(fileUrl.toURI());
        }catch (URISyntaxException ex){
            throw  new RuntimeException(ex);
        }
    }
}
