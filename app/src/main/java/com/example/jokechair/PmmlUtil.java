package com.example.jokechair;

import android.content.Context;
import android.util.Log;

import org.dmg.pmml.FieldName;
import org.dmg.pmml.PMML;
import org.jpmml.evaluator.Computable;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.ModelEvaluatorBuilder;
import org.jpmml.evaluator.TargetField;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PmmlUtil {
    private static final String TAG = "PmmlUtil";

    public void createModelFile(Context context, String fileName, String jsonString) {
        try {
            System.out.println("HERE");
            FileOutputStream fos = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            if (jsonString != null) {
                fos.write(jsonString.getBytes());
            }
        } catch (IOException e) {
            Log.e(TAG, String.valueOf(e));
        }
    }

    public InputStream readModelFile(Context context, String fileName) {
        try {
            return context.openFileInput(fileName);
        } catch (FileNotFoundException e) {
            Log.e(TAG, String.valueOf(e));
            return null;
        }
    }

    public boolean isModelPresent(Context context, String fileName) {
        String path = context.getFilesDir().getAbsolutePath() + "/" + fileName;
        File file = new File(path);
        return file.exists();
    }

    public Evaluator createEvaluator(InputStream is) throws IOException {
        PMML pmml = org.jpmml.model.jackson.JacksonUtil.readPMML(is);
        //PMML pmml = org.jpmml.model.PMMLUtil.unmarshal(is);
        ModelEvaluatorBuilder modelEvaluatorBuilder = new ModelEvaluatorBuilder(pmml);
        Evaluator evaluator = modelEvaluatorBuilder.build();
        evaluator.verify();
        Log.d(TAG, "Evaluator created");

        return evaluator;
    }

    public void testEvaluator(Evaluator evaluator, double[] inputRecord) {
        List<? extends InputField> inputFields = evaluator.getInputFields();

        List<? extends TargetField> targetFields = evaluator.getTargetFields();

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
