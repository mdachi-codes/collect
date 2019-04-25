/*
 * Copyright 2017 Nafundi
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

package org.odk.collect.android.utilities;

import android.content.Context;
import android.content.res.Configuration;

import org.javarosa.core.model.Constants;
import org.javarosa.core.model.SelectChoice;
import org.javarosa.core.model.data.DateData;
import org.javarosa.core.model.data.DateTimeData;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.MultipleItemsData;
import org.javarosa.core.model.data.helper.Selection;
import org.javarosa.form.api.FormEntryPrompt;
import org.odk.collect.android.dao.ItemsetDao;
import org.odk.collect.android.logic.FormController;
import org.odk.collect.android.widgets.WidgetFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Date;
import java.util.List;

import timber.log.Timber;

import static org.javarosa.core.model.Constants.DATATYPE_TEXT;

public class FormEntryPromptUtils {

    private FormEntryPromptUtils() {
    }

    public static String getAnswerText(FormEntryPrompt fep, Context context, FormController formController) {
        IAnswerData data = fep.getAnswerValue();
        final String appearance = fep.getQuestion().getAppearanceAttr();

        if (data instanceof MultipleItemsData) {
            StringBuilder answerText = new StringBuilder();
            List<Selection> values = (List<Selection>) data.getValue();
            for (Selection value : values) {
                if (fep.getControlType() == Constants.CONTROL_RANK) {
                    answerText
                            .append(values.indexOf(value) + 1)
                            .append(". ");
                }
                answerText.append(fep.getSelectItemText(value));

                if ((values.size() - 1) > values.indexOf(value)) {
                    answerText.append(", ");
                }
            }

            return answerText.toString();
        }

        if (data instanceof DateTimeData) {
            return DateTimeUtils.getDateTimeLabel((Date) data.getValue(),
                    DateTimeUtils.getDatePickerDetails(appearance), true, context);
        }

        if (data instanceof DateData) {
            return DateTimeUtils.getDateTimeLabel((Date) data.getValue(),
                    DateTimeUtils.getDatePickerDetails(appearance), false, context);
        }

        if (data != null && appearance != null && appearance.contains("thousands-sep")) {
            try {
                final BigDecimal answerAsDecimal = new BigDecimal(fep.getAnswerText());

                DecimalFormat df = new DecimalFormat();
                df.setGroupingSize(3);
                df.setGroupingUsed(true);
                df.setMaximumFractionDigits(Integer.MAX_VALUE);

                // Use . as decimal marker for consistency with DecimalWidget
                DecimalFormatSymbols customFormat = new DecimalFormatSymbols();
                customFormat.setDecimalSeparator('.');

                if (df.getDecimalFormatSymbols().getGroupingSeparator() == '.') {
                    customFormat.setGroupingSeparator(' ');
                }

                df.setDecimalFormatSymbols(customFormat);

                return df.format(answerAsDecimal);
            } catch (NumberFormatException e) {
                return fep.getAnswerText();
            }
        }

        if (data != null && data.getValue() != null && fep.getDataType() == DATATYPE_TEXT
                && fep.getQuestion().getAdditionalAttribute(null, "query") != null) { // ItemsetWidget

            String language = "";
            if (formController.getLanguages() != null && formController.getLanguages().length > 0) {
                language = formController.getLanguage();
            }

            return new ItemsetDao().getItemLabel(fep.getAnswerValue().getDisplayText(), formController.getMediaFolder().getAbsolutePath(), language);
        }

        return fep.getAnswerText();
    }

    public static String markQuestionIfIsRequired(String questionText, boolean isRequired) {
        if (isRequired) {
            if (questionText == null) {
                questionText = "";
            }
            questionText = "<span style=\"color:#F44336\">*</span> " + questionText;
        }

        return questionText;
    }

    public static CharSequence getItemText(FormEntryPrompt formEntryPrompt, SelectChoice selectChoice) {
        String choiceName = formEntryPrompt.getSelectChoiceText(selectChoice);
        return choiceName != null ? TextUtils.textToHtml(choiceName) : "";
    }

    public static int getNumberOfColumns(FormEntryPrompt formEntryPrompt, Context context) {
        int numColumns = 1;
        String appearance = WidgetFactory.getAppearance(formEntryPrompt);
        if (appearance.contains("columns") && !appearance.contains("columns-")) {
            switch (context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) {
                case Configuration.SCREENLAYOUT_SIZE_SMALL:
                    numColumns = 2;
                    break;
                case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                    numColumns = 3;
                    break;
                case Configuration.SCREENLAYOUT_SIZE_LARGE:
                    numColumns = 4;
                    break;
                case Configuration.SCREENLAYOUT_SIZE_XLARGE:
                    numColumns = 5;
                    break;
                default:
                    numColumns = 3;
            }
        } else if (appearance.contains("columns-")) {
            String columnsAppearance = "columns-";

            if (appearance.contains(columnsAppearance)) {
                try {
                    appearance =
                            appearance.substring(appearance.indexOf(columnsAppearance), appearance.length());
                    int idx = appearance.indexOf(columnsAppearance);
                    if (idx != -1) {
                        String substringFromNumColumns = appearance.substring(idx + columnsAppearance.length());
                        numColumns = Integer.parseInt(substringFromNumColumns.substring(0, substringFromNumColumns.contains(" ")
                                ? substringFromNumColumns.indexOf(' ')
                                : substringFromNumColumns.length()));
                    }
                } catch (Exception e) {
                    Timber.e("Exception parsing columns");
                }
            }
        }
        return numColumns;
    }
}
