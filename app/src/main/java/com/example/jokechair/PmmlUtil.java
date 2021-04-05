package com.example.jokechair;

import android.util.Log;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Computable;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluatorBuilder;
import org.jpmml.evaluator.TargetField;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class PmmlUtil {
    private static final String TAG = "PmmlUtil";

    public Evaluator createEvaluator(InputStream is) throws IOException {
        PMML pmml = org.jpmml.model.jackson.JacksonUtil.readPMML(is);
        ModelEvaluatorBuilder modelEvaluatorBuilder = new ModelEvaluatorBuilder(pmml);
        Evaluator evaluator = modelEvaluatorBuilder.build();
        evaluator.verify();
        Log.d(TAG, "Evaluator created");

        return evaluator;
    }

    public void testEvaluator(Evaluator evaluator) {
        List<? extends InputField> inputFields = evaluator.getInputFields();

        List<? extends TargetField> targetFields = evaluator.getTargetFields();

        double[] inputRecord = {527,541,497,526,83,164,327,273};
        normalizeInput(inputRecord);

        Map<FieldName, FieldValue> arguments = new LinkedHashMap<>();

        int i = 0;
        for(InputField inputField : inputFields) {
            FieldName inputName = inputField.getName();
            Object rawValue = inputRecord[i];
            FieldValue inputValue = inputField.prepare(rawValue);
            arguments.put(inputName, inputValue);
            i++;
        }

        Map<FieldName, ?> results = evaluator.evaluate(arguments);
        Object targetValue = results.get(targetFields.get(0).getName());
        Computable computable = (Computable) targetValue;
        targetValue = computable.getResult();
        Log.d(TAG, "Result: " + targetValue);
    }

    public static void normalizeInput(double[] inputRecord) {
        double sum = 0.0, std = 0.0;
        for (double num : inputRecord) {
            sum += num;
        }
        double mean = sum / inputRecord.length;
        for(double num : inputRecord) {
            std += Math.pow(num - mean, 2);
        }
        std = Math.sqrt(std / inputRecord.length);

        for(int i = 0; i < inputRecord.length; i++) {
            inputRecord[i] = (inputRecord[i] - mean) / std;
        }
    }

}
