package au.org.ala.images

import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Specification

class ImagesTagLibSpec extends Specification implements TagLibUnitTest<ImagesTagLib> {

    void setup() {
        tagLib.sanitiserService = new SanitiserService()
    }

    void 'test sanitised markup is created'() {
        when:
        def expected = '''<a href="http://example.org" rel="nofollow">Link</a>'''
        def output = applyTemplate('''<img:sanitise value="${'<a href="http://example.org" onclick=alert(1)>Link</a>'}"/>''')

        then:
        output == expected
    }

    void 'sanitiseString length, image and key parameters are optional'() {
        given:
        def expected = '''<a href="http://example.org" rel="nofollow">Link</a>'''

        expect:
        tagLib.sanitise(value: '<a href="http://example.org" onclick=alert(1)>Link</a>') == expected
    }

    void 'sanitiseString length parameter is applied'() {
        given:
        def expected = '''<a href="http://example.org" rel="nofollow">Link</a>'''

        expect:
        tagLib.sanitiseString(value: '<a href="http://example.org" onclick=alert(1)>Link</a>') == expected
    }

    void 'sanitise length, image and key parameters are optional'() {
        given:
        def expected = '''<a href="http://example.org" rel="nofollow">Link</a>'''

        expect:
        tagLib.sanitise(value: '<a href="http://example.org" onclick=alert(1)>Link</a>') == expected
    }

    void 'sanitise length parameter is applied'() {
        given:
        def expected = '''<a href="http://example.org" rel="nofollow">Some...</a>'''

        expect:
        tagLib.sanitise(value: '<a href="http://example.org" onclick=alert(1)>Some Text</a>', length: 7) == expected
    }

}
