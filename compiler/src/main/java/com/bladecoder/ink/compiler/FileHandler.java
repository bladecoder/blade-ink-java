package com.bladecoder.ink.compiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class DefaultFileHandler implements IFileHandler {
    @Override
    public String resolveInkFilename(String includeName) {
        Path workingDir = Paths.get("").toAbsolutePath();
        return workingDir.resolve(includeName).toString();
    }

    @Override
    public String loadInkFileContents(String fullFilename) throws IOException {
        byte[] bytes = Files.readAllBytes(Paths.get(fullFilename));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
