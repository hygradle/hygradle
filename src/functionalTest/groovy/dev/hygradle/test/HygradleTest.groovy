package dev.hygradle.test

import spock.lang.Specification

class HygradleTest extends Specification {
    def "test"() {
        var value = 2

        expect:
        value > 1
    }

}
