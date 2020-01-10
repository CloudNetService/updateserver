package eu.cloudnetservice.cloudnet.repository.faq;

import java.util.Map;
import java.util.UUID;

public class FAQEntry {

    private UUID uniqueId;
    private String language;
    private String parentVersionName;
    private long creationTime;
    private String question;
    private String answer;
    private String creator;
    private Map<String, Object> properties;

    public FAQEntry(UUID uniqueId, String language, String parentVersionName, long creationTime, String question, String answer, String creator, Map<String, Object> properties) {
        this.uniqueId = uniqueId;
        this.language = language;
        this.parentVersionName = parentVersionName;
        this.creationTime = creationTime;
        this.question = question;
        this.answer = answer;
        this.creator = creator;
        this.properties = properties;
    }

    public UUID getUniqueId() {
        return this.uniqueId;
    }

    public String getLanguage() {
        return this.language;
    }

    public String getParentVersionName() {
        return this.parentVersionName;
    }

    public long getCreationTime() {
        return this.creationTime;
    }

    public String getCreator() {
        return creator;
    }

    public String getQuestion() {
        return this.question;
    }

    public String getAnswer() {
        return this.answer;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }
}
