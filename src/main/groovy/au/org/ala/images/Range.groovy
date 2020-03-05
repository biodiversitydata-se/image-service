package au.org.ala.images

import groovy.transform.Canonical
import org.apache.commons.io.input.BoundedInputStream

import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * Represents an HTTP byte range request.
 */
@Canonical(includes = ['_totalLength', '_start', '_end', '_suffixLength'])
class Range {

    private static final String byteRangeSetRegex = '(\\s*((?<byteRangeSpec>(?<firstBytePos>\\d+)-(?<lastBytePos>\\d+)?)|(?<suffixByteRangeSpec>-(?<suffixLength>\\d+)))\\s*(,|$))'
    private static final String byteRangesSpecifierRegex = 'bytes=(?<byteRangeSet>' + byteRangeSetRegex + '+)'
    private static final Pattern byteRangeSetPattern = Pattern.compile(byteRangeSetRegex)
    private static final Pattern byteRangesSpecifierPattern = Pattern.compile(byteRangesSpecifierRegex)

    private static final InvalidRangeHeaderException INVALID_RANGE_HEADER_EXCEPTION = new InvalidRangeHeaderException("Invalid range header")


    private Long _totalLength
    private Long _start // inclusive
    private Long _end // inclusive
    private Long _suffixLength

    long start() {
        if (_start != null) {
            _start
        } else if (_suffixLength != null) {
            _totalLength - _suffixLength
        } else {
            0
        }
    }

    long end() {
        if (_end != null && _end < (_totalLength - 1)) {
            _end
        } else {
            _totalLength - 1
        }
    }

    boolean validate() {
        boolean valid = true
        if (_start != null) {
            valid &= _start >= 0
        }
        if (_end != null) {
            valid &= _end >= 0
        }
        if (_suffixLength != null) {
            valid &= _suffixLength >= 0
            valid &= _start == null && _end == null
        }
        if (_start != null && _end != null) {
            valid &= _start <= _end
        }
        return valid
    }

    long length() {
        if (_end != null && _start != null && _end < _totalLength) {
            _end - _start + 1
        } else if (_start != null) {
            _totalLength - _start
        } else if (_suffixLength != null) {
            _suffixLength
        } else {
            _totalLength
        }
    }

    /**
     * Whether this range request contains no defined range
     * @return
     */
    boolean isEmpty() {
        (_start == null && _suffixLength == null)
    }

    String contentRangeHeader() {
//        content-range: bytes 5793340-5793343/5793344
        if (!empty) {
            "bytes ${start()}-${end()}/$_totalLength"
        }
        else {
            null
        }
    }

    static List<Range> decodeRange(String rangeHeader, long totalLength) {

        if (!rangeHeader) return [emptyRange(totalLength)]

        List<Range> ranges = new ArrayList<>()
        Matcher byteRangesSpecifierMatcher = byteRangesSpecifierPattern.matcher(rangeHeader?.trim())
        if (byteRangesSpecifierMatcher.matches()) {
            String byteRangeSet = byteRangesSpecifierMatcher.group("byteRangeSet")
            Matcher byteRangeSetMatcher = byteRangeSetPattern.matcher(byteRangeSet)
            while (byteRangeSetMatcher.find()) {
                Range range = new Range()
                range._totalLength = totalLength
                if (byteRangeSetMatcher.group("byteRangeSpec") != null) {
                    String start = byteRangeSetMatcher.group("firstBytePos")
                    String end = byteRangeSetMatcher.group("lastBytePos")
                    range._start = Long.valueOf(start)
                    range._end = end == null ? null : Long.valueOf(end)
                } else if (byteRangeSetMatcher.group("suffixByteRangeSpec") != null) {
                    range._suffixLength = Long.valueOf(byteRangeSetMatcher.group("suffixLength"))
                } else {
                    throw INVALID_RANGE_HEADER_EXCEPTION
                }
                if (!range.validate()) {
                    throw INVALID_RANGE_HEADER_EXCEPTION
                }
                ranges.add(range)
            }
        } else {
            throw INVALID_RANGE_HEADER_EXCEPTION
        }
        return ranges
    }

    static Range emptyRange(long totalLength) {
        return new Range(_totalLength: totalLength)
    }

    InputStream wrapInputStream(InputStream inputStream) {
        InputStream is = inputStream
        if (!this.empty) {
            def start = this.start()
            // TODO does this need to be inside a use block?
            inputStream.skip(start)
            def end = this.end()
            if (end < _totalLength - 1) {
                is = new BoundedInputStream(is, this.length())
            }
        }
        return is
        def s = $/\u003cp\u003e\u003ca href=\\\"http://en.wikipedia.org/wiki/Raymond_Hoser\\\" title=\\\"Raymond Hoser\\\"\u003eRaymond Hoser\u003c/a\u003e described two subspecies in 2003: \u003ci\u003ePseudechis porphyriacus eipperi\u003c/i\u003e the Atherton Tableland and surrounds in North-east Queensland, which he noted was smaller, rarely attaining 2 m (7 ft) and had a white or pale pink rather than red belly, and \u003ci\u003ePseudechis porphyriacus rentoni\u003c/i\u003e from southeastern South Australia, which has a variable-coloured (often orange or even blueish-tinged) belly. He added that both were disjunct from the main red-bellied black snake population, and as the distinguishing traits of \u003ci\u003eP. porphyriacus rentoni\u003c/i\u003e were not consistent then location was the most reliable way of identifying it.\u003csup\u003e\u003ca href=\\\"#cite_note-hoser2003-14\\\"\u003e[14]\u003c/a\u003e\u003c/sup\u003e These subspecies have not been recognized by other authors.\u003csup\u003e\u003ca href=\\\"#cite_note-AFD-1\\\"\u003e[1]\u003c/a\u003e\u003c/sup\u003e Hoser has been criticized by \u003ca href=\\\"http://en.wikipedia.org/w/index.php?title=Hinrich_Kaiser\u0026amp;action=edit\u0026amp;redlink=1\\\" title=\\\"Hinrich Kaiser (page does not exist)\\\"\u003eHinrich Kaiser\u003c/a\u003e and colleagues for identifying some taxa on location alone.\u003csup\u003e\u003ca href=\\\\"#cite_note-15\\\"\u003e[15]\u003c/a\u003e;--href=\\\"http://en.wikipedia.org/wiki/Raymond_Hoser\\\";--/$
    }

    static final class InvalidRangeHeaderException extends RuntimeException {
        InvalidRangeHeaderException(String message) {
            super(message)
        }

        @Override
        Throwable fillInStackTrace() {
            return this
        }
    }

    @Override
    String toString() {
        return "Range(${start()}..${end()})"
    }
}
