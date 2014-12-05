/*
 * I18nTagLib.groovy
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


package asset.pipeline

import org.codehaus.groovy.grails.commons.GrailsApplication


/**
 * Class {@code I18nTagLib} contains tags that help loading client-side i18n
 * files.
 *
 * @author  Daniel Ellermann
 * @version 1.0
 */
class I18nTagLib {

    //-- Class variables ------------------------

    static namespace = "asset"


    //-- Instance variables ---------------------

    GrailsApplication grailsApplication


    //-- Public methods -------------------------

    /**
     * Includes a JavaScript asset that provides client-side i18n for the given
     * locale.
     *
     * @attr locale the given locale
     * @attr [name] the name of the i18n file without extension; defaults to "messages"
     */
    def i18n = { attrs ->
        def l = attrs.remove('locale')
        String locale = ''
        if (l instanceof Locale) {
            locale = l.toString()
        } else if (l instanceof CharSequence) {
            locale = l
        } else {
            if (log.warnEnabled) {
                log.warn "Unknown type ${l.class.name} for attribute 'locale'; use default locale."
            }
        }
        locale = locale.replace('-', '_')

        String name = attrs.remove('name') ?: 'messages'
        String [] parts = locale.split('_')

        String src = null
        for (int i = parts.length - 1; i >= 0 && !src; --i) {
            StringBuilder buf = new StringBuilder(name)
            for (int j = 0; j <= i; j++) {
                buf << '_' << parts[j]
            }
            String s = buf.toString()
            def f = AssetHelper.fileForUri(
                s.toString(), 'application/javascript'
            )
            if (f != null) {
                src = s
            }
        }

        out << asset.javascript(src: src ?: name)
    }
}
