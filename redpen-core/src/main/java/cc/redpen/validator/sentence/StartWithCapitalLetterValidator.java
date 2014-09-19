package cc.redpen.validator.sentence;

import cc.redpen.RedPenException;
import cc.redpen.ValidationError;
import cc.redpen.model.Sentence;
import cc.redpen.util.ResourceLoader;
import cc.redpen.util.WordListExtractor;
import cc.redpen.validator.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Check if the input sentence start with a capital letter.
 */
final public class StartWithCapitalLetterValidator extends Validator<Sentence> {
    private static final String DEFAULT_RESOURCE_PATH = "default-resources/capital-letter-exception-list";
    private Set<String> whiteList;
    private static final Logger LOG =
            LoggerFactory.getLogger(SpellingValidator.class);

    public boolean addWhiteList(String item) {
        return whiteList.add(item);
    }

    public StartWithCapitalLetterValidator() {
        this.whiteList = new HashSet<>();
    }

    @Override
    public List<ValidationError> validate(Sentence block) {
        List<ValidationError> validationErrors = new ArrayList<>();
        String content = block.content;
        String[] words = content.split(" ");

        if (this.whiteList.contains(words[0])) {
            return validationErrors;
        }

        char headChar = '≡';
        for (char ch: content.toCharArray()) {
            if (ch != ' ') {
                headChar = ch;
            }
        }

        if (headChar == '≡') {
            return validationErrors;
        }

        headChar = content.charAt(0);
        if (Character.isLowerCase(headChar)) {
            validationErrors.add(createValidationError(block, headChar));
        }
        return validationErrors;
    }

    @Override
    protected void init() throws RedPenException {
        WordListExtractor extractor = new WordListExtractor();
        ResourceLoader loader = new ResourceLoader(extractor);

        LOG.info("Loading default capital letter exception dictionary ");
        String defaultDictionaryFile = DEFAULT_RESOURCE_PATH
                + "/default-capital-case-exception-list.dat";
        try {
            loader.loadInternalResource(defaultDictionaryFile);
        } catch (IOException e) {
            throw new RedPenException("Failed to load default dictionary.", e);
        }
        LOG.info("Succeeded to load default dictionary.");

        Optional<String> confFile = getConfigAttribute("dictionary");
        confFile.ifPresent(f -> {
            LOG.info("user dictionary file is " + f);
            try {
                loader.loadExternalFile(f);
            } catch (IOException e) {
                LOG.error("Failed to load user dictionary.");
                return;
            }
            LOG.info("Succeeded to load specified user dictionary.");
        });

        whiteList = extractor.get();
    }

    @Override
    public String toString() {
        return "StartWithCapitalLetterValidator{" +
                "whiteList=" + whiteList +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StartWithCapitalLetterValidator that = (StartWithCapitalLetterValidator) o;

        return !(whiteList != null ? !whiteList.equals(that.whiteList) : that.whiteList != null);

    }

    @Override
    public int hashCode() {
        return whiteList != null ? whiteList.hashCode() : 0;
    }
}
