package org.apache.fop.fonts.base14;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.fop.fonts.Base14Font;
import org.apache.fop.fonts.CodePointMapping;
import org.apache.fop.fonts.FontType;
import org.apache.fop.fonts.Typeface;

public class TimesBoldItalic extends Base14Font {
    private final static String fontName = "Times-BoldItalic";
    private final static String fullName = "Times Bold Italic";
    private final static Set<String> familyNames;
    private final static String encoding = "WinAnsiEncoding";
    private final static int capHeight = 669;
    private final static int xHeight = 462;
    private final static int ascender = 699;
    private final static int descender = -205;
    private final static int firstChar = 32;
    private final static int lastChar = 255;
    private final static int[] width;
    private final CodePointMapping mapping = CodePointMapping
            .getMapping("WinAnsiEncoding");

    private final static Map<Integer, Map<Integer, Integer>> kerning;

    private boolean enableKerning = false;

    static {
        width = new int[256];

        width[0x41] = 667;
        width[0xc6] = 944;
        width[0xc1] = 667;
        width[0xc2] = 667;
        width[0xc4] = 667;
        width[0xc0] = 667;
        width[0xc5] = 667;
        width[0xc3] = 667;
        width[0x42] = 667;
        width[0x43] = 667;
        width[0xc7] = 667;
        width[0x44] = 722;
        width[0x45] = 667;
        width[0xc9] = 667;
        width[0xca] = 667;
        width[0xcb] = 667;
        width[0xc8] = 667;
        width[0xd0] = 722;
        width[0x80] = 500;
        width[0x46] = 667;
        width[0x47] = 722;
        width[0x48] = 778;
        width[0x49] = 389;
        width[0xcd] = 389;
        width[0xce] = 389;
        width[0xcf] = 389;
        width[0xcc] = 389;
        width[0x4a] = 500;
        width[0x4b] = 667;
        width[0x4c] = 611;

        width[0x4d] = 889;
        width[0x4e] = 722;
        width[0xd1] = 722;
        width[0x4f] = 722;
        width[0x8c] = 944;
        width[0xd3] = 722;
        width[0xd4] = 722;
        width[0xd6] = 722;
        width[0xd2] = 722;
        width[0xd8] = 722;
        width[0xd5] = 722;
        width[0x50] = 611;
        width[0x51] = 722;
        width[0x52] = 667;
        width[0x53] = 556;
        width[0x8a] = 556;
        width[0x54] = 611;
        width[0xde] = 611;
        width[0x55] = 722;
        width[0xda] = 722;
        width[0xdb] = 722;
        width[0xdc] = 722;
        width[0xd9] = 722;
        width[0x56] = 667;
        width[0x57] = 889;
        width[0x58] = 667;
        width[0x59] = 611;
        width[0xdd] = 611;
        width[0x9f] = 611;
        width[0x5a] = 611;
        width[0x8e] = 611;
        width[0x61] = 500;
        width[0xe1] = 500;
        width[0xe2] = 500;
        width[0xb4] = 333;
        width[0xe4] = 500;
        width[0xe6] = 722;
        width[0xe0] = 500;
        width[0x26] = 778;
        width[0xe5] = 500;
        width[0x5e] = 570;
        width[0x7e] = 570;
        width[0x2a] = 500;
        width[0x40] = 832;
        width[0xe3] = 500;
        width[0x62] = 500;
        width[0x5c] = 278;
        width[0x7c] = 220;
        width[0x7b] = 348;
        width[0x7d] = 348;
        width[0x5b] = 333;
        width[0x5d] = 333;

        width[0xa6] = 220;
        width[0x95] = 350;
        width[0x63] = 444;

        width[0xe7] = 444;
        width[0xb8] = 333;
        width[0xa2] = 500;
        width[0x88] = 333;
        width[0x3a] = 333;
        width[0x2c] = 250;
        width[0xa9] = 747;
        width[0xa4] = 500;
        width[0x64] = 500;
        width[0x86] = 500;
        width[0x87] = 500;
        width[0xb0] = 400;
        width[0xa8] = 333;
        width[0xf7] = 570;
        width[0x24] = 500;

        width[0x65] = 444;
        width[0xe9] = 444;
        width[0xea] = 444;
        width[0xeb] = 444;
        width[0xe8] = 444;
        width[0x38] = 500;
        width[0x85] = 1000;
        width[0x97] = 1000;
        width[0x96] = 500;
        width[0x3d] = 570;
        width[0xf0] = 500;
        width[0x21] = 389;
        width[0xa1] = 389;
        width[0x66] = 333;

        width[0x35] = 500;

        width[0x83] = 500;
        width[0x34] = 500;

        width[0x67] = 500;
        width[0xdf] = 500;
        width[0x60] = 333;
        width[0x3e] = 570;
        width[0xab] = 500;
        width[0xbb] = 500;
        width[0x8b] = 333;
        width[0x9b] = 333;
        width[0x68] = 556;

        width[0x2d] = 333;
        width[0x69] = 278;
        width[0xed] = 278;
        width[0xee] = 278;
        width[0xef] = 278;
        width[0xec] = 278;
        width[0x6a] = 278;
        width[0x6b] = 500;
        width[0x6c] = 278;
        width[0x3c] = 570;
        width[0xac] = 606;

        width[0x6d] = 778;
        width[0xaf] = 333;

        width[0xb5] = 576;
        width[0xd7] = 570;
        width[0x6e] = 556;
        width[0x39] = 500;
        width[0xf1] = 556;
        width[0x23] = 500;
        width[0x6f] = 500;
        width[0xf3] = 500;
        width[0xf4] = 500;
        width[0xf6] = 500;
        width[0x9c] = 722;

        width[0xf2] = 500;
        width[0x31] = 500;
        width[0xbd] = 750;
        width[0xbc] = 750;
        width[0xb9] = 300;
        width[0xaa] = 266;
        width[0xba] = 300;
        width[0xf8] = 500;
        width[0xf5] = 500;
        width[0x70] = 500;
        width[0xb6] = 500;
        width[0x28] = 333;
        width[0x29] = 333;
        width[0x25] = 833;
        width[0x2e] = 250;
        width[0xb7] = 250;
        width[0x89] = 1000;
        width[0x2b] = 570;
        width[0xb1] = 570;
        width[0x71] = 500;
        width[0x3f] = 500;
        width[0xbf] = 500;
        width[0x22] = 555;
        width[0x84] = 500;
        width[0x93] = 500;
        width[0x94] = 500;
        width[0x91] = 333;
        width[0x92] = 333;
        width[0x82] = 333;
        width[0x27] = 278;
        width[0x72] = 389;
        width[0xae] = 747;

        width[0x73] = 389;
        width[0x9a] = 389;
        width[0xa7] = 500;
        width[0x3b] = 333;
        width[0x37] = 500;
        width[0x36] = 500;
        width[0x2f] = 278;
        width[0x20] = 250;

        width[0xa3] = 500;
        width[0x74] = 278;
        width[0xfe] = 500;
        width[0x33] = 500;
        width[0xbe] = 750;
        width[0xb3] = 300;
        width[0x98] = 333;
        width[0x99] = 1000;
        width[0x32] = 500;
        width[0xb2] = 300;
        width[0x75] = 556;
        width[0xfa] = 556;
        width[0xfb] = 556;
        width[0xfc] = 556;
        width[0xf9] = 556;
        width[0x5f] = 500;
        width[0x76] = 444;
        width[0x77] = 667;
        width[0x78] = 500;
        width[0x79] = 444;
        width[0xfd] = 444;
        width[0xff] = 444;
        width[0xa5] = 500;
        width[0x7a] = 389;
        width[0x9e] = 389;
        width[0x30] = 500;

        kerning = new HashMap<>();
        Integer first, second;
        Map pairs;

        first = new Integer(79);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(65);
        pairs.put(second, new Integer(-40));

        second = new Integer(87);
        pairs.put(second, new Integer(-50));

        second = new Integer(89);
        pairs.put(second, new Integer(-50));

        second = new Integer(84);
        pairs.put(second, new Integer(-40));

        second = new Integer(46);
        pairs.put(second, new Integer(0));

        second = new Integer(86);
        pairs.put(second, new Integer(-50));

        second = new Integer(88);
        pairs.put(second, new Integer(-40));

        second = new Integer(44);
        pairs.put(second, new Integer(0));

        first = new Integer(107);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(-10));

        second = new Integer(121);
        pairs.put(second, new Integer(0));

        second = new Integer(101);
        pairs.put(second, new Integer(-30));

        first = new Integer(112);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(121);
        pairs.put(second, new Integer(0));

        first = new Integer(80);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(-55));

        second = new Integer(97);
        pairs.put(second, new Integer(-40));

        second = new Integer(65);
        pairs.put(second, new Integer(-85));

        second = new Integer(46);
        pairs.put(second, new Integer(-129));

        second = new Integer(101);
        pairs.put(second, new Integer(-50));

        second = new Integer(44);
        pairs.put(second, new Integer(-129));

        first = new Integer(86);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(-111));

        second = new Integer(79);
        pairs.put(second, new Integer(-30));

        second = new Integer(58);
        pairs.put(second, new Integer(-74));

        second = new Integer(71);
        pairs.put(second, new Integer(-10));

        second = new Integer(44);
        pairs.put(second, new Integer(-129));

        second = new Integer(59);
        pairs.put(second, new Integer(-74));

        second = new Integer(45);
        pairs.put(second, new Integer(-70));

        second = new Integer(105);
        pairs.put(second, new Integer(-55));

        second = new Integer(65);
        pairs.put(second, new Integer(-85));

        second = new Integer(97);
        pairs.put(second, new Integer(-111));

        second = new Integer(117);
        pairs.put(second, new Integer(-55));

        second = new Integer(46);
        pairs.put(second, new Integer(-129));

        second = new Integer(101);
        pairs.put(second, new Integer(-111));

        first = new Integer(118);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(-15));

        second = new Integer(97);
        pairs.put(second, new Integer(0));

        second = new Integer(46);
        pairs.put(second, new Integer(-37));

        second = new Integer(101);
        pairs.put(second, new Integer(-15));

        second = new Integer(44);
        pairs.put(second, new Integer(-37));

        first = new Integer(32);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(65);
        pairs.put(second, new Integer(-37));

        second = new Integer(87);
        pairs.put(second, new Integer(-70));

        second = new Integer(147);
        pairs.put(second, new Integer(0));

        second = new Integer(89);
        pairs.put(second, new Integer(-70));

        second = new Integer(84);
        pairs.put(second, new Integer(0));

        second = new Integer(145);
        pairs.put(second, new Integer(0));

        second = new Integer(86);
        pairs.put(second, new Integer(-70));

        first = new Integer(97);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(119);
        pairs.put(second, new Integer(0));

        second = new Integer(116);
        pairs.put(second, new Integer(0));

        second = new Integer(121);
        pairs.put(second, new Integer(0));

        second = new Integer(112);
        pairs.put(second, new Integer(0));

        second = new Integer(103);
        pairs.put(second, new Integer(0));

        second = new Integer(98);
        pairs.put(second, new Integer(0));

        second = new Integer(118);
        pairs.put(second, new Integer(0));

        first = new Integer(70);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(-70));

        second = new Integer(105);
        pairs.put(second, new Integer(-40));

        second = new Integer(114);
        pairs.put(second, new Integer(-50));

        second = new Integer(97);
        pairs.put(second, new Integer(-95));

        second = new Integer(65);
        pairs.put(second, new Integer(-100));

        second = new Integer(46);
        pairs.put(second, new Integer(-129));

        second = new Integer(101);
        pairs.put(second, new Integer(-100));

        second = new Integer(44);
        pairs.put(second, new Integer(-129));

        first = new Integer(85);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(65);
        pairs.put(second, new Integer(-45));

        second = new Integer(46);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(0));

        first = new Integer(100);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(100);
        pairs.put(second, new Integer(0));

        second = new Integer(119);
        pairs.put(second, new Integer(0));

        second = new Integer(121);
        pairs.put(second, new Integer(0));

        second = new Integer(46);
        pairs.put(second, new Integer(0));

        second = new Integer(118);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(0));

        first = new Integer(83);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(46);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(0));

        first = new Integer(122);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(0));

        second = new Integer(101);
        pairs.put(second, new Integer(0));

        first = new Integer(68);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(65);
        pairs.put(second, new Integer(-25));

        second = new Integer(87);
        pairs.put(second, new Integer(-40));

        second = new Integer(89);
        pairs.put(second, new Integer(-50));

        second = new Integer(46);
        pairs.put(second, new Integer(0));

        second = new Integer(86);
        pairs.put(second, new Integer(-50));

        second = new Integer(44);
        pairs.put(second, new Integer(0));

        first = new Integer(146);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(148);
        pairs.put(second, new Integer(0));

        second = new Integer(100);
        pairs.put(second, new Integer(-15));

        second = new Integer(32);
        pairs.put(second, new Integer(-74));

        second = new Integer(146);
        pairs.put(second, new Integer(-74));

        second = new Integer(114);
        pairs.put(second, new Integer(-15));

        second = new Integer(116);
        pairs.put(second, new Integer(-37));

        second = new Integer(108);
        pairs.put(second, new Integer(0));

        second = new Integer(115);
        pairs.put(second, new Integer(-74));

        second = new Integer(118);
        pairs.put(second, new Integer(-15));

        first = new Integer(58);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(32);
        pairs.put(second, new Integer(0));

        first = new Integer(119);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(-15));

        second = new Integer(97);
        pairs.put(second, new Integer(-10));

        second = new Integer(104);
        pairs.put(second, new Integer(0));

        second = new Integer(46);
        pairs.put(second, new Integer(-37));

        second = new Integer(101);
        pairs.put(second, new Integer(-10));

        second = new Integer(44);
        pairs.put(second, new Integer(-37));

        first = new Integer(75);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(-25));

        second = new Integer(79);
        pairs.put(second, new Integer(-30));

        second = new Integer(117);
        pairs.put(second, new Integer(-20));

        second = new Integer(121);
        pairs.put(second, new Integer(-20));

        second = new Integer(101);
        pairs.put(second, new Integer(-25));

        first = new Integer(82);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(79);
        pairs.put(second, new Integer(-40));

        second = new Integer(87);
        pairs.put(second, new Integer(-18));

        second = new Integer(85);
        pairs.put(second, new Integer(-40));

        second = new Integer(89);
        pairs.put(second, new Integer(-18));

        second = new Integer(84);
        pairs.put(second, new Integer(-30));

        second = new Integer(86);
        pairs.put(second, new Integer(-18));

        first = new Integer(145);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(65);
        pairs.put(second, new Integer(0));

        second = new Integer(145);
        pairs.put(second, new Integer(-74));

        first = new Integer(103);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(0));

        second = new Integer(105);
        pairs.put(second, new Integer(0));

        second = new Integer(114);
        pairs.put(second, new Integer(0));

        second = new Integer(97);
        pairs.put(second, new Integer(0));

        second = new Integer(121);
        pairs.put(second, new Integer(0));

        second = new Integer(46);
        pairs.put(second, new Integer(0));

        second = new Integer(103);
        pairs.put(second, new Integer(0));

        second = new Integer(101);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(0));

        first = new Integer(66);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(65);
        pairs.put(second, new Integer(-25));

        second = new Integer(85);
        pairs.put(second, new Integer(-10));

        second = new Integer(46);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(0));

        first = new Integer(98);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(117);
        pairs.put(second, new Integer(-20));

        second = new Integer(121);
        pairs.put(second, new Integer(0));

        second = new Integer(46);
        pairs.put(second, new Integer(-40));

        second = new Integer(108);
        pairs.put(second, new Integer(0));

        second = new Integer(98);
        pairs.put(second, new Integer(-10));

        second = new Integer(118);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(0));

        first = new Integer(81);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(85);
        pairs.put(second, new Integer(-10));

        second = new Integer(46);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(0));

        first = new Integer(44);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(148);
        pairs.put(second, new Integer(-95));

        second = new Integer(32);
        pairs.put(second, new Integer(0));

        second = new Integer(146);
        pairs.put(second, new Integer(-95));

        first = new Integer(102);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(148);
        pairs.put(second, new Integer(0));

        second = new Integer(111);
        pairs.put(second, new Integer(-10));

        second = new Integer(105);
        pairs.put(second, new Integer(0));

        second = new Integer(146);
        pairs.put(second, new Integer(55));

        second = new Integer(97);
        pairs.put(second, new Integer(0));

        second = new Integer(102);
        pairs.put(second, new Integer(-18));

        second = new Integer(46);
        pairs.put(second, new Integer(-10));

        second = new Integer(108);
        pairs.put(second, new Integer(0));

        second = new Integer(101);
        pairs.put(second, new Integer(-10));

        second = new Integer(44);
        pairs.put(second, new Integer(-10));

        first = new Integer(84);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(-95));

        second = new Integer(79);
        pairs.put(second, new Integer(-18));

        second = new Integer(119);
        pairs.put(second, new Integer(-37));

        second = new Integer(58);
        pairs.put(second, new Integer(-74));

        second = new Integer(114);
        pairs.put(second, new Integer(-37));

        second = new Integer(104);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(-92));

        second = new Integer(59);
        pairs.put(second, new Integer(-74));

        second = new Integer(45);
        pairs.put(second, new Integer(-92));

        second = new Integer(105);
        pairs.put(second, new Integer(-37));

        second = new Integer(65);
        pairs.put(second, new Integer(-55));

        second = new Integer(97);
        pairs.put(second, new Integer(-92));

        second = new Integer(117);
        pairs.put(second, new Integer(-37));

        second = new Integer(121);
        pairs.put(second, new Integer(-37));

        second = new Integer(46);
        pairs.put(second, new Integer(-92));

        second = new Integer(101);
        pairs.put(second, new Integer(-92));

        first = new Integer(121);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(0));

        second = new Integer(97);
        pairs.put(second, new Integer(0));

        second = new Integer(46);
        pairs.put(second, new Integer(-37));

        second = new Integer(101);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(-37));

        first = new Integer(120);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(101);
        pairs.put(second, new Integer(-10));

        first = new Integer(101);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(119);
        pairs.put(second, new Integer(0));

        second = new Integer(121);
        pairs.put(second, new Integer(0));

        second = new Integer(112);
        pairs.put(second, new Integer(0));

        second = new Integer(46);
        pairs.put(second, new Integer(0));

        second = new Integer(103);
        pairs.put(second, new Integer(0));

        second = new Integer(98);
        pairs.put(second, new Integer(-10));

        second = new Integer(120);
        pairs.put(second, new Integer(0));

        second = new Integer(118);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(0));

        first = new Integer(99);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(107);
        pairs.put(second, new Integer(-10));

        second = new Integer(104);
        pairs.put(second, new Integer(-10));

        second = new Integer(121);
        pairs.put(second, new Integer(0));

        second = new Integer(46);
        pairs.put(second, new Integer(0));

        second = new Integer(108);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(0));

        first = new Integer(87);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(-80));

        second = new Integer(79);
        pairs.put(second, new Integer(-15));

        second = new Integer(58);
        pairs.put(second, new Integer(-55));

        second = new Integer(104);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(-74));

        second = new Integer(59);
        pairs.put(second, new Integer(-55));

        second = new Integer(45);
        pairs.put(second, new Integer(-50));

        second = new Integer(105);
        pairs.put(second, new Integer(-37));

        second = new Integer(65);
        pairs.put(second, new Integer(-74));

        second = new Integer(97);
        pairs.put(second, new Integer(-85));

        second = new Integer(117);
        pairs.put(second, new Integer(-55));

        second = new Integer(121);
        pairs.put(second, new Integer(-55));

        second = new Integer(46);
        pairs.put(second, new Integer(-74));

        second = new Integer(101);
        pairs.put(second, new Integer(-90));

        first = new Integer(104);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(121);
        pairs.put(second, new Integer(0));

        first = new Integer(71);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(46);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(0));

        first = new Integer(105);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(118);
        pairs.put(second, new Integer(0));

        first = new Integer(65);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(79);
        pairs.put(second, new Integer(-50));

        second = new Integer(146);
        pairs.put(second, new Integer(-74));

        second = new Integer(119);
        pairs.put(second, new Integer(-74));

        second = new Integer(87);
        pairs.put(second, new Integer(-100));

        second = new Integer(67);
        pairs.put(second, new Integer(-65));

        second = new Integer(112);
        pairs.put(second, new Integer(0));

        second = new Integer(81);
        pairs.put(second, new Integer(-55));

        second = new Integer(71);
        pairs.put(second, new Integer(-60));

        second = new Integer(86);
        pairs.put(second, new Integer(-95));

        second = new Integer(118);
        pairs.put(second, new Integer(-74));

        second = new Integer(148);
        pairs.put(second, new Integer(0));

        second = new Integer(85);
        pairs.put(second, new Integer(-50));

        second = new Integer(117);
        pairs.put(second, new Integer(-30));

        second = new Integer(89);
        pairs.put(second, new Integer(-70));

        second = new Integer(121);
        pairs.put(second, new Integer(-74));

        second = new Integer(84);
        pairs.put(second, new Integer(-55));

        first = new Integer(147);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(65);
        pairs.put(second, new Integer(0));

        second = new Integer(145);
        pairs.put(second, new Integer(0));

        first = new Integer(78);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(65);
        pairs.put(second, new Integer(-30));

        second = new Integer(46);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(0));

        first = new Integer(115);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(119);
        pairs.put(second, new Integer(0));

        first = new Integer(111);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(119);
        pairs.put(second, new Integer(-25));

        second = new Integer(121);
        pairs.put(second, new Integer(-10));

        second = new Integer(103);
        pairs.put(second, new Integer(0));

        second = new Integer(120);
        pairs.put(second, new Integer(-10));

        second = new Integer(118);
        pairs.put(second, new Integer(-15));

        first = new Integer(114);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(0));

        second = new Integer(100);
        pairs.put(second, new Integer(0));

        second = new Integer(107);
        pairs.put(second, new Integer(0));

        second = new Integer(114);
        pairs.put(second, new Integer(0));

        second = new Integer(99);
        pairs.put(second, new Integer(0));

        second = new Integer(112);
        pairs.put(second, new Integer(0));

        second = new Integer(103);
        pairs.put(second, new Integer(0));

        second = new Integer(108);
        pairs.put(second, new Integer(0));

        second = new Integer(113);
        pairs.put(second, new Integer(0));

        second = new Integer(118);
        pairs.put(second, new Integer(0));

        second = new Integer(44);
        pairs.put(second, new Integer(-65));

        second = new Integer(45);
        pairs.put(second, new Integer(0));

        second = new Integer(105);
        pairs.put(second, new Integer(0));

        second = new Integer(109);
        pairs.put(second, new Integer(0));

        second = new Integer(97);
        pairs.put(second, new Integer(0));

        second = new Integer(117);
        pairs.put(second, new Integer(0));

        second = new Integer(116);
        pairs.put(second, new Integer(0));

        second = new Integer(121);
        pairs.put(second, new Integer(0));

        second = new Integer(46);
        pairs.put(second, new Integer(-65));

        second = new Integer(110);
        pairs.put(second, new Integer(0));

        second = new Integer(115);
        pairs.put(second, new Integer(0));

        second = new Integer(101);
        pairs.put(second, new Integer(0));

        first = new Integer(108);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(119);
        pairs.put(second, new Integer(0));

        second = new Integer(121);
        pairs.put(second, new Integer(0));

        first = new Integer(76);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(148);
        pairs.put(second, new Integer(0));

        second = new Integer(146);
        pairs.put(second, new Integer(-55));

        second = new Integer(87);
        pairs.put(second, new Integer(-37));

        second = new Integer(89);
        pairs.put(second, new Integer(-37));

        second = new Integer(121);
        pairs.put(second, new Integer(-37));

        second = new Integer(84);
        pairs.put(second, new Integer(-18));

        second = new Integer(86);
        pairs.put(second, new Integer(-37));

        first = new Integer(148);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(32);
        pairs.put(second, new Integer(0));

        first = new Integer(109);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(117);
        pairs.put(second, new Integer(0));

        second = new Integer(121);
        pairs.put(second, new Integer(0));

        first = new Integer(89);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(-111));

        second = new Integer(45);
        pairs.put(second, new Integer(-92));

        second = new Integer(105);
        pairs.put(second, new Integer(-55));

        second = new Integer(79);
        pairs.put(second, new Integer(-25));

        second = new Integer(58);
        pairs.put(second, new Integer(-92));

        second = new Integer(97);
        pairs.put(second, new Integer(-92));

        second = new Integer(65);
        pairs.put(second, new Integer(-74));

        second = new Integer(117);
        pairs.put(second, new Integer(-92));

        second = new Integer(46);
        pairs.put(second, new Integer(-74));

        second = new Integer(101);
        pairs.put(second, new Integer(-111));

        second = new Integer(59);
        pairs.put(second, new Integer(-92));

        second = new Integer(44);
        pairs.put(second, new Integer(-92));

        first = new Integer(74);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(111);
        pairs.put(second, new Integer(-40));

        second = new Integer(97);
        pairs.put(second, new Integer(-40));

        second = new Integer(65);
        pairs.put(second, new Integer(-25));

        second = new Integer(117);
        pairs.put(second, new Integer(-40));

        second = new Integer(46);
        pairs.put(second, new Integer(-10));

        second = new Integer(101);
        pairs.put(second, new Integer(-40));

        second = new Integer(44);
        pairs.put(second, new Integer(-10));

        first = new Integer(46);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(148);
        pairs.put(second, new Integer(-95));

        second = new Integer(146);
        pairs.put(second, new Integer(-95));

        first = new Integer(110);
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = new Integer(117);
        pairs.put(second, new Integer(0));

        second = new Integer(121);
        pairs.put(second, new Integer(0));

        second = new Integer(118);
        pairs.put(second, new Integer(-40));

        familyNames = new HashSet();
        familyNames.add("Times");
    }

    public TimesBoldItalic() {
        this(false);
    }

    public TimesBoldItalic(final boolean enableKerning) {
        this.enableKerning = enableKerning;
    }

    @Override
    public String getEncodingName() {
        return encoding;
    }

    @Override
    public String getFontName() {
        return fontName;
    }

    @Override
    public String getEmbedFontName() {
        return getFontName();
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public Set<String> getFamilyNames() {
        return familyNames;
    }

    @Override
    public FontType getFontType() {
        return FontType.TYPE1;
    }

    @Override
    public int getAscender(final int size) {
        return size * ascender;
    }

    @Override
    public int getCapHeight(final int size) {
        return size * capHeight;
    }

    @Override
    public int getDescender(final int size) {
        return size * descender;
    }

    @Override
    public int getXHeight(final int size) {
        return size * xHeight;
    }

    public int getFirstChar() {
        return firstChar;
    }

    public int getLastChar() {
        return lastChar;
    }

    @Override
    public int getWidth(final int i, final int size) {
        return size * width[i];
    }

    @Override
    public int[] getWidths() {
        final int[] arr = new int[getLastChar() - getFirstChar() + 1];
        System.arraycopy(width, getFirstChar(), arr, 0, getLastChar()
                - getFirstChar() + 1);
        return arr;
    }

    @Override
    public boolean hasKerningInfo() {
        return this.enableKerning;
    }

    @Override
    public Map<Integer, Map<Integer, Integer>> getKerningInfo() {
        return kerning;
    }

    @Override
    public char mapChar(final char c) {
        notifyMapOperation();
        final char d = this.mapping.mapChar(c);
        if (d != 0) {
            return d;
        } else {
            warnMissingGlyph(c);
            return Typeface.NOT_FOUND;
        }
    }

    @Override
    public boolean hasChar(final char c) {
        return this.mapping.mapChar(c) > 0;
    }

}