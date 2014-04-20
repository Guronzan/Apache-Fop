/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* $Id: TextLayoutManager.java 893238 2009-12-22 17:20:51Z vhennebert $ */

package org.apache.fop.layoutmgr.inline;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import lombok.extern.slf4j.Slf4j;

import org.apache.fop.area.Trait;
import org.apache.fop.area.inline.TextArea;
import org.apache.fop.fo.Constants;
import org.apache.fop.fo.FOText;
import org.apache.fop.fo.FObj;
import org.apache.fop.fo.properties.StructurePointerPropertySet;
import org.apache.fop.fonts.Font;
import org.apache.fop.fonts.FontSelector;
import org.apache.fop.layoutmgr.InlineKnuthSequence;
import org.apache.fop.layoutmgr.KnuthBox;
import org.apache.fop.layoutmgr.KnuthElement;
import org.apache.fop.layoutmgr.KnuthGlue;
import org.apache.fop.layoutmgr.KnuthPenalty;
import org.apache.fop.layoutmgr.KnuthSequence;
import org.apache.fop.layoutmgr.LayoutContext;
import org.apache.fop.layoutmgr.LeafPosition;
import org.apache.fop.layoutmgr.ListElement;
import org.apache.fop.layoutmgr.Position;
import org.apache.fop.layoutmgr.PositionIterator;
import org.apache.fop.layoutmgr.TraitSetter;
import org.apache.fop.text.linebreak.LineBreakStatus;
import org.apache.fop.traits.MinOptMax;
import org.apache.fop.traits.SpaceVal;
import org.apache.fop.util.CharUtilities;
import org.apache.fop.util.ListUtil;

/**
 * LayoutManager for text (a sequence of characters) which generates one or more
 * inline areas.
 */
@Slf4j
public class TextLayoutManager extends LeafNodeLayoutManager {

    // TODO: remove all final modifiers at local variables

    // static final int SOFT_HYPHEN_PENALTY = KnuthPenalty.FLAGGED_PENALTY / 10;
    private static final int SOFT_HYPHEN_PENALTY = 1;

    /**
     * Store information about each potential text area. Index of character
     * which ends the area, IPD of area, including any word-space and
     * letter-space. Number of word-spaces?
     */
    private class AreaInfo {

        private final int startIndex;
        private final int breakIndex;
        private final int wordSpaceCount;
        private int letterSpaceCount;
        private MinOptMax areaIPD;
        private final boolean isHyphenated;
        private final boolean isSpace;
        private boolean breakOppAfter;
        private final Font font;

        AreaInfo(final int startIndex, final int breakIndex,
                final int wordSpaceCount, final int letterSpaceCount,
                final MinOptMax areaIPD, final boolean isHyphenated,
                final boolean isSpace, final boolean breakOppAfter,
                final Font font) {
            assert startIndex <= breakIndex;
            this.startIndex = startIndex;
            this.breakIndex = breakIndex;
            this.wordSpaceCount = wordSpaceCount;
            this.letterSpaceCount = letterSpaceCount;
            this.areaIPD = areaIPD;
            this.isHyphenated = isHyphenated;
            this.isSpace = isSpace;
            this.breakOppAfter = breakOppAfter;
            this.font = font;
        }

        private int getCharLength() {
            return this.breakIndex - this.startIndex;
        }

        private void addToAreaIPD(final MinOptMax idp) {
            this.areaIPD = this.areaIPD.plus(idp);
        }

        @Override
        public String toString() {
            return "AreaInfo[" + "letterSpaceCount = " + this.letterSpaceCount
                    + ", wordSpaceCount = " + this.wordSpaceCount
                    + ", areaIPD = " + this.areaIPD + ", startIndex = "
                    + this.startIndex + ", breakIndex = " + this.breakIndex
                    + ", isHyphenated = " + this.isHyphenated + ", isSpace = "
                    + this.isSpace + ", font = " + this.font + "]";
        }
    }

    /**
     * this class stores information about changes in vecAreaInfo which are not
     * yet applied
     */
    private final class PendingChange {

        private final AreaInfo areaInfo;
        private final int index;

        private PendingChange(final AreaInfo areaInfo, final int index) {
            this.areaInfo = areaInfo;
            this.index = index;
        }
    }

    // Hold all possible breaks for the text in this LM's FO.
    private final List<AreaInfo> areaInfos;

    /** Non-space characters on which we can end a line. */
    private static final String BREAK_CHARS = "-/";

    private final FOText foText;

    /**
     * Contains an array of widths to adjust for kerning. The first entry can be
     * used to influence the start position of the first letter. The entry i+1
     * defines the cursor advancement after the character i. A null entry means
     * no special advancement.
     */
    private final MinOptMax[] letterAdjustArray; // size = textArray.length + 1

    /** Font used for the space between words. */
    private Font spaceFont = null;
    /** Start index of next TextArea */
    private int nextStart = 0;
    /** size of a space character (U+0020) glyph in current font */
    private int spaceCharIPD;
    private MinOptMax wordSpaceIPD;
    private MinOptMax letterSpaceIPD;
    /** size of the hyphen character glyph in current font */
    private int hyphIPD;

    private boolean hasChanged = false;
    private int returnedIndex = 0;
    private int thisStart = 0;
    private int tempStart = 0;
    private final List<PendingChange> changeList = new LinkedList<PendingChange>();

    private AlignmentContext alignmentContext = null;

    /**
     * The width to be reserved for border and padding at the start of the line.
     */
    private int lineStartBAP = 0;

    /**
     * The width to be reserved for border and padding at the end of the line.
     */
    private int lineEndBAP = 0;

    private boolean keepTogether;

    private final Position auxiliaryPosition = new LeafPosition(this, -1);

    /**
     * Create a Text layout manager.
     *
     * @param node
     *            The FOText object to be rendered
     */
    public TextLayoutManager(final FOText node) {
        this.foText = node;
        this.letterAdjustArray = new MinOptMax[node.length() + 1];
        this.areaInfos = new ArrayList<AreaInfo>();
    }

    private KnuthPenalty makeZeroWidthPenalty(final int penaltyValue) {
        return new KnuthPenalty(0, penaltyValue, false, this.auxiliaryPosition,
                true);
    }

    private KnuthBox makeAuxiliaryZeroWidthBox() {
        return new KnuthInlineBox(0, null,
                notifyPos(new LeafPosition(this, -1)), true);
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {

        this.foText.resetBuffer();

        this.spaceFont = FontSelector.selectFontForCharacterInText(' ',
                this.foText, this);

        // With CID fonts, space isn't necessary currentFontState.width(32)
        this.spaceCharIPD = this.spaceFont.getCharWidth(' ');

        // Use hyphenationChar property
        // TODO: Use hyphen based on actual font used!
        this.hyphIPD = this.foText.getCommonHyphenation().getHyphIPD(
                this.spaceFont);

        final SpaceVal letterSpacing = SpaceVal.makeLetterSpacing(this.foText
                .getLetterSpacing());
        final SpaceVal wordSpacing = SpaceVal.makeWordSpacing(
                this.foText.getWordSpacing(), letterSpacing, this.spaceFont);

        // letter space applies only to consecutive non-space characters,
        // while word space applies to space characters;
        // i.e. the spaces in the string "A SIMPLE TEST" are:
        // A<<ws>>S<ls>I<ls>M<ls>P<ls>L<ls>E<<ws>>T<ls>E<ls>S<ls>T
        // there is no letter space after the last character of a word,
        // nor after a space character
        // NOTE: The above is not quite correct. Read on in XSL 1.0, 7.16.2,
        // letter-spacing

        // set letter space and word space dimension;
        // the default value "normal" was converted into a MinOptMax value
        // in the SpaceVal.makeWordSpacing() method
        this.letterSpaceIPD = letterSpacing.getSpace();
        this.wordSpaceIPD = MinOptMax.getInstance(this.spaceCharIPD).plus(
                wordSpacing.getSpace());
        this.keepTogether = this.foText.getKeepTogether().getWithinLine()
                .getEnum() == Constants.EN_ALWAYS;
    }

    /**
     * Generate and add areas to parent area. This can either generate an area
     * for each TextArea and each space, or an area containing all text with a
     * parameter controlling the size of the word space. The latter is most
     * efficient for PDF generation. Set size of each area.
     *
     * @param posIter
     *            Iterator over Position information returned by this
     *            LayoutManager.
     * @param context
     *            LayoutContext for adjustments
     */
    @Override
    public void addAreas(final PositionIterator posIter,
            final LayoutContext context) {

        // Add word areas
        AreaInfo areaInfo;
        int wordSpaceCount = 0;
        int letterSpaceCount = 0;
        int firstAreaInfoIndex = -1;
        int lastAreaInfoIndex = 0;
        MinOptMax realWidth = MinOptMax.ZERO;

        /*
         * On first area created, add any leading space. Calculate word-space
         * stretch value.
         */
        AreaInfo lastAreaInfo = null;
        while (posIter.hasNext()) {
            final LeafPosition tbpNext = (LeafPosition) posIter.next();
            if (tbpNext == null) {
                continue; // Ignore elements without Positions
            }
            if (tbpNext.getLeafPos() != -1) {
                areaInfo = this.areaInfos.get(tbpNext.getLeafPos());
                if (lastAreaInfo == null || areaInfo.font != lastAreaInfo.font) {
                    if (lastAreaInfo != null) {
                        addAreaInfoAreas(lastAreaInfo, wordSpaceCount,
                                letterSpaceCount, firstAreaInfoIndex,
                                lastAreaInfoIndex, realWidth, context);
                    }
                    firstAreaInfoIndex = tbpNext.getLeafPos();
                    wordSpaceCount = 0;
                    letterSpaceCount = 0;
                    realWidth = MinOptMax.ZERO;
                }
                wordSpaceCount += areaInfo.wordSpaceCount;
                letterSpaceCount += areaInfo.letterSpaceCount;
                realWidth = realWidth.plus(areaInfo.areaIPD);
                lastAreaInfoIndex = tbpNext.getLeafPos();
                lastAreaInfo = areaInfo;
            }
        }
        if (lastAreaInfo != null) {
            addAreaInfoAreas(lastAreaInfo, wordSpaceCount, letterSpaceCount,
                    firstAreaInfoIndex, lastAreaInfoIndex, realWidth, context);
        }
    }

    private void addAreaInfoAreas(final AreaInfo areaInfo,
            final int wordSpaceCount, int letterSpaceCount,
            final int firstAreaInfoIndex, final int lastAreaInfoIndex,
            MinOptMax realWidth, final LayoutContext context) {

        // TODO: These two statements (if, for) were like this before my recent
        // changes. However, it seems as if they should use the AreaInfo from
        // firstAreaInfoIndex.. lastAreaInfoIndex rather than just the last
        // areaInfo.
        // This needs to be checked.
        final int textLength = areaInfo.getCharLength();
        if (areaInfo.letterSpaceCount == textLength && !areaInfo.isHyphenated
                && context.isLastArea()) {
            // the line ends at a character like "/" or "-";
            // remove the letter space after the last character
            realWidth = realWidth.minus(this.letterSpaceIPD);
            letterSpaceCount--;
        }

        for (int i = areaInfo.startIndex; i < areaInfo.breakIndex; ++i) {
            final MinOptMax letterAdjustment = this.letterAdjustArray[i + 1];
            if (letterAdjustment != null && letterAdjustment.isElastic()) {
                letterSpaceCount++;
            }
        }

        // add hyphenation character if the last word is hyphenated
        if (context.isLastArea() && areaInfo.isHyphenated) {
            realWidth = realWidth.plus(this.hyphIPD);
        }

        /* Calculate adjustments */
        final double ipdAdjust = context.getIPDAdjust();

        // calculate total difference between real and available width
        int difference;
        if (ipdAdjust > 0.0) {
            difference = (int) (realWidth.getStretch() * ipdAdjust);
        } else {
            difference = (int) (realWidth.getShrink() * ipdAdjust);
        }

        // set letter space adjustment
        int letterSpaceDim = this.letterSpaceIPD.getOpt();
        if (ipdAdjust > 0.0) {
            letterSpaceDim += (int) (this.letterSpaceIPD.getStretch() * ipdAdjust);
        } else {
            letterSpaceDim += (int) (this.letterSpaceIPD.getShrink() * ipdAdjust);
        }
        int totalAdjust = (letterSpaceDim - this.letterSpaceIPD.getOpt())
                * letterSpaceCount;

        // set word space adjustment
        int wordSpaceDim = this.wordSpaceIPD.getOpt();
        if (wordSpaceCount > 0) {
            wordSpaceDim += (difference - totalAdjust) / wordSpaceCount;
        }
        totalAdjust += (wordSpaceDim - this.wordSpaceIPD.getOpt())
                * wordSpaceCount;
        if (totalAdjust != difference) {
            // the applied adjustment is greater or smaller than the needed one
            log.trace("TextLM.addAreas: error in word / letter space adjustment = "
                    + (totalAdjust - difference));
            // set totalAdjust = difference, so that the width of the TextArea
            // will counterbalance the error and the other inline areas will be
            // placed correctly
            totalAdjust = difference;
        }

        final TextArea textArea = new TextAreaBuilder(realWidth, totalAdjust,
                context, firstAreaInfoIndex, lastAreaInfoIndex,
                context.isLastArea(), areaInfo.font).build();

        // wordSpaceDim is computed in relation to wordSpaceIPD.opt
        // but the renderer needs to know the adjustment in relation
        // to the size of the space character in the current font;
        // moreover, the pdf renderer adds the character spacing even to
        // the last character of a word and to space characters: in order
        // to avoid this, we must subtract the letter space width twice;
        // the renderer will compute the space width as:
        // space width =
        // = "normal" space width + letterSpaceAdjust + wordSpaceAdjust
        // = spaceCharIPD + letterSpaceAdjust +
        // + (wordSpaceDim - spaceCharIPD - 2 * letterSpaceAdjust)
        // = wordSpaceDim - letterSpaceAdjust
        textArea.setTextLetterSpaceAdjust(letterSpaceDim);
        textArea.setTextWordSpaceAdjust(wordSpaceDim - this.spaceCharIPD - 2
                * textArea.getTextLetterSpaceAdjust());
        if (context.getIPDAdjust() != 0) {
            // add information about space width
            textArea.setSpaceDifference(this.wordSpaceIPD.getOpt()
                    - this.spaceCharIPD - 2
                    * textArea.getTextLetterSpaceAdjust());
        }
        this.parentLayoutManager.addChildArea(textArea);
    }

    private final class TextAreaBuilder {

        private final MinOptMax width;
        private final int adjust;
        private final LayoutContext context;
        private final int firstIndex;
        private final int lastIndex;
        private final boolean isLastArea;
        private final Font font;

        private int blockProgressionDimension;
        private AreaInfo areaInfo;
        private StringBuilder wordChars;
        private int[] letterAdjust;
        private int letterAdjustIndex;

        private TextArea textArea;

        /**
         * Creates a new <code>TextAreaBuilder</code> which itself builds an
         * inline word area. This creates a TextArea and sets up the various
         * attributes.
         *
         * @param width
         *            the MinOptMax width of the content
         * @param adjust
         *            the total ipd adjustment with respect to the optimal width
         * @param context
         *            the layout context
         * @param firstIndex
         *            the index of the first AreaInfo used for the TextArea
         * @param lastIndex
         *            the index of the last AreaInfo used for the TextArea
         * @param isLastArea
         *            is this TextArea the last in a line?
         * @param font
         *            Font to be used in this particular TextArea
         */
        private TextAreaBuilder(final MinOptMax width, final int adjust,
                final LayoutContext context, final int firstIndex,
                final int lastIndex, final boolean isLastArea, final Font font) {
            this.width = width;
            this.adjust = adjust;
            this.context = context;
            this.firstIndex = firstIndex;
            this.lastIndex = lastIndex;
            this.isLastArea = isLastArea;
            this.font = font;
        }

        private TextArea build() {
            createTextArea();
            setInlineProgressionDimension();
            calcBlockProgressionDimension();
            setBlockProgressionDimension();
            setBaselineOffset();
            setOffset();
            setText();
            TraitSetter.addFontTraits(this.textArea, this.font);
            this.textArea.addTrait(Trait.COLOR,
                    TextLayoutManager.this.foText.getColor());
            TraitSetter.addPtr(this.textArea, getPtr()); // used for
            // accessibility
            TraitSetter.addTextDecoration(this.textArea,
                    TextLayoutManager.this.foText.getTextDecoration());
            TraitSetter.addFontTraits(this.textArea, this.font);
            return this.textArea;
        }

        /**
         * Creates an plain <code>TextArea</code> or a justified
         * <code>TextArea</code> with additional information.
         */
        private void createTextArea() {
            if (this.context.getIPDAdjust() == 0.0) {
                this.textArea = new TextArea();
            } else {
                this.textArea = new TextArea(this.width.getStretch(),
                        this.width.getShrink(), this.adjust);
            }
        }

        private void setInlineProgressionDimension() {
            this.textArea.setIPD(this.width.getOpt() + this.adjust);
        }

        private void calcBlockProgressionDimension() {
            this.blockProgressionDimension = this.font.getAscender()
                    - this.font.getDescender();
        }

        private void setBlockProgressionDimension() {
            this.textArea.setBPD(this.blockProgressionDimension);
        }

        private void setBaselineOffset() {
            this.textArea.setBaselineOffset(this.font.getAscender());
        }

        private void setOffset() {
            if (this.blockProgressionDimension == TextLayoutManager.this.alignmentContext
                    .getHeight()) {
                this.textArea.setOffset(0);
            } else {
                this.textArea.setOffset(TextLayoutManager.this.alignmentContext
                        .getOffset());
            }
        }

        /**
         * Sets the text of the TextArea, split into words and spaces.
         */
        private void setText() {
            int wordStartIndex = -1;
            int wordCharLength = 0;
            for (int wordIndex = this.firstIndex; wordIndex <= this.lastIndex; wordIndex++) {
                this.areaInfo = getAreaInfo(wordIndex);
                if (this.areaInfo.isSpace) {
                    addSpaces();
                } else {
                    // areaInfo stores information about a word fragment
                    if (wordStartIndex == -1) {
                        // here starts a new word
                        wordStartIndex = wordIndex;
                        wordCharLength = 0;
                    }
                    wordCharLength += this.areaInfo.getCharLength();
                    if (isWordEnd(wordIndex)) {
                        addWord(wordStartIndex, wordIndex, wordCharLength);
                        wordStartIndex = -1;
                    }
                }
            }
        }

        private boolean isWordEnd(final int areaInfoIndex) {
            return areaInfoIndex == this.lastIndex
                    || getAreaInfo(areaInfoIndex + 1).isSpace;
        }

        private void addWord(final int startIndex, final int endIndex,
                int charLength) {
            if (isHyphenated(endIndex)) {
                charLength++;
            }
            initWord(charLength);
            for (int i = startIndex; i <= endIndex; ++i) {
                final AreaInfo wordAreaInfo = getAreaInfo(i);
                addWordChars(wordAreaInfo);
                addLetterAdjust(wordAreaInfo);
            }
            if (isHyphenated(endIndex)) {
                addHyphenationChar();
            }
            this.textArea.addWord(this.wordChars.toString(), 0,
                    this.letterAdjust);
        }

        private void initWord(final int charLength) {
            this.wordChars = new StringBuilder(charLength);
            this.letterAdjust = new int[charLength];
            this.letterAdjustIndex = 0;
        }

        private boolean isHyphenated(final int endIndex) {
            return this.isLastArea && endIndex == this.lastIndex
                    && this.areaInfo.isHyphenated;
        }

        private void addHyphenationChar() {
            this.wordChars.append(TextLayoutManager.this.foText
                    .getCommonHyphenation().getHyphChar(this.font));
        }

        private void addWordChars(final AreaInfo wordAreaInfo) {
            for (int i = wordAreaInfo.startIndex; i < wordAreaInfo.breakIndex; ++i) {
                this.wordChars.append(TextLayoutManager.this.foText.charAt(i));
            }
        }

        private void addLetterAdjust(final AreaInfo wordAreaInfo) {
            int letterSpaceCount = wordAreaInfo.letterSpaceCount;
            for (int i = wordAreaInfo.startIndex; i < wordAreaInfo.breakIndex; ++i) {
                if (this.letterAdjustIndex > 0) {
                    final MinOptMax adj = TextLayoutManager.this.letterAdjustArray[i];
                    this.letterAdjust[this.letterAdjustIndex] = adj == null ? 0
                            : adj.getOpt();
                }
                if (letterSpaceCount > 0) {
                    this.letterAdjust[this.letterAdjustIndex] += this.textArea
                            .getTextLetterSpaceAdjust();
                    letterSpaceCount--;
                }
                this.letterAdjustIndex++;
            }
        }

        /**
         * The <code>AreaInfo</code> stores information about spaces.
         * <p/>
         * Add the spaces - except zero-width spaces - to the TextArea.
         */
        private void addSpaces() {
            for (int i = this.areaInfo.startIndex; i < this.areaInfo.breakIndex; ++i) {
                final char spaceChar = TextLayoutManager.this.foText.charAt(i);
                if (!CharUtilities.isZeroWidthSpace(spaceChar)) {
                    this.textArea.addSpace(spaceChar, 0,
                            CharUtilities.isAdjustableSpace(spaceChar));
                }
            }
        }
    }

    /**
     * used for accessibility
     *
     * @return ptr of fobj
     */
    private String getPtr() {
        final FObj fobj = this.parentLayoutManager.getFObj();
        if (fobj instanceof StructurePointerPropertySet) {
            return ((StructurePointerPropertySet) fobj).getPtr();
        } else {
            // No structure pointer applicable
            return null;
        }
    }

    private AreaInfo getAreaInfo(final int index) {
        return this.areaInfos.get(index);
    }

    private void addToLetterAdjust(final int index, final int width) {
        if (this.letterAdjustArray[index] == null) {
            this.letterAdjustArray[index] = MinOptMax.getInstance(width);
        } else {
            this.letterAdjustArray[index] = this.letterAdjustArray[index]
                    .plus(width);
        }
    }

    /**
     * Indicates whether a character is a space in terms of this layout manager.
     *
     * @param ch
     *            the character
     * @return true if it's a space
     */
    private static boolean isSpace(final char ch) {
        return ch == CharUtilities.SPACE
                || CharUtilities.isNonBreakableSpace(ch)
                || CharUtilities.isFixedWidthSpace(ch);
    }

    /** {@inheritDoc} */
    @Override
    public List<KnuthSequence> getNextKnuthElements(
            final LayoutContext context, final int alignment) {
        this.lineStartBAP = context.getLineStartBorderAndPaddingWidth();
        this.lineEndBAP = context.getLineEndBorderAndPaddingWidth();
        this.alignmentContext = context.getAlignmentContext();

        final List<KnuthSequence> returnList = new LinkedList<>();
        KnuthSequence sequence = new InlineKnuthSequence();
        AreaInfo areaInfo = null;
        AreaInfo prevAreaInfo = null;
        returnList.add(sequence);

        final LineBreakStatus lineBreakStatus = new LineBreakStatus();
        this.thisStart = this.nextStart;
        boolean inWord = false;
        boolean inWhitespace = false;
        char ch = 0;
        while (this.nextStart < this.foText.length()) {
            ch = this.foText.charAt(this.nextStart);
            boolean breakOpportunity = false;
            final byte breakAction = this.keepTogether ? LineBreakStatus.PROHIBITED_BREAK
                    : lineBreakStatus.nextChar(ch);
            switch (breakAction) {
            case LineBreakStatus.COMBINING_PROHIBITED_BREAK:
            case LineBreakStatus.PROHIBITED_BREAK:
                break;
            case LineBreakStatus.EXPLICIT_BREAK:
                break;
            case LineBreakStatus.COMBINING_INDIRECT_BREAK:
            case LineBreakStatus.DIRECT_BREAK:
            case LineBreakStatus.INDIRECT_BREAK:
                breakOpportunity = true;
                break;
            default:
                log.error("Unexpected breakAction: " + breakAction);
            }
            if (inWord) {
                if (breakOpportunity || TextLayoutManager.isSpace(ch)
                        || CharUtilities.isExplicitBreak(ch)) {
                    // this.foText.charAt(lastIndex) ==
                    // CharUtilities.SOFT_HYPHEN
                    prevAreaInfo = processWord(alignment, sequence,
                            prevAreaInfo, ch, breakOpportunity, true);
                }
            } else if (inWhitespace) {
                if (ch != CharUtilities.SPACE || breakOpportunity) {
                    prevAreaInfo = processWhitespace(alignment, sequence,
                            breakOpportunity);
                }
            } else {
                if (areaInfo != null) {
                    prevAreaInfo = areaInfo;
                    processLeftoverAreaInfo(alignment, sequence, areaInfo,
                            ch == CharUtilities.SPACE || breakOpportunity);
                    areaInfo = null;
                }
                if (breakAction == LineBreakStatus.EXPLICIT_BREAK) {
                    sequence = processLinebreak(returnList, sequence);
                }
            }

            if (ch == CharUtilities.SPACE
                    && this.foText.getWhitespaceTreatment() == Constants.EN_PRESERVE
                    || ch == CharUtilities.NBSPACE) {
                // preserved space or non-breaking space:
                // create the AreaInfo object
                areaInfo = new AreaInfo(this.nextStart, this.nextStart + 1, 1,
                        0, this.wordSpaceIPD, false, true, breakOpportunity,
                        this.spaceFont);
                this.thisStart = this.nextStart + 1;
            } else if (CharUtilities.isFixedWidthSpace(ch)
                    || CharUtilities.isZeroWidthSpace(ch)) {
                // create the AreaInfo object
                final Font font = FontSelector.selectFontForCharacterInText(ch,
                        this.foText, this);
                final MinOptMax ipd = MinOptMax.getInstance(font
                        .getCharWidth(ch));
                areaInfo = new AreaInfo(this.nextStart, this.nextStart + 1, 0,
                        0, ipd, false, true, breakOpportunity, font);
                this.thisStart = this.nextStart + 1;
            } else if (CharUtilities.isExplicitBreak(ch)) {
                // mandatory break-character: only advance index
                this.thisStart = this.nextStart + 1;
            }

            inWord = !TextLayoutManager.isSpace(ch)
                    && !CharUtilities.isExplicitBreak(ch);
            inWhitespace = ch == CharUtilities.SPACE
                    && this.foText.getWhitespaceTreatment() != Constants.EN_PRESERVE;
            this.nextStart++;
        }

        // Process any last elements
        if (inWord) {
            processWord(alignment, sequence, prevAreaInfo, ch, false, false);
        } else if (inWhitespace) {
            processWhitespace(alignment, sequence, true);
        } else if (areaInfo != null) {
            processLeftoverAreaInfo(alignment, sequence, areaInfo,
                    ch == CharUtilities.ZERO_WIDTH_SPACE);
        } else if (CharUtilities.isExplicitBreak(ch)) {
            processLinebreak(returnList, sequence);
        }

        if (((List) ListUtil.getLast(returnList)).isEmpty()) {
            // Remove an empty sequence because of a trailing newline
            ListUtil.removeLast(returnList);
        }

        setFinished(true);
        if (returnList.isEmpty()) {
            return null;
        } else {
            return returnList;
        }
    }

    private KnuthSequence processLinebreak(
            final List<KnuthSequence> returnList, KnuthSequence sequence) {
        if (this.lineEndBAP != 0) {
            sequence.add(new KnuthGlue(this.lineEndBAP, 0, 0,
                    this.auxiliaryPosition, true));
        }
        sequence.endSequence();
        sequence = new InlineKnuthSequence();
        returnList.add(sequence);
        return sequence;
    }

    private void processLeftoverAreaInfo(final int alignment,
            final KnuthSequence sequence, final AreaInfo areaInfo,
            final boolean breakOpportunityAfter) {
        this.areaInfos.add(areaInfo);
        areaInfo.breakOppAfter = breakOpportunityAfter;
        addElementsForASpace(sequence, alignment, areaInfo,
                this.areaInfos.size() - 1);
    }

    private AreaInfo processWhitespace(final int alignment,
            final KnuthSequence sequence, final boolean breakOpportunity) {
        // End of whitespace
        // create the AreaInfo object
        assert this.nextStart >= this.thisStart;
        final AreaInfo areaInfo = new AreaInfo(this.thisStart, this.nextStart,
                this.nextStart - this.thisStart, 0,
                this.wordSpaceIPD.mult(this.nextStart - this.thisStart), false,
                true, breakOpportunity, this.spaceFont);

        this.areaInfos.add(areaInfo);

        // create the elements
        addElementsForASpace(sequence, alignment, areaInfo,
                this.areaInfos.size() - 1);

        this.thisStart = this.nextStart;
        return areaInfo;
    }

    private AreaInfo processWord(final int alignment,
            final KnuthSequence sequence, AreaInfo prevAreaInfo, final char ch,
            final boolean breakOpportunity, final boolean checkEndsWithHyphen) {

        // Word boundary found, process widths and kerning
        int lastIndex = this.nextStart;
        while (lastIndex > 0
                && this.foText.charAt(lastIndex - 1) == CharUtilities.SOFT_HYPHEN) {
            lastIndex--;
        }
        final boolean endsWithHyphen = checkEndsWithHyphen
                && this.foText.charAt(lastIndex) == CharUtilities.SOFT_HYPHEN;
        final Font font = FontSelector.selectFontForCharactersInText(
                this.foText, this.thisStart, lastIndex, this.foText, this);
        final int wordLength = lastIndex - this.thisStart;
        final boolean kerning = font.hasKerning();
        MinOptMax wordIPD = MinOptMax.ZERO;
        for (int i = this.thisStart; i < lastIndex; ++i) {
            final char currentChar = this.foText.charAt(i);

            // character width
            final int charWidth = font.getCharWidth(currentChar);
            wordIPD = wordIPD.plus(charWidth);

            // kerning
            if (kerning) {
                int kern = 0;
                if (i > this.thisStart) {
                    final char previousChar = this.foText.charAt(i - 1);
                    kern = font.getKernValue(previousChar, currentChar);
                } else if (prevAreaInfo != null && !prevAreaInfo.isSpace
                        && prevAreaInfo.breakIndex > 0) {
                    final char previousChar = this.foText
                            .charAt(prevAreaInfo.breakIndex - 1);
                    kern = font.getKernValue(previousChar, currentChar);
                }
                if (kern != 0) {
                    addToLetterAdjust(i, kern);
                    wordIPD = wordIPD.plus(kern);
                }
            }
        }
        if (kerning && breakOpportunity && !TextLayoutManager.isSpace(ch)
                && lastIndex > 0 && endsWithHyphen) {
            final int kern = font.getKernValue(
                    this.foText.charAt(lastIndex - 1), ch);
            if (kern != 0) {
                addToLetterAdjust(lastIndex, kern);
                // TODO: add kern to wordIPD?
            }
        }
        int iLetterSpaces = wordLength - 1;
        // if there is a break opportunity and the next one
        // is not a space, it could be used as a line end;
        // add one more letter space, in case other text follows
        if (breakOpportunity && !TextLayoutManager.isSpace(ch)) {
            iLetterSpaces++;
        }
        assert iLetterSpaces >= 0;
        wordIPD = wordIPD.plus(this.letterSpaceIPD.mult(iLetterSpaces));

        // create the AreaInfo object
        final AreaInfo areaInfo = new AreaInfo(this.thisStart, lastIndex, 0,
                iLetterSpaces, wordIPD, endsWithHyphen, false,
                breakOpportunity, font);
        prevAreaInfo = areaInfo;
        this.areaInfos.add(areaInfo);
        this.tempStart = this.nextStart;

        // add the elements
        addElementsForAWordFragment(sequence, alignment, areaInfo,
                this.areaInfos.size() - 1);
        this.thisStart = this.nextStart;

        return prevAreaInfo;
    }

    /** {@inheritDoc} */
    @Override
    public List<ListElement> addALetterSpaceTo(final List<ListElement> oldList) {
        // old list contains only a box, or the sequence: box penalty glue box;
        // look at the Position stored in the first element in oldList
        // which is always a box
        ListIterator<ListElement> oldListIterator = oldList.listIterator();
        final ListElement knuthElement = oldListIterator.next();
        final LeafPosition pos = (LeafPosition) ((KnuthBox) knuthElement)
                .getPosition();
        final int index = pos.getLeafPos();
        // element could refer to '-1' position, for non-collapsed spaces (?)
        if (index > -1) {
            final AreaInfo areaInfo = getAreaInfo(index);
            areaInfo.letterSpaceCount++;
            areaInfo.addToAreaIPD(this.letterSpaceIPD);
            if (TextLayoutManager.BREAK_CHARS.indexOf(this.foText
                    .charAt(this.tempStart - 1)) >= 0) {
                // the last character could be used as a line break
                // append new elements to oldList
                oldListIterator = oldList.listIterator(oldList.size());
                oldListIterator.add(new KnuthPenalty(0,
                        KnuthPenalty.FLAGGED_PENALTY, true,
                        this.auxiliaryPosition, false));
                oldListIterator.add(new KnuthGlue(this.letterSpaceIPD,
                        this.auxiliaryPosition, false));
            } else if (this.letterSpaceIPD.isStiff()) {
                // constant letter space: replace the box
                oldListIterator.set(new KnuthInlineBox(areaInfo.areaIPD
                        .getOpt(), this.alignmentContext, pos, false));
            } else {
                // adjustable letter space: replace the glue
                oldListIterator.next(); // this would return the penalty element
                oldListIterator.next(); // this would return the glue element
                oldListIterator.set(new KnuthGlue(this.letterSpaceIPD
                        .mult(areaInfo.letterSpaceCount),
                        this.auxiliaryPosition, true));
            }
        }
        return oldList;
    }

    /**
     * Removes the <code>AreaInfo</code> object represented by the given
     * elements, so that it won't generate any element when
     * <code>getChangedKnuthElements</code> is called.
     *
     * @param oldList
     *            the elements representing the word space
     */
    @Override
    public void removeWordSpace(final List oldList) {
        // find the element storing the Position whose value
        // points to the AreaInfo object
        final ListIterator oldListIterator = oldList.listIterator();
        if (((KnuthElement) ((LinkedList) oldList).getFirst()).isPenalty()) {
            // non breaking space: oldList starts with a penalty
            oldListIterator.next();
        }
        if (oldList.size() > 2) {
            // alignment is either center, start or end:
            // the first two elements does not store the needed Position
            oldListIterator.next();
            oldListIterator.next();
        }
        final KnuthElement knuthElement = (KnuthElement) oldListIterator.next();
        final int leafValue = ((LeafPosition) knuthElement.getPosition())
                .getLeafPos();
        // only the last word space can be a trailing space!
        if (leafValue == this.areaInfos.size() - 1) {
            this.areaInfos.remove(leafValue);
        } else {
            log.error("trying to remove a non-trailing word space");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void hyphenate(final Position pos, final HyphContext hyphContext) {
        final AreaInfo areaInfo = getAreaInfo(((LeafPosition) pos).getLeafPos());
        int startIndex = areaInfo.startIndex;
        int stopIndex;
        boolean nothingChanged = true;
        final Font font = areaInfo.font;

        while (startIndex < areaInfo.breakIndex) {
            MinOptMax newIPD = MinOptMax.ZERO;
            boolean hyphenFollows;

            stopIndex = startIndex + hyphContext.getNextHyphPoint();
            if (hyphContext.hasMoreHyphPoints()
                    && stopIndex <= areaInfo.breakIndex) {
                // stopIndex is the index of the first character
                // after a hyphenation point
                hyphenFollows = true;
            } else {
                // there are no more hyphenation points,
                // or the next one is after areaInfo.breakIndex
                hyphenFollows = false;
                stopIndex = areaInfo.breakIndex;
            }

            hyphContext.updateOffset(stopIndex - startIndex);

            // log.info("Word: " + new String(textArray, startIndex, stopIndex -
            // startIndex));
            for (int i = startIndex; i < stopIndex; ++i) {
                final char ch = this.foText.charAt(i);
                newIPD = newIPD.plus(font.getCharWidth(ch));
                // if (i > startIndex) {
                if (i < stopIndex) {
                    MinOptMax letterAdjust = this.letterAdjustArray[i + 1];
                    if (i == stopIndex - 1 && hyphenFollows) {
                        // the letter adjust here needs to be handled further
                        // down during
                        // element generation because it depends on hyph/no-hyph
                        // condition
                        letterAdjust = null;
                    }
                    if (letterAdjust != null) {
                        newIPD = newIPD.plus(letterAdjust);
                    }
                }
            }

            // add letter spaces
            final boolean isWordEnd = stopIndex == areaInfo.breakIndex
                    && areaInfo.letterSpaceCount < areaInfo.getCharLength();
            final int letterSpaceCount = isWordEnd ? stopIndex - startIndex - 1
                    : stopIndex - startIndex;

            assert letterSpaceCount >= 0;
            newIPD = newIPD.plus(this.letterSpaceIPD.mult(letterSpaceCount));

            if (!(nothingChanged && stopIndex == areaInfo.breakIndex && !hyphenFollows)) {
                // the new AreaInfo object is not equal to the old one
                this.changeList
                        .add(new PendingChange(new AreaInfo(startIndex,
                                stopIndex, 0, letterSpaceCount, newIPD,
                                hyphenFollows, false, false, font),
                                ((LeafPosition) pos).getLeafPos()));
                nothingChanged = false;
            }
            startIndex = stopIndex;
        }
        this.hasChanged |= !nothingChanged;
    }

    /** {@inheritDoc} */
    @Override
    public boolean applyChanges(final List oldList) {
        setFinished(false);

        if (!this.changeList.isEmpty()) {
            int areaInfosAdded = 0;
            int areaInfosRemoved = 0;
            int oldIndex = -1, changeIndex;
            PendingChange currChange;
            final ListIterator<PendingChange> changeListIterator = this.changeList
                    .listIterator();
            while (changeListIterator.hasNext()) {
                currChange = changeListIterator.next();
                if (currChange.index == oldIndex) {
                    areaInfosAdded++;
                    changeIndex = currChange.index + areaInfosAdded
                            - areaInfosRemoved;
                } else {
                    areaInfosRemoved++;
                    areaInfosAdded++;
                    oldIndex = currChange.index;
                    changeIndex = currChange.index + areaInfosAdded
                            - areaInfosRemoved;
                    this.areaInfos.remove(changeIndex);
                }
                this.areaInfos.add(changeIndex, currChange.areaInfo);
            }
            this.changeList.clear();
        }

        this.returnedIndex = 0;
        return this.hasChanged;
    }

    /** {@inheritDoc} */
    @Override
    public List<ListElement> getChangedKnuthElements(final List oldList,
            final int alignment) {
        if (isFinished()) {
            return null;
        }

        final LinkedList<ListElement> returnList = new LinkedList<>();

        while (this.returnedIndex < this.areaInfos.size()) {
            final AreaInfo areaInfo = getAreaInfo(this.returnedIndex);
            if (areaInfo.wordSpaceCount == 0) {
                // areaInfo refers either to a word or a word fragment
                addElementsForAWordFragment(returnList, alignment, areaInfo,
                        this.returnedIndex);
            } else {
                // areaInfo refers to a space
                addElementsForASpace(returnList, alignment, areaInfo,
                        this.returnedIndex);
            }
            this.returnedIndex++;
        }
        setFinished(true);
        // ElementListObserver.observe(returnList, "text-changed", null);
        return returnList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWordChars(final Position pos) {
        final int leafValue = ((LeafPosition) pos).getLeafPos();
        if (leafValue != -1) {
            final AreaInfo areaInfo = getAreaInfo(leafValue);
            final StringBuilder buffer = new StringBuilder(
                    areaInfo.getCharLength());
            for (int i = areaInfo.startIndex; i < areaInfo.breakIndex; ++i) {
                buffer.append(this.foText.charAt(i));
            }
            return buffer.toString();
        } else {
            return "";
        }
    }

    private void addElementsForASpace(final List<ListElement> baseList,
            final int alignment, final AreaInfo areaInfo, final int leafValue) {
        final LeafPosition mainPosition = new LeafPosition(this, leafValue);

        if (!areaInfo.breakOppAfter) {
            // a non-breaking space
            if (alignment == Constants.EN_JUSTIFY) {
                // the space can stretch and shrink, and must be preserved
                // when starting a line
                baseList.add(makeAuxiliaryZeroWidthBox());
                baseList.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
                baseList.add(new KnuthGlue(areaInfo.areaIPD, mainPosition,
                        false));
            } else {
                // the space does not need to stretch or shrink, and must be
                // preserved when starting a line
                baseList.add(new KnuthInlineBox(areaInfo.areaIPD.getOpt(),
                        null, mainPosition, true));
            }
        } else {
            if (this.foText.charAt(areaInfo.startIndex) != CharUtilities.SPACE
                    || this.foText.getWhitespaceTreatment() == Constants.EN_PRESERVE) {
                // a breaking space that needs to be preserved
                baseList.addAll(getElementsForBreakingSpace(alignment,
                        areaInfo, this.auxiliaryPosition, 0, mainPosition,
                        areaInfo.areaIPD.getOpt(), true));
            } else {
                // a (possible block) of breaking spaces
                baseList.addAll(getElementsForBreakingSpace(alignment,
                        areaInfo, mainPosition, areaInfo.areaIPD.getOpt(),
                        this.auxiliaryPosition, 0, false));
            }
        }
    }

    private List<KnuthElement> getElementsForBreakingSpace(final int alignment,
            final AreaInfo areaInfo, final Position pos2,
            final int p2WidthOffset, final Position pos3,
            final int p3WidthOffset, final boolean skipZeroCheck) {
        final List<KnuthElement> elements = new ArrayList<>();

        switch (alignment) {
        case EN_CENTER:
            // centered text:
            // if the second element is chosen as a line break these
            // elements
            // add a constant amount of stretch at the end of a line and at
            // the
            // beginning of the next one, otherwise they don't add any
            // stretch
            elements.add(new KnuthGlue(this.lineEndBAP,
                    3 * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0,
                    this.auxiliaryPosition, false));
            elements.add(makeZeroWidthPenalty(0));
            elements.add(new KnuthGlue(p2WidthOffset
                    - (this.lineStartBAP + this.lineEndBAP), -6
                    * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0, pos2, false));
            elements.add(makeAuxiliaryZeroWidthBox());
            elements.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
            elements.add(new KnuthGlue(this.lineStartBAP + p3WidthOffset,
                    3 * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0, pos3, false));
            break;

        case EN_START: // fall through
        case EN_END:
            // left- or right-aligned text:
            // if the second element is chosen as a line break these
            // elements
            // add a constant amount of stretch at the end of a line,
            // otherwise
            // they don't add any stretch
            if (skipZeroCheck || this.lineStartBAP != 0 || this.lineEndBAP != 0) {
                elements.add(new KnuthGlue(this.lineEndBAP,
                        3 * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0,
                        this.auxiliaryPosition, false));
                elements.add(makeZeroWidthPenalty(0));
                elements.add(new KnuthGlue(p2WidthOffset
                        - (this.lineStartBAP + this.lineEndBAP), -3
                        * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0, pos2, false));
                elements.add(makeAuxiliaryZeroWidthBox());
                elements.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
                elements.add(new KnuthGlue(this.lineStartBAP + p3WidthOffset,
                        0, 0, pos3, false));
            } else {
                elements.add(new KnuthGlue(0,
                        3 * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0,
                        this.auxiliaryPosition, false));
                elements.add(makeZeroWidthPenalty(0));
                elements.add(new KnuthGlue(areaInfo.areaIPD.getOpt(), -3
                        * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0, pos2, false));
            }
            break;

        case EN_JUSTIFY:
            // justified text:
            // the stretch and shrink depends on the space width
            elements.addAll(getElementsForJustifiedText(areaInfo, pos2,
                    p2WidthOffset, pos3, p3WidthOffset, skipZeroCheck,
                    areaInfo.areaIPD.getShrink()));
            break;

        default:
            // last line justified, the other lines unjustified:
            // use only the space stretch
            elements.addAll(getElementsForJustifiedText(areaInfo, pos2,
                    p2WidthOffset, pos3, p3WidthOffset, skipZeroCheck, 0));
        }
        return elements;
    }

    private List<KnuthElement> getElementsForJustifiedText(
            final AreaInfo areaInfo, final Position pos2,
            final int p2WidthOffset, final Position pos3,
            final int p3WidthOffset, final boolean skipZeroCheck,
            final int shrinkability) {

        final int stretchability = areaInfo.areaIPD.getStretch();

        final List<KnuthElement> elements = new ArrayList<>();
        if (skipZeroCheck || this.lineStartBAP != 0 || this.lineEndBAP != 0) {
            elements.add(new KnuthGlue(this.lineEndBAP, 0, 0,
                    this.auxiliaryPosition, false));
            elements.add(makeZeroWidthPenalty(0));
            elements.add(new KnuthGlue(p2WidthOffset
                    - (this.lineStartBAP + this.lineEndBAP), stretchability,
                    shrinkability, pos2, false));
            elements.add(makeAuxiliaryZeroWidthBox());
            elements.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
            elements.add(new KnuthGlue(this.lineStartBAP + p3WidthOffset, 0, 0,
                    pos3, false));
        } else {
            elements.add(new KnuthGlue(areaInfo.areaIPD.getOpt(),
                    stretchability, shrinkability, pos2, false));
        }
        return elements;
    }

    private void addElementsForAWordFragment(final List<ListElement> baseList,
            final int alignment, final AreaInfo areaInfo, final int leafValue) {
        final LeafPosition mainPosition = new LeafPosition(this, leafValue);

        // if the last character of the word fragment is '-' or '/',
        // the fragment could end a line; in this case, it loses one
        // of its letter spaces;
        final boolean suppressibleLetterSpace = areaInfo.breakOppAfter
                && !areaInfo.isHyphenated;

        if (this.letterSpaceIPD.isStiff()) {
            // constant letter spacing
            baseList.add(new KnuthInlineBox(
                    suppressibleLetterSpace ? areaInfo.areaIPD.getOpt()
                            - this.letterSpaceIPD.getOpt() : areaInfo.areaIPD
                            .getOpt(), this.alignmentContext,
                    notifyPos(mainPosition), false));
        } else {
            // adjustable letter spacing
            final int unsuppressibleLetterSpaces = suppressibleLetterSpace ? areaInfo.letterSpaceCount - 1
                    : areaInfo.letterSpaceCount;
            baseList.add(new KnuthInlineBox(areaInfo.areaIPD.getOpt()
                    - areaInfo.letterSpaceCount * this.letterSpaceIPD.getOpt(),
                    this.alignmentContext, notifyPos(mainPosition), false));
            baseList.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
            baseList.add(new KnuthGlue(this.letterSpaceIPD
                    .mult(unsuppressibleLetterSpaces), this.auxiliaryPosition,
                    true));
            baseList.add(makeAuxiliaryZeroWidthBox());
        }

        // extra-elements if the word fragment is the end of a syllable,
        // or it ends with a character that can be used as a line break
        if (areaInfo.isHyphenated) {
            MinOptMax widthIfNoBreakOccurs = null;
            if (areaInfo.breakIndex < this.foText.length()) {
                // Add in kerning in no-break condition
                widthIfNoBreakOccurs = this.letterAdjustArray[areaInfo.breakIndex];
            }
            // if (areaInfo.breakIndex)

            // the word fragment ends at the end of a syllable:
            // if a break occurs the content width increases,
            // otherwise nothing happens
            addElementsForAHyphen(baseList, alignment, this.hyphIPD,
                    widthIfNoBreakOccurs, areaInfo.breakOppAfter
                            && areaInfo.isHyphenated);
        } else if (suppressibleLetterSpace) {
            // the word fragment ends with a character that acts as a hyphen
            // if a break occurs the width does not increase,
            // otherwise there is one more letter space
            addElementsForAHyphen(baseList, alignment, 0, this.letterSpaceIPD,
                    true);
        }
    }

    private void addElementsForAHyphen(final List<ListElement> baseList,
            final int alignment, final int widthIfBreakOccurs,
            MinOptMax widthIfNoBreakOccurs, final boolean unflagged) {

        if (widthIfNoBreakOccurs == null) {
            widthIfNoBreakOccurs = MinOptMax.ZERO;
        }

        switch (alignment) {
        case EN_CENTER:
            // centered text:
            baseList.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
            baseList.add(new KnuthGlue(this.lineEndBAP,
                    3 * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0,
                    this.auxiliaryPosition, true));
            baseList.add(new KnuthPenalty(this.hyphIPD,
                    unflagged ? TextLayoutManager.SOFT_HYPHEN_PENALTY
                            : KnuthPenalty.FLAGGED_PENALTY, !unflagged,
                    this.auxiliaryPosition, false));
            baseList.add(new KnuthGlue(-(this.lineEndBAP + this.lineStartBAP),
                    -6 * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0,
                    this.auxiliaryPosition, false));
            baseList.add(makeAuxiliaryZeroWidthBox());
            baseList.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
            baseList.add(new KnuthGlue(this.lineStartBAP,
                    3 * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0,
                    this.auxiliaryPosition, true));
            break;

        case EN_START: // fall through
        case EN_END:
            // left- or right-aligned text:
            if (this.lineStartBAP != 0 || this.lineEndBAP != 0) {
                baseList.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
                baseList.add(new KnuthGlue(this.lineEndBAP,
                        3 * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0,
                        this.auxiliaryPosition, false));
                baseList.add(new KnuthPenalty(widthIfBreakOccurs,
                        unflagged ? TextLayoutManager.SOFT_HYPHEN_PENALTY
                                : KnuthPenalty.FLAGGED_PENALTY, !unflagged,
                        this.auxiliaryPosition, false));
                baseList.add(new KnuthGlue(widthIfNoBreakOccurs.getOpt()
                        - (this.lineStartBAP + this.lineEndBAP), -3
                        * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0,
                        this.auxiliaryPosition, false));
                baseList.add(makeAuxiliaryZeroWidthBox());
                baseList.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
                baseList.add(new KnuthGlue(this.lineStartBAP, 0, 0,
                        this.auxiliaryPosition, false));
            } else {
                baseList.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
                baseList.add(new KnuthGlue(0,
                        3 * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0,
                        this.auxiliaryPosition, false));
                baseList.add(new KnuthPenalty(widthIfBreakOccurs,
                        unflagged ? TextLayoutManager.SOFT_HYPHEN_PENALTY
                                : KnuthPenalty.FLAGGED_PENALTY, !unflagged,
                        this.auxiliaryPosition, false));
                baseList.add(new KnuthGlue(widthIfNoBreakOccurs.getOpt(), -3
                        * LineLayoutManager.DEFAULT_SPACE_WIDTH, 0,
                        this.auxiliaryPosition, false));
            }
            break;

        default:
            // justified text, or last line justified:
            // just a flagged penalty
            if (this.lineStartBAP != 0 || this.lineEndBAP != 0) {
                baseList.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
                baseList.add(new KnuthGlue(this.lineEndBAP, 0, 0,
                        this.auxiliaryPosition, false));
                baseList.add(new KnuthPenalty(widthIfBreakOccurs,
                        unflagged ? TextLayoutManager.SOFT_HYPHEN_PENALTY
                                : KnuthPenalty.FLAGGED_PENALTY, !unflagged,
                        this.auxiliaryPosition, false));
                // extra elements representing a letter space that is
                // suppressed
                // if a break occurs
                if (widthIfNoBreakOccurs.isNonZero()) {
                    baseList.add(new KnuthGlue(widthIfNoBreakOccurs.getOpt()
                            - (this.lineStartBAP + this.lineEndBAP),
                            widthIfNoBreakOccurs.getStretch(),
                            widthIfNoBreakOccurs.getShrink(),
                            this.auxiliaryPosition, false));
                } else {
                    baseList.add(new KnuthGlue(
                            -(this.lineStartBAP + this.lineEndBAP), 0, 0,
                            this.auxiliaryPosition, false));
                }
                baseList.add(makeAuxiliaryZeroWidthBox());
                baseList.add(makeZeroWidthPenalty(KnuthElement.INFINITE));
                baseList.add(new KnuthGlue(this.lineStartBAP, 0, 0,
                        this.auxiliaryPosition, false));
            } else {
                baseList.add(new KnuthPenalty(widthIfBreakOccurs,
                        unflagged ? TextLayoutManager.SOFT_HYPHEN_PENALTY
                                : KnuthPenalty.FLAGGED_PENALTY, !unflagged,
                        this.auxiliaryPosition, false));
                // extra elements representing a letter space that is
                // suppressed
                // if a break occurs
                if (widthIfNoBreakOccurs.isNonZero()) {
                    baseList.add(new KnuthGlue(widthIfNoBreakOccurs,
                            this.auxiliaryPosition, false));
                }
            }
        }

    }

}
