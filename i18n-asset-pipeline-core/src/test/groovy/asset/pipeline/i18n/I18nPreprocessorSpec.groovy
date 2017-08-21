/*
 * I18nPreprocessorSpec.groovy
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

import asset.pipeline.AssetHelper
import asset.pipeline.GenericAssetFile
import spock.lang.Specification


class I18nPreprocessorSpec extends Specification {

    //-- Fields ---------------------------------

    I18nPreprocessor preprocessor = I18nPreprocessor.instance


    //-- Fixture methods ------------------------

    def setup() {
        def files = [
            'core.i18n': new MockAssetFile(
                '/foo/bar/core.i18n', 'abc.def\nabc.ghi\nabc.jkl'
            ),
            'plugin.i18n': new MockAssetFile(
                '/foo/bar/plugin.i18n',
                'mno.pqr\n@import core\nmno.stu\nmno.vwx'
            ),
            'a.i18n': new MockAssetFile(
                '/foo/bar/a.i18n', 'a1\n@import b\na2\na3'
            ),
            'b.i18n': new MockAssetFile(
                '/foo/bar/b.i18n', 'b1\n@import a\nb2\nb3'
            ),
        ]
        AssetHelper.metaClass.'static'.fileForUri = { String fileName ->
            println "load file ${fileName}"
            files[fileName]
        }
    }


    //-- Feature methods ------------------------

    def 'Instances are singleton'() {
        when: 'I create two instances'
        def p1 = I18nPreprocessor.instance
        def p2 = I18nPreprocessor.instance

        then: 'they are the same object'
        p1.is p2
    }

    def 'Can preprocess empty file'() {
        given: 'an asset file'
        def assetFile = new MockAssetFile('/foo/bar/mymessages.i18n', '')

        when: 'I preprocess an empty file'
        String res = I18nPreprocessor.instance.preprocess(assetFile)

        then: 'I get an empty string'
        '' == res
    }

    def 'Whitespaces are ignored'() {
        given: 'an asset file'
        def assetFile = new MockAssetFile('/foo/bar/mymessages.i18n', '  \t ')

        when: 'I preprocess a file with a blank string'
        String res = preprocessor.preprocess(assetFile)

        then: 'I get an empty string'
        '' == res
    }

    def 'Empty lines are ignored'() {
        given: 'an asset file'
        def assetFile = new MockAssetFile(
            '/foo/bar/mymessages.i18n', '    \n  \n\n\n     \n  '
        )

        when: 'I preprocess a file with blank strings'
        String res = preprocessor.preprocess(assetFile)

        then: 'I get an empty string'
        '' == res
    }

    def 'Comments are ignored'() {
        given: 'an asset file'
        def assetFile = new MockAssetFile(
            '/foo/bar/mymessages.i18n', '''
# This is a comment.

# Another comment.
        # also a comment

##### comment

'''
        )

        when: 'I preprocess a file with comments'
        String res = preprocessor.preprocess(assetFile)

        then: 'I get an empty string'
        '' == res
    }

    def 'Items are processed'() {
        given: 'an asset file'
        def assetFile = new MockAssetFile(
            '/foo/bar/mymessages.i18n',
            '    \nfoo.bar\n# Comment\n\nfoo.foo     \n  '
        )

        when: 'I preprocess a file with items'
        String res = preprocessor.preprocess(assetFile)

        then: 'I get the items'
        'foo.bar\nfoo.foo\n' == res
    }

    def 'Imports are resolved'() {
        given: 'an asset file'
        def assetFile = new MockAssetFile(
            '/foo/bar/mymessages.i18n',
            '# Imports\n@import core.i18n\n\nfoo.bar\nfoo.foo'
        )

        when: 'I preprocess the file'
        String res = preprocessor.preprocess(assetFile)

        then: 'I get the items'
        'abc.def\nabc.ghi\nabc.jkl\nfoo.bar\nfoo.foo\n' == res
    }

    def 'Imports are resolved at right place'() {
        given: 'an asset file'
        def assetFile = new MockAssetFile(
            '/foo/bar/mymessages.i18n',
            'foo.bar\n# Imports\n@import core.i18n\n\nfoo.foo'
        )

        when: 'I preprocess the file'
        String res = preprocessor.preprocess(assetFile)

        then: 'I get the items'
        'foo.bar\nabc.def\nabc.ghi\nabc.jkl\nfoo.foo\n' == res
    }

    def 'Imports are resolved without file extension'() {
        given: 'an asset file'
        def assetFile = new MockAssetFile(
            '/foo/bar/mymessages.i18n',
            '# Imports\n@import core\n\nfoo.bar\nfoo.foo'
        )

        when: 'I preprocess the file'
        String res = preprocessor.preprocess(assetFile)

        then: 'I get the items'
        'abc.def\nabc.ghi\nabc.jkl\nfoo.bar\nfoo.foo\n' == res
    }

    def 'Multiple imports are resolved only once'() {
        given: 'an asset file'
        def assetFile = new MockAssetFile(
            '/foo/bar/mymessages.i18n',
            '''
# Imports
@import core.i18n

foo.bar

@import core.i18n
foo.foo
'''
        )

        when: 'I preprocess the file'
        String res = preprocessor.preprocess(assetFile)

        then: 'I get the items'
        'abc.def\nabc.ghi\nabc.jkl\nfoo.bar\nfoo.foo\n' == res
    }

    def 'Imports dependencies are handled correctly'() {
        given: 'an asset file'
        def assetFile = new MockAssetFile(
            '/foo/bar/mymessages.i18n',
            '# Imports\n@import plugin.i18n\n\nfoo.bar\nfoo.foo'
        )

        when: 'I preprocess the file'
        String res = preprocessor.preprocess(assetFile)

        then: 'I get the items'
        'mno.pqr\nabc.def\nabc.ghi\nabc.jkl\nmno.stu\nmno.vwx\nfoo.bar\nfoo.foo\n' == res
    }

    def 'Circular imports dependencies are handled correctly'() {
        given: 'an asset file'
        def a = AssetHelper.fileForUri('a.i18n')
        def b = AssetHelper.fileForUri('b.i18n')

        when: 'I preprocess the file'
        String res = preprocessor.preprocess(a)

        then: 'I get the items'
        'a1\nb1\nb2\nb3\na2\na3\n' == res

        when: 'I preprocess the other file'
        res = preprocessor.preprocess(b)

        then: 'I get the items'
        'b1\na1\na2\na3\nb2\nb3\n' == res
    }

    def 'Imports are resolved independently'() {
        given: 'an asset file'
        def assetFile = new MockAssetFile(
            '/foo/bar/mymessages.i18n',
            '# Imports\n@import core.i18n\n\nfoo.bar\nfoo.foo'
        )

        when: 'I preprocess the file'
        String res = preprocessor.preprocess(assetFile)

        then: 'I get the items'
        'abc.def\nabc.ghi\nabc.jkl\nfoo.bar\nfoo.foo\n' == res

        when: 'I preprocess the file again'
        res = preprocessor.preprocess(assetFile)

        then: 'I get the same result'
        'abc.def\nabc.ghi\nabc.jkl\nfoo.bar\nfoo.foo\n' == res
    }
}


class MockAssetFile extends GenericAssetFile {

    //-- Instance variables ---------------------

    String content


    //-- Constructors ---------------------------

    MockAssetFile(String path, String content) {
        this.path = path
        this.content = content
    }


    //-- Public methods -------------------------

    @Override
    Byte [] getBytes() {
        content.bytes
    }

    @Override
    InputStream getInputStream() {
        new ByteArrayInputStream(bytes)
    }
}
