/*
 * I18nAssetFile.groovy
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

import asset.pipeline.AbstractAssetFile
import asset.pipeline.AssetHelper
import asset.pipeline.CacheManager


/**
 * The class {@code I18nAssetFile} represents an asset file which converts code
 * keys to localized messages.
 *
 * @author  Daniel Ellermann
 * @version 0.9.0
 */
class I18nAssetFile extends AbstractAssetFile {

    //-- Class variables ------------------------

    static final contentType = [
        'application/javascript', 'application/x-javascript', 'text/javascript'
    ]
    static extensions = ['i18n']
    static final String compiledExtension = 'js'
    static processors = [I18nProcessor]


    //-- Public methods -------------------------

    @Override
    String directiveForLine(String line) {
        line.find(/#=(.*)/) { fullMatch, directive -> directive }
    }

    @Override
    String processedStream(precompiler) {
        def skipCache = precompiler ?: (!processors || processors.size() == 0)

        String fileText
        if (baseFile?.encoding || encoding) {
            fileText = file?.getText(
                baseFile?.encoding ? baseFile.encoding : encoding
            )
        } else {
            fileText = file?.text
        }

        fileText = I18nPreprocessor.instance.preprocess(file, fileText)

        def md5 = AssetHelper.getByteDigest(fileText.bytes)
        if (!skipCache) {
            def cache = CacheManager.findCache(
                file.canonicalPath, md5, baseFile?.file?.canonicalPath
            )
            if (cache) {
                return cache
            }
        }

        for (processor in processors) {
            def processInstance = processor.newInstance(precompiler)
            fileText = processInstance.process(fileText, this)
        }

        if (!skipCache) {
            CacheManager.createCache(
                file.canonicalPath, md5, fileText,
                baseFile?.file?.canonicalPath
            )
        }

        fileText
    }
}
