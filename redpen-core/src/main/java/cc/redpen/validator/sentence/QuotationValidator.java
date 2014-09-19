/**
 * redpen: a text inspection tool
 * Copyright (C) 2014 Recruit Technologies Co., Ltd. and contributors
 * (see CONTRIBUTORS.md)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cc.redpen.validator.sentence;

import cc.redpen.RedPenException;
import cc.redpen.ValidationError;
import cc.redpen.config.Symbol;
import cc.redpen.model.Sentence;
import cc.redpen.symbol.DefaultSymbols;
import cc.redpen.validator.Validator;

import java.util.ArrayList;
import java.util.List;

/**
 * Validator to validate quotation characters.
 */
public class QuotationValidator extends Validator<Sentence> {

    private static final List<String> DEFAULT_EXCEPTION_SUFFIXES;

    static {
        DEFAULT_EXCEPTION_SUFFIXES = new ArrayList<>();
        DEFAULT_EXCEPTION_SUFFIXES.add("s "); // He's
        DEFAULT_EXCEPTION_SUFFIXES.add("m "); // I'm
    }

    private List<String> exceptionSuffixes = DEFAULT_EXCEPTION_SUFFIXES;
    private Symbol leftSingleQuotationMark;
    private Symbol rightSingleQuotationMark;
    private Symbol leftDoubleQuotationMark;
    private Symbol rightDoubleQuotationMark;
    private Character period;

    public QuotationValidator() {
        this(false);
    }

    QuotationValidator(boolean useAscii) {
        this(useAscii, DefaultSymbols.getInstance().get("FULL_STOP").getValue().charAt(0));
    }

    /**
     * Constructor.
     *
     * @param useAscii true when this validator uses ascii setting,
     *                   false uses the user-defined character settings
     * @param fullStop   period character
     */
    QuotationValidator(boolean useAscii, Character fullStop) {
        setUseAscii(useAscii);
        this.period = fullStop;
    }

    @Override
    public List<ValidationError> validate(Sentence sentence) {
        List<ValidationError> errors = new ArrayList<>();
        // validate single quotation
        List<ValidationError> result = this.checkQuotation(sentence,
                leftSingleQuotationMark, rightSingleQuotationMark);
        if (result != null) {
            errors.addAll(result);
        }

        // validate double quotation
        errors.addAll(this.checkQuotation(sentence,
                leftDoubleQuotationMark, rightDoubleQuotationMark));
        return errors;
    }

    @Override
    protected void init() throws RedPenException {
        this.period = DefaultSymbols.getInstance().get(
                "FULL_STOP").getValue().charAt(0);

        if (getSymbolTable().containsSymbol("FULL_STOP")) {
            this.period = getSymbolTable().getSymbol("FULL_STOP").getValue().charAt(0);
        }

        setUseAscii(getConfigAttributeAsBoolean("use_ascii", false));
    }

    private void setUseAscii(boolean useAscii) {
        if (useAscii) {
            leftSingleQuotationMark =
                    new Symbol("LEFT_SINGLE_QUOTATION_MARK", "'", "", true, false);
            rightSingleQuotationMark =
                    new Symbol("RIGHT_SINGLE_QUOTATION_MARK", "'", "", false, true);
            leftDoubleQuotationMark =
                    new Symbol("LEFT_DOUBLE_QUOTATION_MARK", "\"", "", true, false);
            rightDoubleQuotationMark =
                    new Symbol("RIGHT_DOUBLE_QUOTATION_MARK", "\"", "", false, true);
        } else {
            // single quotes
            leftSingleQuotationMark =
                    new Symbol("LEFT_SINGLE_QUOTATION_MARK", "‘", "", true, false);
            rightSingleQuotationMark =
                    new Symbol("RIGHT_SINGLE_QUOTATION_MARK", "’", "", false, true);
            leftDoubleQuotationMark =
                    new Symbol("LEFT_DOUBLE_QUOTATION_MARK", "“", "", true, false);
            rightDoubleQuotationMark =
                    new Symbol("RIGHT_DOUBLE_QUOTATION_MARK", "”", "", false, true);
        }
    }

    private List<ValidationError> checkQuotation(Sentence sentence,
                                                 Symbol leftQuotation,
                                                 Symbol rightQuotation) {
        String sentenceString = sentence.content;
        List<ValidationError> errors = new ArrayList<>();
        int leftPosition = 0;
        int rightPosition = 0;
        while (leftPosition >= 0 && rightPosition < sentenceString.length()) {
            leftPosition = this.getQuotePosition(sentenceString,
                    leftQuotation.getValue(),
                    rightPosition + 1);

            if (leftPosition < 0) {
                rightPosition = this.getQuotePosition(sentenceString,
                        rightQuotation.getValue(),
                        rightPosition + 1);
            } else {
                rightPosition = this.getQuotePosition(sentenceString,
                        rightQuotation.getValue(),
                        leftPosition + 1);
            }

            // validate if left and right quote pair exists
            if (leftPosition >= 0 && rightPosition < 0) {
                errors.add(createValidationError("RightExist", sentence));
                break;
            }

            if (leftPosition < 0 && rightPosition >= 0) {
                errors.add(createValidationError("LeftExist", sentence));
                break;
            }

            // validate inconsistent quotation marks
            int nextLeftPosition = this.getQuotePosition(sentenceString,
                    leftQuotation.getValue(),
                    leftPosition + 1);

            int nextRightPosition = this.getQuotePosition(sentenceString,
                    leftQuotation.getValue(),
                    leftPosition + 1);

            if (nextLeftPosition < rightPosition && nextLeftPosition > 0) {
                errors.add(createValidationError("DoubleRight", sentence));
            }

            if (nextRightPosition < leftPosition && nextRightPosition > 0) {
                errors.add(createValidationError("DoubleLeft", sentence));
            }

            // validate if quotes have white spaces
            if (leftPosition > 0 && leftQuotation.isNeedBeforeSpace()
                    && (sentenceString.charAt(leftPosition - 1) != ' ')) {
                errors.add(createValidationError("LeftSpace", sentence));
            }

            if (rightPosition > 0 && rightPosition < sentenceString.length() - 1
                    && rightQuotation.isNeedAfterSpace()
                    && (sentenceString.charAt(rightPosition + 1) != ' '
                    && sentenceString.charAt(rightPosition + 1) != this.period)) {
                errors.add(createValidationError("RightSpace", sentence));
            }
        }
        return errors;
    }

    private int getQuotePosition(String sentenceStr, String quote,
                                 int startPosition) {
        int quoteCandidatePosition = startPosition;
        boolean isFound;
        while (startPosition > -1) {
            quoteCandidatePosition = sentenceStr.indexOf(quote, startPosition);
            isFound = detectIsFound(sentenceStr, quoteCandidatePosition);
            if (isFound) {
                return quoteCandidatePosition;
            } else if (quoteCandidatePosition >= 0) { // exception case
                startPosition = quoteCandidatePosition + 1;
            } else {
                return -1;
            }
        }
        return quoteCandidatePosition;
    }

    private boolean detectIsFound(String sentenceStr, final int startPosition) {
        if (startPosition < 0) {
            return false;
        }

        for (String exceptionSuffix : exceptionSuffixes) {
            if (sentenceStr.startsWith(exceptionSuffix, startPosition + 1)) {
                return false;
            }
        }
        return true;
    }
}
