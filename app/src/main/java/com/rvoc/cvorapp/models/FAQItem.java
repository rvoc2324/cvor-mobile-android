package com.rvoc.cvorapp.models;

public class FAQItem {
    private final String question;
    private final String answer;
    private boolean isExpanded; // To track expansion state

    public FAQItem(String question, String answer) {
        this.question = question;
        this.answer = answer;
        this.isExpanded = false;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer() {
        return answer;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
}
