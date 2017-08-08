/*
 * I18nProcessor.groovy
 *
 * Copyright (c) 2014-2016, Daniel Ellermann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package asset.pipeline.i18n

import asset.pipeline.AbstractProcessor
import asset.pipeline.AssetCompiler
import asset.pipeline.AssetFile
import grails.io.IOUtils
import grails.util.Holders
import groovy.transform.CompileStatic
import org.grails.plugins.BinaryGrailsPlugin
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.ResourcePatternResolver


import org.springframework.core.io.support.ResourcePatternUtils

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * The class {@code I18nProcessor} represents an asset processor which converts
 * i18n file consisting of code keys to localized messages and builds a
 * JavaScript file containing the function {@code $L} to obtain localized
 * strings on client side.
 * <p>
 * I18n files must obey the following rules:
 * <ul>
 *   <li>The file name (without extension) must end with the locale
 *   specification, e. g. {@code messages_de.i18n} or
 *   {@code msg_en_UK.i18n}.</li>
 *   <li>The files are line based.</li>
 *   <li>All lines are trimmed (that is, whitespaces are removed from beginning
 *   and end of lines.</li>
 *   <li>Empty lines and lines starting with a hash {@code #} (comment lines)
 *   are ignored.</li>
 *   <li>Lines starting with <code>@import <i>path</i></code> are replaced by
 *   the content of the file with path <code><i>path</i></code>.  The suffix
 *   {@code .i18n} at path is optional and is appended automatically.</li>
 *   <li>All other lines are treated as code keys which will be looked up in
 *   Grails message resources for the locale specified in the file.</li>
 * </ul>
 *
 * @author  Daniel Ellermann
 * @author  David Estes
 * @version 3.0
 */
@CompileStatic
class I18nProcessor extends AbstractProcessor {

    //-- Constants ------------------------------

    protected static final String PROPERTIES_SUFFIX = '.properties'
    protected static final String XML_SUFFIX = '.xml'


    //-- Fields ---------------------------------

    ResourceLoader resourceLoader = new DefaultResourceLoader()


    //-- Constructors ---------------------------

    /**
     * Creates a new i18n resource processor within the given asset
     * pre-compiler.
     *
     * @param precompiler   the given asset pre-compiler
     */
    I18nProcessor(AssetCompiler precompiler) {
        super(precompiler)
    }


    //-- Public methods -------------------------

    @Override
    String process(String inputText, AssetFile assetFile) {
        Matcher m = assetFile.name =~ /._(\w+)\.i18n$/
        StringBuilder buf = new StringBuilder('messages')
        String locale = m ? m.group(1) : null
        if (locale) buf << '_' << locale
        Properties props
        Set<String> setPattern = [] as Set
        inputText.toString().eachLine { String line ->
            if (line.trim()) {
                setPattern << line.trim()
            }
        }
        if (assetFile.encoding != null) {
            props = loadMessages(setPattern, buf.toString(), locale, assetFile.encoding)
        } else {
            props = loadMessages(setPattern, buf.toString(), locale)
        }
        // At this point, inputText has been pre-processed (I18nPreprocessor).
        Map<String, String> messages = [:]
        props.stringPropertyNames().each {
            messages.put(it, props.getProperty(it, it))
        }
        compileJavaScript messages
    }

    //-- Non-public methods ---------------------

    /**
     * Compiles JavaScript code from the given localized messages.
     *
     * @param messages  the given messages
     * @return          the compiled JavaScript code
     */
    private String compileJavaScript(Map<String, String> messages) {
        StringBuilder buf = new StringBuilder()
        int i = 0
        for (Map.Entry<String, String> entry in messages.entrySet()) {
            if (i++ > 0) {
                buf << ',\n'
            }
            String value = entry.value
                    .replace('\\', '\\\\')
                    .replace('\n', '\\n')
                    .replace('"', '\\"')
            buf << '        "' << entry.key << '": "' << value << '"'
        }
        return getJavaScriptCode(buf.toString())
    }

    /**
     * Return the javascript function that will create $L
     * The function can use parameters to pass to messages
     * Eg :  $L('code', 21)
     * @param messages the javascript object containing the messages in a key value pair structure
     * @return String containing the complete javascript function
     */
    String getJavaScriptCode(String messages) {
        StringBuilder buf = new StringBuilder('''(function (win) {
            if (win.i18n_messages) {
                var tmpMsg = {
                                ''')
                    buf << messages
                    buf << '''
                }
                for (var attrname in tmpMsg) { win.i18n_messages[attrname] = tmpMsg[attrname]; }
            }
            else {
                win.i18n_messages = {
                '''
                buf << messages
                buf << '''
                    };
            }
            var messages = win.i18n_messages;
            var stringFormat = function(format, prevArgs) {
                var args = Array.prototype.slice.call(prevArgs, 1);
                return format.replace(/{(\\d+)}/g, function(match, number) { 
                  return typeof args[number] != 'undefined\'
                    ? args[number] 
                    : match
                  ;
                });
             };
            win.$L = function (code) {
                var message = messages[code];
                if(message === undefined) {
                    return "[" + code + "]";
                } else {
                    return stringFormat(message, arguments);
                }
            };
            win.msg = function(code) {
               var message = messages[code];
                if(message === undefined) {
                    return "[" + code + "]";
                } else {
                    return stringFormat(message, arguments);
                }
            };
        }(this));
        '''

        buf.toString()
    }

    /**
     * Loads the message resources from the given file.
     *
     * @param fileName                  the given base file name
     * @param locale                    the locale of the properties file
     * @param                           encoding the charset of the properties file
     * @return                          the read message resources
     * @throws FileNotFoundException    if no resource with the required
     *                                  localized messages exists
     */
    private Properties loadMessages(Set<String> listPattern, String fileName, String locale, String encoding = 'utf-8') {
        Set<Resource> listRes = locateResource(fileName)
        Properties props = new Properties()
        for (resource in listRes) {
            String propertiesString = IOUtils.toString(resource.inputStream, encoding)
            props.load(new StringReader(propertiesString))
        }
        if (Holders.pluginManager?.allPlugins != null) {
            for (plugin in Holders.pluginManager.allPlugins) {
                if (plugin instanceof BinaryGrailsPlugin) {
                    Locale loc = locale ? new Locale(locale) : new Locale('en')
                    Properties propPlugin = ((BinaryGrailsPlugin) plugin).getProperties(loc)
                    if (propPlugin != null) {
                        props.putAll(propPlugin)
                    }
                }
            }
        }
        if (props.isEmpty()) {
            throw new FileNotFoundException('File '+fileName+' has not been found')
        }

        //Filter messages based on the list of regex pattern
        Properties filteredProperties = new Properties()

        //Checks every pattern against all the properties
        for (String pattern in listPattern) {
            boolean hasBeenMatched = false

            for (String key in props.keySet()) {
                Matcher m = key =~ pattern
                if (m.groupCount() == 0) {
                    m = key =~ '^' + pattern + '(.*)'
                }
                if (m.matches()) {
                    //Matches! We only keep the part in the last group for our JS file
                    String keyWithoutPrefix = m.group(1)
                    hasBeenMatched = true
                    if (keyWithoutPrefix) {
                        filteredProperties.remove(key)
                        filteredProperties.put(keyWithoutPrefix, props.getProperty(key))
                    }
                    else {
                        //No prefix, we just copy the property
                        filteredProperties.put(key, props.getProperty(key))
                    }
                }
            }
            //Not matched, we include it in the properties as it is
            if (!hasBeenMatched) {
                filteredProperties.put(pattern, pattern)
            }
        }
        filteredProperties
    }

    /**
     * Locates the resource containing the localized messages.  The method looks
     * in the following places:
     * <ul>
     *   <li>in classpath with extension {@code .properties}</li>
     *   <li>in classpath with extension {@code .xml}</li>
     *   <li>in file system in folder {@code grails-app/i18n} with extension
     *   {@code .properties}</li>
     *   <li>in file system in folder {@code grails-app/i18n} with extension
     *   {@code .xml}</li>
     * </ul>
     *
     * @param fileName                  the given base file name
     * @return                          the resource containing the messages
     * @throws FileNotFoundException    if no resource with the required
     *                                  localized messages exists
     */
    private Set<Resource> locateResource(String fileName) {
        Set<Resource> resourceList = [] as Set
        Resource resource =
                resourceLoader.getResource("classpath*:" + fileName + PROPERTIES_SUFFIX)
        if (resource?.exists()) {
            resourceList << resource
        }
        Resource resource2 = resourceLoader.getResource(fileName + XML_SUFFIX)
        if (resource2?.exists()) {
            resourceList << resource2
        }
        Resource resource3 = resourceLoader.getResource(
                "file:grails-app/i18n/${fileName}${PROPERTIES_SUFFIX}"
        )
        if (resource3?.exists()) {
            resourceList << resource3
        }
        Resource resource4 = resourceLoader.getResource(
                "file:grails-app/i18n/${fileName}${XML_SUFFIX}"
        )
        if (resource4?.exists()) {
            resourceList << resource4
        }
        Resource resource5 = resourceLoader.getResource("${fileName}${PROPERTIES_SUFFIX}")
        if (resource5?.exists()) {
            resourceList << resource5
        }
        String paths = System.getProperty("com.i18n-asset-pipeline.pluginRuntimePath")
        if (paths) {
            String[] runtimePath = paths.split(',')
            FileSystemResourceLoader fileSystemResourceLoader = new FileSystemResourceLoader()
            ResourcePatternResolver patternResolver = ResourcePatternUtils.getResourcePatternResolver(fileSystemResourceLoader)
            for (path in runtimePath) {
                if (!path) {
                    continue
                }
                Resource[] found = patternResolver.getResources('jar:file:' + path + '!/' + fileName + PROPERTIES_SUFFIX)
                for (res in found) {
                    if (res.exists()) {
                        resourceList.add(res)
                        println("Found file ${fileName}${PROPERTIES_SUFFIX} for path $path : ${found.size()}")
                    }
                }
            }
        }
        return resourceList
    }
}
