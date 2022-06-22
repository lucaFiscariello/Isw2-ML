package com.fiscariello;

import com.fiscariello.dataset.DatasetCreator;
import com.fiscariello.ml.Weka;
import com.fiscariello.ml.Weka.OutputWeka;

import tech.tablesaw.api.DoubleColumn;
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

        String projectname = "BOOKKEEPER";
        Table dataset = Table.read().csv("DatasetMetrics"+projectname+".csv");

        OutputWeka output;
        DatsetGenerator dg = new DatsetGenerator();

        String datasetFiltrCSV = "DatasetMetricsFiltr.csv";
        String namecol1 = DatasetCreator.NameColumnDataset.LOC_WEIGHTED_METHODS.toString();
        String namecol2 = DatasetCreator.NameColumnDataset.VARIANCE_LOC_ADDED.toString();

        Table datasetFiltr = filterTable(dataset, namecol1, namecol2);
        datasetFiltr.write().csv(datasetFiltrCSV);
        output = evaluateModel(datasetFiltrCSV);
        dg.addrow(output);

        Table datasetFiltr1 = filterTable(dataset, namecol1);
        datasetFiltr1.write().csv(datasetFiltrCSV);
        output = evaluateModel(datasetFiltrCSV);
        dg.addrow(output);

        Table datasetFiltr2 = filterTable(dataset, namecol2);
        datasetFiltr2.write().csv(datasetFiltrCSV);
        output = evaluateModel(datasetFiltrCSV);
        dg.addrow(output);

        dataset.write().csv(datasetFiltrCSV);
        output = evaluateModel(datasetFiltrCSV);
        dg.addrow(output);

        dg.getTable().write().csv("Metrics"+projectname+".csv");
       
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
			eval.recall(1)
		);

        output.setPrecision(eval.precision(1));

        return output;
    }


    private static class DatsetGenerator {
        private Double[] auc;
        private Double[] kappa ;
        private Double[] recall ;
        private Double[] precision ;
        private int currentrow;

        public DatsetGenerator(){
            auc= new Double[10];
            kappa= new Double[10];
            recall= new Double[10];
            precision= new Double[10];
            currentrow=0;
        }

        public void addrow(OutputWeka output){
    
            auc[currentrow] = output.getAuc();
            kappa[currentrow] = output.getKappa();
            recall[currentrow] = output.getRecall();
            precision[currentrow] = output.getPrecision();
            currentrow++;
    
    
        }

        public Table getTable(){
            DoubleColumn aucColumn = DoubleColumn.create("AUC", auc);
            DoubleColumn recallColumn = DoubleColumn.create("RECALL", recall);
            DoubleColumn precisionColumn = DoubleColumn.create("PRECISION", precision);
            DoubleColumn kappaColumn = DoubleColumn.create("KAPPA", kappa);
    
            Table table = Table.create("Dataset");
            table.addColumns(aucColumn,recallColumn,precisionColumn,kappaColumn);
            
            return table;
        }
    }
    
}
