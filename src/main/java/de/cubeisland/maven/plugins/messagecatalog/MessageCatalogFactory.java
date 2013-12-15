package de.cubeisland.maven.plugins.messagecatalog;

import org.apache.velocity.Template;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import de.cubeisland.maven.plugins.messagecatalog.exception.ConfigurationException;
import de.cubeisland.maven.plugins.messagecatalog.exception.ConfigurationNotFoundException;
import de.cubeisland.maven.plugins.messagecatalog.format.CatalogConfig;
import de.cubeisland.maven.plugins.messagecatalog.format.CatalogFormat;
import de.cubeisland.maven.plugins.messagecatalog.exception.UnknownCatalogFormatException;
import de.cubeisland.maven.plugins.messagecatalog.format.gettext.PlaintextGettextCatalogFormat;
import de.cubeisland.maven.plugins.messagecatalog.parser.SourceConfig;
import de.cubeisland.maven.plugins.messagecatalog.parser.SourceParser;
import de.cubeisland.maven.plugins.messagecatalog.exception.UnknownSourceLanguageException;
import de.cubeisland.maven.plugins.messagecatalog.parser.java.JavaSourceParser;
import de.cubeisland.maven.plugins.messagecatalog.util.Misc;

public class MessageCatalogFactory
{
    private Map<String, Class<? extends SourceParser>> sourceParserMap;
    private Map<String, Class<? extends CatalogFormat>> catalogFormatMap;

    private Logger logger;

    public MessageCatalogFactory()
    {
        this(Logger.getLogger("messagecatalog"));
    }

    public MessageCatalogFactory(Logger logger)
    {
        this.logger = logger;
        this.sourceParserMap = new HashMap<String, Class<? extends SourceParser>>();
        this.catalogFormatMap = new HashMap<String, Class<? extends CatalogFormat>>();

        this.loadDefaultClasses();
    }

    public Class<? extends SourceParser> getSourceParser(String language)
    {
        return this.sourceParserMap.get(language);
    }

    public Class<? extends CatalogFormat> getCatalogFormat(String format)
    {
        return this.catalogFormatMap.get(format);
    }

    public MessageCatalog getMessageCatalog(File configuration, Context context) throws ConfigurationException
    {
        if(!configuration.exists())
        {
            throw new ConfigurationNotFoundException("The configuration file does not exist!");
        }

        VelocityEngine velocityEngine = Misc.getVelocityEngine(configuration);
        velocityEngine.init();

        Template configTemplate = velocityEngine.getTemplate(configuration.getName());
        StringWriter stringWriter = new StringWriter();

        configTemplate.merge(context, stringWriter);

        SourceParser sourceParser;
        SourceConfig sourceConfig;

        CatalogFormat catalogFormat;
        CatalogConfig catalogConfig;

        NodeList list = this.getRootNode(stringWriter.toString()).getChildNodes();
        for(int i = 0; i < list.getLength(); i++)
        {
            Node node = list.item(i);
            if(node.getNodeName().equals("source"))
            {
                String language = node.getAttributes().getNamedItem("language").getTextContent();
                Class<? extends SourceParser> sourceParserClass = this.getSourceParser(language);
                if(sourceParserClass == null)
                {
                    throw new UnknownSourceLanguageException("Unknown source language " + language);
                }
                try
                {
                    sourceParser = sourceParserClass.newInstance();
                    sourceParser.init(this.logger);
                }
                catch (Exception e)
                {
                    throw new ConfigurationException("Could not create a SourceParser instance of " + sourceParserClass.getName());
                }

                Class<? extends SourceConfig> sourceConfigClass = sourceParser.getSourceConfigClass();
                try
                {
                    sourceConfig = sourceConfigClass.newInstance();
                    sourceConfig.parse(node);
                }
                catch (Exception e)
                {
                    throw new ConfigurationException("Could not create a SourceConfig instance of " + sourceConfigClass.getName());
                }
            }
            else if(node.getNodeName().equals("catalog"))
            {
                String format = node.getAttributes().getNamedItem("format").getTextContent();
                Class<? extends CatalogFormat> catalogFormatClass = this.getCatalogFormat(format);
                if(catalogFormatClass == null)
                {
                    throw new UnknownCatalogFormatException("Unknown catalog format " + format);
                }
                try
                {
                    catalogFormat = catalogFormatClass.newInstance();
                    catalogFormat.init(this.logger);
                }
                catch (Exception e)
                {
                    throw new ConfigurationException("Could not create a CatalogFormat instance of " + catalogFormatClass.getName());
                }

                // TODO create CatalogConfig
            }
        }

        return null;
    }

    private Node getRootNode(String xml) throws ConfigurationException
    {
        Document document = null;
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource inputSource = new InputSource(new StringReader(xml));
            document = builder.parse(inputSource);
        }
        catch (Exception e)
        {
            throw new ConfigurationException("Could not parse the configuration file", e);
        }
        assert document != null;

        NodeList list = document.getElementsByTagName("messagecatalog");
        if(list.getLength() == 0)
        {
            throw new ConfigurationException("The configuration file doesn't have a <messagecatalog> node");
        }
        else if (list.getLength() > 1)
        {
            throw new ConfigurationException("The configuration file has more than 1 <messagecatalog> node");
        }
        return list.item(0);
    }

    private void loadDefaultClasses()
    {
        this.sourceParserMap.put("java", JavaSourceParser.class);
        this.catalogFormatMap.put("gettext", PlaintextGettextCatalogFormat.class);
    }

//    private void parseConfiguration(String xml) throws ParserConfigurationException, IOException, SAXException, UnknownSourceLanguageException, IllegalAccessException, InstantiationException, UnknownCatalogFormatException
//    {
//        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//        DocumentBuilder builder = factory.newDocumentBuilder();
//        InputSource inputSource = new InputSource(new StringReader(xml));
//        Document document = builder.parse(inputSource);
//
//        NodeList list = document.getElementsByTagName("messagecatalog");
//        Node node = list.item(0);
//
//        list = node.getChildNodes();
//        for(int i = 0; i < list.getLength(); i++)
//        {
//            node = list.item(0);
//            if(node.getNodeName().equals("source"))
//            {
//                String language = node.getAttributes().getNamedItem("language").getTextContent();
//                Class<? extends SourceParser> sourceParser = this.getSourceParserClass(language);
//                if(sourceParser == null)
//                {
//                    throw new UnknownSourceLanguageException("Unknown source language " + language);
//                }
//                this.sourceParser = sourceParser.newInstance();
//                this.sourceParser.init(this.logger);
//            }
//            else if(node.getNodeName().equals("catalog"))
//            {
//                String format = node.getAttributes().getNamedItem("format").getTextContent();
//                Class<? extends CatalogFormat> catalogFormat = this.getCatalogFormatClass(null);
//                if(catalogFormat == null)
//                {
//                    throw new UnknownCatalogFormatException("Unknown catalog format " + format);
//                }
//                this.catalogFormat = catalogFormat.newInstance();
//                this.catalogFormat.init(this.logger);
//            }
//            else
//            {
//                this.logger.info("Unknown node " + node.getNodeName());
//            }
//        }
//    }
}
