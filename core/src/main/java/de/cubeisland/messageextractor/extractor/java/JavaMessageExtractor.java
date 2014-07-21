/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2014 Phillip Schichtel, Stefan Wolf
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package de.cubeisland.messageextractor.extractor.java;

import com.martiansoftware.jsap.JSAPException;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import de.cubeisland.messageextractor.exception.MessageExtractionException;
import de.cubeisland.messageextractor.exception.SourceDirectoryNotExistingException;
import de.cubeisland.messageextractor.extractor.ExtractorConfiguration;
import de.cubeisland.messageextractor.extractor.MessageExtractor;
import de.cubeisland.messageextractor.extractor.java.processor.MethodInvocationProcessor;
import de.cubeisland.messageextractor.message.MessageStore;
import de.cubeisland.messageextractor.util.Misc;
import spoon.Launcher;
import spoon.compiler.SpoonCompiler;
import spoon.compiler.SpoonResourceHelper;
import spoon.processing.ProcessingManager;
import spoon.reflect.factory.Factory;
import spoon.support.QueueProcessingManager;

public class JavaMessageExtractor implements MessageExtractor
{
    private final FileFilter fileFilter;

    /**
     * constructor creates an instance of this class
     */
    public JavaMessageExtractor()
    {
        this.fileFilter = new JavaFileFilter();
    }

    /**
     * {@inheritDoc}
     *
     * @param config the config which shall be used to extract the messages
     *
     * @return
     *
     * @throws MessageExtractionException
     */
    @Override
    public MessageStore extract(ExtractorConfiguration config) throws MessageExtractionException
    {
        return this.extract(config, null);
    }

    /**
     * {@inheritDoc}
     *
     * @param config             the config which shall be used to extract the messages
     * @param loadedMessageStore a messagestore containing the messages from the old catalog
     *
     * @return
     *
     * @throws MessageExtractionException
     */
    @Override
    public MessageStore extract(ExtractorConfiguration config, MessageStore loadedMessageStore) throws MessageExtractionException
    {
        JavaExtractorConfiguration extractorConfig = (JavaExtractorConfiguration) config;

        if (!extractorConfig.getDirectory().exists())
        {
            throw new SourceDirectoryNotExistingException();
        }
        List<File> files;
        try
        {
            files = Misc.scanFilesRecursive(extractorConfig.getDirectory(), this.fileFilter);
        }
        catch (IOException e)
        {
            throw new MessageExtractionException("Failed to enlist the applicable files!", e);
        }

        MessageStore messageStore = loadedMessageStore;
        if (messageStore == null)
        {
            messageStore = new MessageStore();
        }

        String[] environment = new String[files.size()];
        for (int i = 0; i < environment.length; i++)
        {
            environment[i] = files.get(i).getAbsolutePath();
        }

        try
        {
            Launcher launcher = new Launcher();
            SpoonCompiler compiler = launcher.createCompiler();
            compiler.addInputSources(SpoonResourceHelper.resources(environment));

            compiler.setEncoding(config.getCharset().name());

            Factory spoonFactory = compiler.getFactory();
            ProcessingManager processManager = new QueueProcessingManager(spoonFactory);
            processManager.addProcessor(new MethodInvocationProcessor((JavaExtractorConfiguration) config, messageStore));

            spoonFactory.getEnvironment().setManager(processManager);
            compiler.build();

            processManager.process();
        }
        catch (JSAPException e)
        {
            e.printStackTrace();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return messageStore;
    }
}
