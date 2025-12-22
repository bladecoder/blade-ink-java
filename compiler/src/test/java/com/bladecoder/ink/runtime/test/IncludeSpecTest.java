package com.bladecoder.ink.runtime.test;

import com.bladecoder.ink.compiler.Compiler;
import com.bladecoder.ink.compiler.IFileHandler;
import com.bladecoder.ink.runtime.Story;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class IncludeSpecTest {

    @Test
    public void includeBasic() throws Exception {
        List<String> text = new ArrayList<>();

        String source = TestUtils.readFileAsString("inkfiles/include/main.ink");
        Compiler.Options options = new Compiler.Options();
        options.sourceFilename = "main.ink";
        options.fileHandler = new ResourceFileHandler("inkfiles/include");

        Compiler compiler = new Compiler(source, options);
        Story story = compiler.compile();

        TestUtils.nextAll(story, text);
        Assert.assertEquals(2, text.size());
        Assert.assertEquals("This is included.", text.get(0));
        Assert.assertEquals("This is main.", text.get(1));
    }

    private static class ResourceFileHandler implements IFileHandler {
        private final String basePath;

        private ResourceFileHandler(String basePath) {
            this.basePath = basePath;
        }

        @Override
        public String resolveInkFilename(String includeName) {
            return basePath + "/" + includeName;
        }

        @Override
        public String loadInkFileContents(String fullFilename) throws IOException {
            return TestUtils.readFileAsString(fullFilename);
        }
    }
}
