package eu.cloudnetservice.cloudnet.repository.console.animation.questionlist.answer;

import de.dytanic.cloudnet.common.Validate;
import de.dytanic.cloudnet.common.language.LanguageManager;
import eu.cloudnetservice.cloudnet.repository.console.animation.questionlist.QuestionAnswerType;

import java.util.Collection;

public class QuestionAnswerTypeDouble implements QuestionAnswerType<Double> {

    @Override
    public boolean isValidInput(String input) {
        return Validate.testStringParseToDouble(input);
    }

    @Override
    public Double parse(String input) {
        return Double.parseDouble(input);
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return null;
    }

    @Override
    public String getInvalidInputMessage(String input) {
        return LanguageManager.getMessage("ca-question-list-invalid-double");
    }

}
