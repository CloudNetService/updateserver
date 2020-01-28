package eu.cloudnetservice.cloudnet.repository.console.animation.questionlist.answer;

import eu.cloudnetservice.cloudnet.repository.console.animation.questionlist.QuestionAnswerType;

import java.util.Collection;

public class QuestionAnswerTypeString implements QuestionAnswerType<String> {

    @Override
    public boolean isValidInput(String input) {
        return true;
    }

    @Override
    public String parse(String input) {
        return input;
    }

    @Override
    public Collection<String> getPossibleAnswers() {
        return null;
    }

}
