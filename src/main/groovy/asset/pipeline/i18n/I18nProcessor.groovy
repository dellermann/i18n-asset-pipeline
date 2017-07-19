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
import groovy.transform.CompileStatic
import org.springframework.core.io.FileSystemResourceLoader
import org.springframework.core.io.support.ResourcePatternResolver
import org.springframework.core.io.support.ResourcePatternUtils

import java.util.regex.Matcher
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader


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
 * {@code msg_en_UK.i18n}.</li>
 *   <li>The files are line based.</li>
 *   <li>All lines are trimmed (that is, whitespaces are removed from beginning
 *   and end of lines.</li>
 *   <li>Empty lines and lines starting with a hash {@code #} (comment lines)
 *   are ignored.</li>
 *   <li>Lines starting with <code>@import <i>path</i></code> are replaced by
 *   the content of the file with path <code><i>path</i></code>.  The suffix
 * {@code .i18n} at path is optional and is appended automatically.</li>
 *   <li>All other lines are treated as code keys which will be looked up in
 *   Grails message resources for the locale specified in the file.</li>
 * </ul>
 *
 * @author Daniel Ellermann
 * @author David Estes
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
     * @param precompiler the given asset pre-compiler
     */
    I18nProcessor(AssetCompiler precompiler) {
        super(precompiler)
    }

    //-- Public methods -------------------------

    @Override
    String process(String inputText, AssetFile assetFile) {
        Matcher m = assetFile.name =~ /._(\w+)\.i18n$/
        StringBuilder buf = new StringBuilder('messages')
        if (m) buf << '_' << m.group(1)
        Properties props = loadMessages(buf.toString())
        println('-------Properties content-----------')
        props.list(System.out)

        println("Input text : $inputText")
        // At this point, inputText has been pre-processed (I18nPreprocessor).
        Map<String, String> messages = [:]
        inputText.toString()
                .eachLine { String line ->
            if (line != '') {
                messages.put line, props.getProperty(line, line)
            }
        }
        println('Messages for JS : '+messages.toString())
        compileJavaScript messages
    }

    //-- Non-public methods ---------------------

    /**
     * Compiles JavaScript code from the given localized messages.
     *
     * @param messages the given messages
     * @return the compiled JavaScript code
     */
    private String compileJavaScript(Map<String, String> messages) {
        StringBuilder buf = new StringBuilder('''(function (win) {
    var messages = {
''')
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
        buf << '''
    }

    win.$L = function (code) {
        return messages[code];
    }
}(this));
'''
        buf.toString()
    }

    /**
     * Loads the message resources from the given file.
     *
     * @param fileName the given base file name
     * @return the read message resources
     * @throws FileNotFoundException    if no resource with the required
     *                                  localized messages exists
     */
    private Properties loadMessages(String fileName) {
        List<Resource> listRes = locateResource(fileName)
        Properties props = null
        for (resource in listRes) {
            if (!props) {
                props = new Properties()
            }
            else {
                props = new Properties(props)
            }
            props.load(resource.inputStream)
        }
        props
    }

    /**
     * Locates the resources containing the localized messages.  The method looks
     * in the following places:
     * <ul>
     *   <li>in classpath with extension {@code .properties}</li>
     *   <li>in classpath with extension {@code .xml}</li>
     *   <li>in file system in folder {@code grails-app/i18n} with extension
     * {@code .properties}</li>
     *   <li>in file system in folder {@code grails-app/i18n} with extension
     * {@code .xml}</li>
     *  <li>in a list of jar supplied in comma separated string in the com.i18n-asset-pipeline.pluginRuntimePath
     *  system env property<li>
     * </ul>
     *
     * @param fileName the given base file name
     * @return the resource containing the messages
     * @throws FileNotFoundException    if no resource with the required
     *                                  localized messages exists
     */
    private List<Resource> locateResource(String fileName) {
        List<Resource> resourceList = []
        Resource resource =
                resourceLoader.getResource("classpath*:"+fileName + PROPERTIES_SUFFIX)
        if (resource.exists()) {
            resourceList << resource
        }
        Resource resource2 = resourceLoader.getResource(fileName + XML_SUFFIX)
        if (resource2.exists()) {
            resourceList << resource2
        }
        Resource resource3 = resourceLoader.getResource(
                "file:grails-app/i18n/${fileName}${PROPERTIES_SUFFIX}"
        )
        if (resource3.exists()) {
            resourceList << resource3
        }
        Resource resource4 = resourceLoader.getResource(
                "file:grails-app/i18n/${fileName}${XML_SUFFIX}"
        )
        if (resource4.exists()) {
            resourceList << resource4
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
                        println('Found for path ' + path + ' : ' + found.size())
                    }
                }
            }
        }
        return resourceList
    }
}
