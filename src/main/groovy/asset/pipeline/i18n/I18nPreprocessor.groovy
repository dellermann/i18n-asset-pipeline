/*
 * I18nPreprocessor.groovy
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

import asset.pipeline.AssetFile
import asset.pipeline.AssetHelper
import groovy.transform.TypeChecked
import java.util.regex.Matcher
import java.util.regex.Pattern


/**
 * The class {@code I18nPreprocessor} represents a pre-processor for i18n files
 * which are used in the asset pipeline.
 * <p>
 * Implementation notes: this class must be compiled with {@code @TypeChecked}
 * not {@code CompileStatic} because the unit test {@code I18nPreprocessorSpec}
 * mocks the static method {@code AssetFile.fileForUri} with meta class.
 *
 * @author  Daniel Ellermann
 * @author  David Estes
 * @version 3.0
 */
@TypeChecked
class I18nPreprocessor {

    //-- Constants ------------------------------

    protected static final Pattern REGEX_IGNORE = ~/^\s*(?:#.*)?$/
    protected static final Pattern REGEX_IMPORT = ~/^\s*@import\s+(.+)$/


    //-- Constructors ---------------------------

    protected I18nPreprocessor() {}


    //-- Public methods -------------------------

    /**
     * Gets the one and only factory instance.
     *
     * @return  the singleton instance of this factory
     */
    static I18nPreprocessor getInstance() {
        InstanceHolder.INSTANCE
    }

    /**
     * Pre-processes the given i18n file by removing empty lines and comment
     * lines and resolving all imports.
     *
     * @param file  the given i18n file
     * @param input the content of the i18n file
     * @return      the pre-processed content
     */
    String preprocess(AssetFile file, String input = file?.inputStream?.text) {
        if (!input) {
            return ''
        }

        Set<AssetFile> fileHistory = new HashSet<>()
        fileHistory << file

        doPreprocess input, fileHistory
    }


    //-- Non-public methods ---------------------

    /**
     * Pre-processes an i18n file by removing empty lines and comment lines and
     * resolving all imports.
     *
     * @param input         the content of the i18n file
     * @param fileHistory   the history of all import files that have been
     *                      processed already; this is needed to handle
     *                      circular dependencies
     * @return              the pre-processed content
     */
    private String doPreprocess(String input, Set<AssetFile> fileHistory) {
        StringBuffer buf = new StringBuffer(input.length())
        input.eachLine { String line ->
            line = line.trim()
            if (line ==~ REGEX_IGNORE) return

            Matcher m = line =~ REGEX_IMPORT
            if (m) {
                line = resolveImport(m.group(1).trim(), fileHistory)
                if (!line) return
                line = line.trim()
            }
            buf << line << '\n'
        }

        buf.toString()
    }

    /**
     * Loads the import file with the file name and processes its content.
     *
     * @param fileName      the name of the import file
     * @param fileHistory   the history of all import files that have been
     *                      processed already; this is needed to handle
     *                      circular dependencies
     * @return              the pre-processed content of the import file
     */
    private String resolveImport(String fileName, Set<AssetFile> fileHistory) {
        if (!fileName.endsWith('.i18n')) {
            fileName += '.i18n'
        }

        AssetFile importFile = AssetHelper.fileForUri(fileName)
        if (importFile == null || importFile in fileHistory) {
            return ''
        }

        fileHistory << importFile
        doPreprocess importFile.inputStream.text, fileHistory
    }


    //-- Inner classes --------------------------

    private static class InstanceHolder {

        //-- Constants --------------------------

        public static final I18nPreprocessor INSTANCE =
            new I18nPreprocessor()
    }
}
