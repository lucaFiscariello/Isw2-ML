package com.fiscariello;

import com.fiscariello.ML.Weka;
import com.fiscariello.ML.Weka.OutputWeka;
import com.fiscariello.dataset.DatasetCreator;

import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;
import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.Instances;
import weka.core.Debug.Random;
import weka.core.converters.CSVLoader;

import java.io.File;

public class MainMetrics {
    public static void main(String[] args) throws Exception {
        Table dataset = Table.read().csv("DatasetMetrics.csv");
        OutputWeka output;

        String datasetFiltrCSV = "DatasetMetricsFiltr.cvs";
        String namecol1 = DatasetCreator.NameColumnDataset.Loc_Weighted_Methods.toString();
        String namecol2 = DatasetCreator.NameColumnDataset.Variance_Loc_Added.toString();

        Table datasetFiltr = filterTable(dataset, namecol1, namecol2);
        datasetFiltr.write().csv(datasetFiltrCSV);
        output = evaluateModel(datasetFiltrCSV);
        System.out.println(output);

        Table datasetFiltr1 = filterTable(dataset, namecol1);
        datasetFiltr1.write().csv(datasetFiltrCSV);
        output = evaluateModel(datasetFiltrCSV);
        System.out.println(output);

        Table datasetFiltr2 = filterTable(dataset, namecol2);
        datasetFiltr2.write().csv(datasetFiltrCSV);
        output = evaluateModel(datasetFiltrCSV);
        System.out.println(output);

        dataset.write().csv(datasetFiltrCSV);
        output = evaluateModel(datasetFiltrCSV);
        System.out.println(output);

       
    }

    public static Table filterTable(Table table , String column1, String column2){
        Table datasetFiltr = Table.create();

        for(Column<?> column : table.columns()){
            if(!column.name().equals(column1) && !column.name().equals(column2)  )
                datasetFiltr.addColumns(column);
        }

        return datasetFiltr;
    }

    public static Table filterTable(Table table , String columnName){
        Table datasetFiltr = Table.create();

        for(Column<?> column : table.columns()){
            if(!column.name().equals(columnName) )
                datasetFiltr.addColumns(column);
        }

        return datasetFiltr;
    }

    public static OutputWeka evaluateModel(String datasetFiltrCSV) throws Exception{
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(datasetFiltrCSV));
        Instances data = loader.getDataSet();

        int numAttr = data.numAttributes();

        NaiveBayes naiveBayes = new NaiveBayes();
        data.setClassIndex(data.numAttributes() - 1 );
        Evaluation eval = new Evaluation(data);
        eval.crossValidateModel(naiveBayes, data, 10, new Random(1));

        OutputWeka output = new Weka.OutputWeka(
			eval.areaUnderROC(1),
			eval.kappa(),
			eval.numTrueNegatives(numAttr - 1),
			eval.numTruePositives(numAttr - 1),
			eval.numFalseNegatives(numAttr - 1),
			eval.numFalsePositives(numAttr - 1),
			eval.recall(0),
			eval.precision(0)
		);

        return output;
    }
}
