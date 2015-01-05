/*
 * I18nTagLib.groovy
 *
 * Copyright (c) 2014-2015, Daniel Ellermann
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
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.springframework.core.io.Resource


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
        def l = attrs.remove('locale') ?: ''
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
        if (log.debugEnabled) {
            log.debug "Retrieving i18n messages for locale ${locale}…"
        }

        String name = attrs.remove('name') ?: 'messages'
        String [] parts = locale.split('_')

        String src = null
        for (int i = parts.length - 1; i >= 0 && !src; --i) {
            StringBuilder buf = new StringBuilder(name)
            for (int j = 0; j <= i; j++) {
                buf << '_' << parts[j]
            }
            String s = buf.toString()
            if (log.debugEnabled) {
                log.debug "Trying to find asset ${s}…"
            }

            /*
             * XXX This is a somewhat dirty hack.  When running in WAR file a
             * filter (asset.pipeline.AssetPipelineFilter) looks for a resource
             * in folder "assets".  So we try this first, and, if not found, we
             * look in "grails-app/assets" via fileForUri().
             */
            Resource res =
                grailsApplication.mainContext.getResource("assets/${s}.js")
            if (res.exists()) {
                src = s
                break
            } else {
                AssetFile f =
                    AssetHelper.fileForUri(s, 'application/javascript')
                if (f != null) {
                    src = s
                    break
                }
            }
        }
        if (log.debugEnabled) {
            log.debug "Found asset ${src ?: name}"
        }

        out << asset.javascript(src: src ?: name)
    }
}

