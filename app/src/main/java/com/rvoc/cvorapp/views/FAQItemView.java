package com.rvoc.cvorapp.views;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import androidx.annotation.Nullable;
import com.google.android.material.expandable.ExpandableWidget;
import com.google.android.material.expandable.ExpandableWidgetHelper;
import com.google.android.material.textview.MaterialTextView;
import com.rvoc.cvorapp.R;


public class FAQItemView extends LinearLayout implements ExpandableWidget {

    private final ExpandableWidgetHelper expandableWidgetHelper;
    private final MaterialTextView faqQuestion;
    private final MaterialTextView faqAnswer;

    public FAQItemView(Context context) {
        this(context, null);
    }

    public FAQItemView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FAQItemView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);

        // Initialize ExpandableWidgetHelper
        expandableWidgetHelper = new ExpandableWidgetHelper(this);

        // Inflate layout
        LayoutInflater.from(context).inflate(R.layout.item_faq, this, true);
        faqQuestion = findViewById(R.id.faq_question);
        faqAnswer = findViewById(R.id.faq_answer);

        // Initially collapsed
        faqAnswer.setVisibility(GONE);

        // Handle click event for expansion
        faqQuestion.setOnClickListener(v -> toggleExpansion());
    }

    private void toggleExpansion() {
        setExpanded(!isExpanded());
    }

    @Override
    public boolean isExpanded() {
        return expandableWidgetHelper.isExpanded();
    }

    @Override
    public boolean setExpanded(boolean expanded) {
        if (expanded) {
            faqAnswer.setVisibility(View.VISIBLE);
            faqQuestion.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_expand_less_24, 0);
        } else {
            faqAnswer.setVisibility(View.GONE);
            faqQuestion.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.baseline_expand_more_24, 0);
        }
        return expandableWidgetHelper.setExpanded(expanded);
    }

    public void setQuestion(String question) {
        faqQuestion.setText(question);
    }

    public void setAnswer(String answer) {
        faqAnswer.setText(answer);
    }
}
