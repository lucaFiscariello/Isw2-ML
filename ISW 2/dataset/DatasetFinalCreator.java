package com.fiscariello.dataset;

import com.fiscariello.ML.Weka;
import tech.tablesaw.api.DoubleColumn;
import tech.tablesaw.api.IntColumn;
import tech.tablesaw.api.StringColumn;
import tech.tablesaw.api.Table;
import weka.classifiers.Classifier;


public class DatasetFinalCreator {
    private Table table;
    private Double[] auc;
    private Double[] kappa;
    private Double[] tn;
    private Double[] tp;
    private Double[] fn;
    private Double[] fp;
    private Double[] recall;
    private Double[] precision;
    private Double[] percTrainTotal;

    private Integer[] numTrainRelease;
    private Integer[] snoringClass;

    private String[] datasetname;
    private String[] classifier;
    private String[] sampling;


    private int currentRows;

    public enum NameColumnDataset {
        Auc, Kappa, Tue_Negative, True_Positive, False_Negative, False_positive, Recall, Precision, Dataset, Classifier , Num_Training_Release, Perc_Train_Total, Sampling, Snoring_Class;
    }

    public DatasetFinalCreator(int size){
        auc = new Double[size];
        kappa = new Double[size];
        tn = new Double[size];
        tp = new Double[size];
        fn = new Double[size];
        fp = new Double[size];
        recall = new Double[size];
        precision = new Double[size];
        numTrainRelease = new Integer[size];
        percTrainTotal = new Double[size];
        datasetname = new String[size];
        classifier = new String[size];
        sampling = new String[size];
        snoringClass = new Integer[size];

        currentRows=0;
    }

    
    public void addRow(Weka.OutputWeka outputWeka, String project, Classifier classifierName, int numTrainingRelease, double perctraintot, int snoringCl){
        auc[currentRows] = outputWeka.getAuc();
        kappa[currentRows] = outputWeka.getKappa();
        tn[currentRows] = outputWeka.getTrueNegative();
        tp[currentRows] = outputWeka.getTruePositive();
        fn[currentRows] = outputWeka.getFalseNegative();
        fp[currentRows] = outputWeka.getFalsePositive();
        sampling[currentRows]= outputWeka.getSampling();
        precision[currentRows] = outputWeka.getPrecision();
        recall[currentRows]= outputWeka.getRecall();
        datasetname[currentRows] = project;
        
        String nameClassifier = classifierName.getClass().getName();
        nameClassifier= nameClassifier.substring(nameClassifier.lastIndexOf(".")+1);
        classifier[currentRows] = nameClassifier;

        numTrainRelease[currentRows] = numTrainingRelease;
        percTrainTotal[currentRows] = perctraintot;
        snoringClass[currentRows] = snoringCl;
        
        currentRows++;

    }

    public Table getTable(){

        StringColumn classifierColumn = StringColumn.create(NameColumnDataset.Classifier.toString(),this.classifier);
        StringColumn datasetnameColumn =  StringColumn.create(NameColumnDataset.Dataset.toString(),this.datasetname);
        StringColumn samplingColumn =  StringColumn.create(NameColumnDataset.Sampling.toString(),this.sampling);

        DoubleColumn aucColumn = DoubleColumn.create(NameColumnDataset.Auc.toString(), auc);
        DoubleColumn kappaColumn = DoubleColumn.create(NameColumnDataset.Kappa.toString(), kappa);
        DoubleColumn tnColumn = DoubleColumn.create(NameColumnDataset.Tue_Negative.toString(), tn);
        DoubleColumn tpColumn = DoubleColumn.create(NameColumnDataset.True_Positive.toString(), tp);
        DoubleColumn fnColumn = DoubleColumn.create(NameColumnDataset.False_Negative.toString(), fn);
        DoubleColumn fpColumn = DoubleColumn.create(NameColumnDataset.False_positive.toString(), fp);
        DoubleColumn precisionColumn = DoubleColumn.create(NameColumnDataset.Precision.toString(), precision);
        DoubleColumn recallColumn = DoubleColumn.create(NameColumnDataset.Recall.toString(), recall);
        DoubleColumn percTrainColumn = DoubleColumn.create(NameColumnDataset.Perc_Train_Total.toString(), percTrainTotal);

        IntColumn numTrainColumn = IntColumn.create(NameColumnDataset.Num_Training_Release.toString(), numTrainRelease);
        IntColumn numScnoringClassColumn = IntColumn.create(NameColumnDataset.Snoring_Class.toString(), snoringClass);


        table = new Dataset("Dataset");
        table.addColumns(classifierColumn,datasetnameColumn,samplingColumn,aucColumn,kappaColumn,tnColumn,tpColumn,fnColumn,fpColumn,precisionColumn,recallColumn,percTrainColumn,numTrainColumn,numScnoringClassColumn);

        return table;
    }

}
