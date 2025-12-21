package com.bladecoder.ink.compiler;

import java.io.IOException;

public interface IFileHandler {
    String resolveInkFilename(String includeName);

    String loadInkFileContents(String fullFilename) throws IOException;
}
