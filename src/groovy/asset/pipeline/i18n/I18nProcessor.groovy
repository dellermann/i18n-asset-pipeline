/*
 * I18nProcessor.groovy
 *
 * Copyright (c) 2014, Daniel Ellermann
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
 * @version 1.0
 */
class I18nProcessor extends AbstractProcessor {

    //-- Constants ------------------------------

    protected static final String PROPERTIES_SUFFIX = '.properties'
    protected static final String XML_SUFFIX = '.xml'


    //-- Instance variables ---------------------

    protected ResourceLoader resourceLoader = new DefaultResourceLoader()


    //-- Constructors ---------------------------

    /**
     * Creates a new i18n resource processor within the given asset
     * pre-compiler.
     *
     * @param precompiler   the given asset pre-compiler
     */
    I18nProcessor(AssetCompiler precompiler){
        super(precompiler)
    }


    //-- Public methods -------------------------

    @Override
    @CompileStatic
    String process(String inputText, AssetFile assetFile) {
        AssetFile f = (AssetFile) assetFile
        Matcher m = f.name =~ /._(\w+)\.i18n$/
        StringBuilder buf = new StringBuilder('grails-app/i18n/messages')
        if (m) buf << '_' << m.group(1)

        Resource res = locateResource(buf.toString())
        Properties props = new Properties()
        props.load res.inputStream

        // At this point, inputText has been pre-processed (I18nPreprocessor).
        Map<String, String> messages = [: ]
        inputText.toString()
            .eachLine {
                if (it != '') messages.put it, props.getProperty(it, it)
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
    @CompileStatic
    protected String compileJavaScript(Map<String, String> messages) {
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
     * Locates the resource containing the localized messages.
     *
     * @param fileName                  the given base file name
     * @return                          the resource containing the messages
     * @throws FileNotFoundException    if no resource with the required
     *                                  localized messages exists
     */
    @CompileStatic
    protected Resource locateResource(String fileName) {
        Resource resource =
            resourceLoader.getResource(fileName + PROPERTIES_SUFFIX)
        if (!resource.exists()) {
            resource = resourceLoader.getResource(fileName + XML_SUFFIX)
        }
        if (!resource.exists()) {
            throw new FileNotFoundException(
                "Cannot find i18n messages file ${fileName}."
            )
        }

        resource
    }
}
