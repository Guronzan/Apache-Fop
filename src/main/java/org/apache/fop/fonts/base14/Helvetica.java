package org.apache.fop.fonts.base14;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.apache.fop.fonts.Base14Font;
import org.apache.fop.fonts.CodePointMapping;
import org.apache.fop.fonts.FontType;
import org.apache.fop.fonts.Typeface;

public class Helvetica extends Base14Font {
    private final static String fontName = "Helvetica";
    private final static String fullName = "Helvetica";
    private final static Collection<String> familyNames;
    private final static String encoding = "WinAnsiEncoding";
    private final static int capHeight = 718;
    private final static int xHeight = 523;
    private final static int ascender = 718;
    private final static int descender = -207;
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
        width[0xc6] = 1000;
        width[0xc1] = 667;
        width[0xc2] = 667;
        width[0xc4] = 667;
        width[0xc0] = 667;
        width[0xc5] = 667;
        width[0xc3] = 667;
        width[0x42] = 667;
        width[0x43] = 722;
        width[0xc7] = 722;
        width[0x44] = 722;
        width[0x45] = 667;
        width[0xc9] = 667;
        width[0xca] = 667;
        width[0xcb] = 667;
        width[0xc8] = 667;
        width[0xd0] = 722;
        width[0x80] = 556;
        width[0x46] = 611;
        width[0x47] = 778;
        width[0x48] = 722;
        width[0x49] = 278;
        width[0xcd] = 278;
        width[0xce] = 278;
        width[0xcf] = 278;
        width[0xcc] = 278;
        width[0x4a] = 500;
        width[0x4b] = 667;
        width[0x4c] = 556;

        width[0x4d] = 833;
        width[0x4e] = 722;
        width[0xd1] = 722;
        width[0x4f] = 778;
        width[0x8c] = 1000;
        width[0xd3] = 778;
        width[0xd4] = 778;
        width[0xd6] = 778;
        width[0xd2] = 778;
        width[0xd8] = 778;
        width[0xd5] = 778;
        width[0x50] = 667;
        width[0x51] = 778;
        width[0x52] = 722;
        width[0x53] = 667;
        width[0x8a] = 667;
        width[0x54] = 611;
        width[0xde] = 667;
        width[0x55] = 722;
        width[0xda] = 722;
        width[0xdb] = 722;
        width[0xdc] = 722;
        width[0xd9] = 722;
        width[0x56] = 667;
        width[0x57] = 944;
        width[0x58] = 667;
        width[0x59] = 667;
        width[0xdd] = 667;
        width[0x9f] = 667;
        width[0x5a] = 611;
        width[0x8e] = 611;
        width[0x61] = 556;
        width[0xe1] = 556;
        width[0xe2] = 556;
        width[0xb4] = 333;
        width[0xe4] = 556;
        width[0xe6] = 889;
        width[0xe0] = 556;
        width[0x26] = 667;
        width[0xe5] = 556;
        width[0x5e] = 469;
        width[0x7e] = 584;
        width[0x2a] = 389;
        width[0x40] = 1015;
        width[0xe3] = 556;
        width[0x62] = 556;
        width[0x5c] = 278;
        width[0x7c] = 260;
        width[0x7b] = 334;
        width[0x7d] = 334;
        width[0x5b] = 278;
        width[0x5d] = 278;

        width[0xa6] = 260;
        width[0x95] = 350;
        width[0x63] = 500;

        width[0xe7] = 500;
        width[0xb8] = 333;
        width[0xa2] = 556;
        width[0x88] = 333;
        width[0x3a] = 278;
        width[0x2c] = 278;
        width[0xa9] = 737;
        width[0xa4] = 556;
        width[0x64] = 556;
        width[0x86] = 556;
        width[0x87] = 556;
        width[0xb0] = 400;
        width[0xa8] = 333;
        width[0xf7] = 584;
        width[0x24] = 556;

        width[0x65] = 556;
        width[0xe9] = 556;
        width[0xea] = 556;
        width[0xeb] = 556;
        width[0xe8] = 556;
        width[0x38] = 556;
        width[0x85] = 1000;
        width[0x97] = 1000;
        width[0x96] = 556;
        width[0x3d] = 584;
        width[0xf0] = 556;
        width[0x21] = 278;
        width[0xa1] = 333;
        width[0x66] = 278;

        width[0x35] = 556;

        width[0x83] = 556;
        width[0x34] = 556;

        width[0x67] = 556;
        width[0xdf] = 611;
        width[0x60] = 333;
        width[0x3e] = 584;
        width[0xab] = 556;
        width[0xbb] = 556;
        width[0x8b] = 333;
        width[0x9b] = 333;
        width[0x68] = 556;

        width[0x2d] = 333;
        width[0x69] = 222;
        width[0xed] = 278;
        width[0xee] = 278;
        width[0xef] = 278;
        width[0xec] = 278;
        width[0x6a] = 222;
        width[0x6b] = 500;
        width[0x6c] = 222;
        width[0x3c] = 584;
        width[0xac] = 584;

        width[0x6d] = 833;
        width[0xaf] = 333;

        width[0xb5] = 556;
        width[0xd7] = 584;
        width[0x6e] = 556;
        width[0x39] = 556;
        width[0xf1] = 556;
        width[0x23] = 556;
        width[0x6f] = 556;
        width[0xf3] = 556;
        width[0xf4] = 556;
        width[0xf6] = 556;
        width[0x9c] = 944;

        width[0xf2] = 556;
        width[0x31] = 556;
        width[0xbd] = 834;
        width[0xbc] = 834;
        width[0xb9] = 333;
        width[0xaa] = 370;
        width[0xba] = 365;
        width[0xf8] = 611;
        width[0xf5] = 556;
        width[0x70] = 556;
        width[0xb6] = 537;
        width[0x28] = 333;
        width[0x29] = 333;
        width[0x25] = 889;
        width[0x2e] = 278;
        width[0xb7] = 278;
        width[0x89] = 1000;
        width[0x2b] = 584;
        width[0xb1] = 584;
        width[0x71] = 556;
        width[0x3f] = 556;
        width[0xbf] = 611;
        width[0x22] = 355;
        width[0x84] = 333;
        width[0x93] = 333;
        width[0x94] = 333;
        width[0x91] = 222;
        width[0x92] = 222;
        width[0x82] = 222;
        width[0x27] = 191;
        width[0x72] = 333;
        width[0xae] = 737;

        width[0x73] = 500;
        width[0x9a] = 500;
        width[0xa7] = 556;
        width[0x3b] = 278;
        width[0x37] = 556;
        width[0x36] = 556;
        width[0x2f] = 278;
        width[0x20] = 278;

        width[0xa3] = 556;
        width[0x74] = 278;
        width[0xfe] = 556;
        width[0x33] = 556;
        width[0xbe] = 834;
        width[0xb3] = 333;
        width[0x98] = 333;
        width[0x99] = 1000;
        width[0x32] = 556;
        width[0xb2] = 333;
        width[0x75] = 556;
        width[0xfa] = 556;
        width[0xfb] = 556;
        width[0xfc] = 556;
        width[0xf9] = 556;
        width[0x5f] = 556;
        width[0x76] = 500;
        width[0x77] = 722;
        width[0x78] = 500;
        width[0x79] = 500;
        width[0xfd] = 500;
        width[0xff] = 500;
        width[0xa5] = 556;
        width[0x7a] = 500;
        width[0x9e] = 500;
        width[0x30] = 556;

        kerning = new HashMap<>();
        Integer first, second;
        Map<Integer, Integer> pairs;

        first = 107;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 111;
        pairs.put(second, -20);

        second = 101;
        pairs.put(second, -20);

        first = 79;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 65;
        pairs.put(second, -20);

        second = 87;
        pairs.put(second, -30);

        second = 89;
        pairs.put(second, -70);

        second = 84;
        pairs.put(second, -40);

        second = 46;
        pairs.put(second, -40);

        second = 86;
        pairs.put(second, -50);

        second = 88;
        pairs.put(second, -60);

        second = 44;
        pairs.put(second, -40);

        first = 104;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 121;
        pairs.put(second, -30);

        first = 87;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 111;
        pairs.put(second, -30);

        second = 45;
        pairs.put(second, -40);

        second = 79;
        pairs.put(second, -20);

        second = 97;
        pairs.put(second, -40);

        second = 65;
        pairs.put(second, -50);

        second = 117;
        pairs.put(second, -30);

        second = 121;
        pairs.put(second, -20);

        second = 46;
        pairs.put(second, -80);

        second = 101;
        pairs.put(second, -30);

        second = 44;
        pairs.put(second, -80);

        first = 99;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 107;
        pairs.put(second, -20);

        second = 44;
        pairs.put(second, -15);

        first = 112;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 121;
        pairs.put(second, -30);

        second = 46;
        pairs.put(second, -35);

        second = 44;
        pairs.put(second, -35);

        first = 80;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 111;
        pairs.put(second, -50);

        second = 97;
        pairs.put(second, -40);

        second = 65;
        pairs.put(second, -120);

        second = 46;
        pairs.put(second, -180);

        second = 101;
        pairs.put(second, -50);

        second = 44;
        pairs.put(second, -180);

        first = 86;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 111;
        pairs.put(second, -80);

        second = 45;
        pairs.put(second, -80);

        second = 79;
        pairs.put(second, -40);

        second = 58;
        pairs.put(second, -40);

        second = 97;
        pairs.put(second, -70);

        second = 65;
        pairs.put(second, -80);

        second = 117;
        pairs.put(second, -70);

        second = 46;
        pairs.put(second, -125);

        second = 71;
        pairs.put(second, -40);

        second = 101;
        pairs.put(second, -80);

        second = 59;
        pairs.put(second, -40);

        second = 44;
        pairs.put(second, -125);

        first = 118;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 111;
        pairs.put(second, -25);

        second = 97;
        pairs.put(second, -25);

        second = 46;
        pairs.put(second, -80);

        second = 101;
        pairs.put(second, -25);

        second = 44;
        pairs.put(second, -80);

        first = 59;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 32;
        pairs.put(second, -50);

        first = 32;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 87;
        pairs.put(second, -40);

        second = 147;
        pairs.put(second, -30);

        second = 89;
        pairs.put(second, -90);

        second = 84;
        pairs.put(second, -50);

        second = 145;
        pairs.put(second, -60);

        second = 86;
        pairs.put(second, -50);

        first = 97;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 119;
        pairs.put(second, -20);

        second = 121;
        pairs.put(second, -30);

        second = 118;
        pairs.put(second, -20);

        first = 65;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 79;
        pairs.put(second, -30);

        second = 119;
        pairs.put(second, -40);

        second = 87;
        pairs.put(second, -50);

        second = 67;
        pairs.put(second, -30);

        second = 81;
        pairs.put(second, -30);

        second = 71;
        pairs.put(second, -30);

        second = 86;
        pairs.put(second, -70);

        second = 118;
        pairs.put(second, -40);

        second = 85;
        pairs.put(second, -50);

        second = 117;
        pairs.put(second, -30);

        second = 89;
        pairs.put(second, -100);

        second = 84;
        pairs.put(second, -120);

        second = 121;
        pairs.put(second, -40);

        first = 70;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 111;
        pairs.put(second, -30);

        second = 114;
        pairs.put(second, -45);

        second = 97;
        pairs.put(second, -50);

        second = 65;
        pairs.put(second, -80);

        second = 46;
        pairs.put(second, -150);

        second = 101;
        pairs.put(second, -30);

        second = 44;
        pairs.put(second, -150);

        first = 85;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 65;
        pairs.put(second, -40);

        second = 46;
        pairs.put(second, -40);

        second = 44;
        pairs.put(second, -40);

        first = 115;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 119;
        pairs.put(second, -30);

        second = 46;
        pairs.put(second, -15);

        second = 44;
        pairs.put(second, -15);

        first = 122;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 111;
        pairs.put(second, -15);

        second = 101;
        pairs.put(second, -15);

        first = 83;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 46;
        pairs.put(second, -20);

        second = 44;
        pairs.put(second, -20);

        first = 111;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 119;
        pairs.put(second, -15);

        second = 121;
        pairs.put(second, -30);

        second = 46;
        pairs.put(second, -40);

        second = 120;
        pairs.put(second, -30);

        second = 118;
        pairs.put(second, -15);

        second = 44;
        pairs.put(second, -40);

        first = 68;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 65;
        pairs.put(second, -40);

        second = 87;
        pairs.put(second, -40);

        second = 89;
        pairs.put(second, -90);

        second = 46;
        pairs.put(second, -70);

        second = 86;
        pairs.put(second, -70);

        second = 44;
        pairs.put(second, -70);

        first = 146;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 100;
        pairs.put(second, -50);

        second = 32;
        pairs.put(second, -70);

        second = 146;
        pairs.put(second, -57);

        second = 114;
        pairs.put(second, -50);

        second = 115;
        pairs.put(second, -50);

        first = 82;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 79;
        pairs.put(second, -20);

        second = 87;
        pairs.put(second, -30);

        second = 85;
        pairs.put(second, -40);

        second = 89;
        pairs.put(second, -50);

        second = 84;
        pairs.put(second, -30);

        second = 86;
        pairs.put(second, -50);

        first = 75;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 111;
        pairs.put(second, -40);

        second = 79;
        pairs.put(second, -50);

        second = 117;
        pairs.put(second, -30);

        second = 121;
        pairs.put(second, -50);

        second = 101;
        pairs.put(second, -40);

        first = 119;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 111;
        pairs.put(second, -10);

        second = 97;
        pairs.put(second, -15);

        second = 46;
        pairs.put(second, -60);

        second = 101;
        pairs.put(second, -10);

        second = 44;
        pairs.put(second, -60);

        first = 58;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 32;
        pairs.put(second, -50);

        first = 114;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 107;
        pairs.put(second, 15);

        second = 58;
        pairs.put(second, 30);

        second = 112;
        pairs.put(second, 30);

        second = 108;
        pairs.put(second, 15);

        second = 118;
        pairs.put(second, 30);

        second = 44;
        pairs.put(second, -50);

        second = 59;
        pairs.put(second, 30);

        second = 105;
        pairs.put(second, 15);

        second = 109;
        pairs.put(second, 25);

        second = 97;
        pairs.put(second, -10);

        second = 117;
        pairs.put(second, 15);

        second = 116;
        pairs.put(second, 40);

        second = 121;
        pairs.put(second, 30);

        second = 46;
        pairs.put(second, -50);

        second = 110;
        pairs.put(second, 25);

        first = 67;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 46;
        pairs.put(second, -30);

        second = 44;
        pairs.put(second, -30);

        first = 145;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 145;
        pairs.put(second, -57);

        first = 103;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 114;
        pairs.put(second, -10);

        first = 66;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 85;
        pairs.put(second, -10);

        second = 46;
        pairs.put(second, -20);

        second = 44;
        pairs.put(second, -20);

        first = 81;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 85;
        pairs.put(second, -10);

        first = 76;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 148;
        pairs.put(second, -140);

        second = 146;
        pairs.put(second, -160);

        second = 87;
        pairs.put(second, -70);

        second = 89;
        pairs.put(second, -140);

        second = 121;
        pairs.put(second, -30);

        second = 84;
        pairs.put(second, -110);

        second = 86;
        pairs.put(second, -110);

        first = 98;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 117;
        pairs.put(second, -20);

        second = 121;
        pairs.put(second, -20);

        second = 46;
        pairs.put(second, -40);

        second = 108;
        pairs.put(second, -20);

        second = 98;
        pairs.put(second, -10);

        second = 118;
        pairs.put(second, -20);

        second = 44;
        pairs.put(second, -40);

        first = 44;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 148;
        pairs.put(second, -100);

        second = 146;
        pairs.put(second, -100);

        first = 148;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 32;
        pairs.put(second, -40);

        first = 109;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 117;
        pairs.put(second, -10);

        second = 121;
        pairs.put(second, -15);

        first = 248;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 107;
        pairs.put(second, -55);

        second = 104;
        pairs.put(second, -55);

        second = 99;
        pairs.put(second, -55);

        second = 112;
        pairs.put(second, -55);

        second = 113;
        pairs.put(second, -55);

        second = 118;
        pairs.put(second, -70);

        second = 105;
        pairs.put(second, -55);

        second = 97;
        pairs.put(second, -55);

        second = 117;
        pairs.put(second, -55);

        second = 116;
        pairs.put(second, -55);

        second = 106;
        pairs.put(second, -55);

        second = 115;
        pairs.put(second, -55);

        second = 122;
        pairs.put(second, -55);

        second = 100;
        pairs.put(second, -55);

        second = 111;
        pairs.put(second, -55);

        second = 119;
        pairs.put(second, -70);

        second = 114;
        pairs.put(second, -55);

        second = 103;
        pairs.put(second, -55);

        second = 108;
        pairs.put(second, -55);

        second = 98;
        pairs.put(second, -55);

        second = 44;
        pairs.put(second, -95);

        second = 109;
        pairs.put(second, -55);

        second = 102;
        pairs.put(second, -55);

        second = 121;
        pairs.put(second, -70);

        second = 46;
        pairs.put(second, -95);

        second = 110;
        pairs.put(second, -55);

        second = 120;
        pairs.put(second, -85);

        second = 101;
        pairs.put(second, -55);

        first = 102;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 148;
        pairs.put(second, 60);

        second = 111;
        pairs.put(second, -30);

        second = 146;
        pairs.put(second, 50);

        second = 97;
        pairs.put(second, -30);

        second = 46;
        pairs.put(second, -30);

        second = 101;
        pairs.put(second, -30);

        second = 44;
        pairs.put(second, -30);

        first = 74;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 97;
        pairs.put(second, -20);

        second = 65;
        pairs.put(second, -20);

        second = 117;
        pairs.put(second, -20);

        second = 46;
        pairs.put(second, -30);

        second = 44;
        pairs.put(second, -30);

        first = 89;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 111;
        pairs.put(second, -140);

        second = 45;
        pairs.put(second, -140);

        second = 105;
        pairs.put(second, -20);

        second = 79;
        pairs.put(second, -85);

        second = 58;
        pairs.put(second, -60);

        second = 97;
        pairs.put(second, -140);

        second = 65;
        pairs.put(second, -110);

        second = 117;
        pairs.put(second, -110);

        second = 46;
        pairs.put(second, -140);

        second = 101;
        pairs.put(second, -140);

        second = 59;
        pairs.put(second, -60);

        second = 44;
        pairs.put(second, -140);

        first = 121;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 111;
        pairs.put(second, -20);

        second = 97;
        pairs.put(second, -20);

        second = 46;
        pairs.put(second, -100);

        second = 101;
        pairs.put(second, -20);

        second = 44;
        pairs.put(second, -100);

        first = 84;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 111;
        pairs.put(second, -120);

        second = 79;
        pairs.put(second, -40);

        second = 58;
        pairs.put(second, -20);

        second = 119;
        pairs.put(second, -120);

        second = 114;
        pairs.put(second, -120);

        second = 44;
        pairs.put(second, -120);

        second = 59;
        pairs.put(second, -20);

        second = 45;
        pairs.put(second, -140);

        second = 65;
        pairs.put(second, -120);

        second = 97;
        pairs.put(second, -120);

        second = 117;
        pairs.put(second, -120);

        second = 121;
        pairs.put(second, -120);

        second = 46;
        pairs.put(second, -120);

        second = 101;
        pairs.put(second, -120);

        first = 46;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 148;
        pairs.put(second, -100);

        second = 32;
        pairs.put(second, -60);

        second = 146;
        pairs.put(second, -100);

        first = 110;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 117;
        pairs.put(second, -10);

        second = 121;
        pairs.put(second, -15);

        second = 118;
        pairs.put(second, -20);

        first = 120;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 101;
        pairs.put(second, -30);

        first = 101;
        pairs = kerning.get(first);
        if (pairs == null) {
            pairs = new HashMap<>();
            kerning.put(first, pairs);
        }

        second = 119;
        pairs.put(second, -20);

        second = 121;
        pairs.put(second, -20);

        second = 46;
        pairs.put(second, -15);

        second = 120;
        pairs.put(second, -30);

        second = 118;
        pairs.put(second, -30);

        second = 44;
        pairs.put(second, -15);

        familyNames = new HashSet<>();
        familyNames.add("Helvetica");
    }

    public Helvetica() {
        this(false);
    }

    public Helvetica(final boolean enableKerning) {
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
    public Collection<String> getFamilyNames() {
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
