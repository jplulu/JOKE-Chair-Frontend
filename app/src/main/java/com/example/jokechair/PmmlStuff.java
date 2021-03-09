package com.example.jokechair;

import org.dmg.pmml.FieldName;
import org.jpmml.evaluator.Computable;
import org.jpmml.evaluator.Evaluator;
import org.jpmml.evaluator.FieldValue;
import org.jpmml.evaluator.InputField;
import org.jpmml.evaluator.LoadingModelEvaluatorBuilder;
import org.jpmml.evaluator.TargetField;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;

public class PmmlStuff {
    public void evaluate() throws IOException, SAXException, JAXBException {
        Evaluator evaluator = new LoadingModelEvaluatorBuilder()
                .load(new File("logistic_regression.pmml"))
                .build();
        evaluator.verify();

        List<? extends InputField> inputFields = evaluator.getInputFields();
        System.out.println(inputFields);

        List<? extends TargetField> targetFields = evaluator.getTargetFields();
        System.out.println(targetFields);

        double[] inputRecord = {527,541,497,526,83,164,327,273};
        normalizeInput(inputRecord);
        System.out.println(Arrays.toString(inputRecord));

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
        System.out.println("Result: " + targetValue);
    }

    private void normalizeInput(double[] inputRecord) {
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
