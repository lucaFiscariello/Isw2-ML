package com.fiscariello.ml;

/*
 *  How to use WEKA API in Java 
 *  Copyright (C) 2014 
 *  @author Dr Noureddin M. Sadawi (noureddin.sadawi@gmail.com)
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it as you wish ... 
 *  I ask you only, as a professional courtesy, to cite my name, web page 
 *  and my YouTube Channel!
 *  
 */

//import required classes
import weka.core.Instances;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.converters.ConverterUtils.DataSource;
import weka.filters.supervised.instance.Resample;
import weka.filters.supervised.instance.SMOTE;
import weka.filters.supervised.instance.SpreadSubsample;


public class Weka{
	public Weka.OutputWeka evalModel(String pathTraining, String pathTesting, Classifier  classifier) throws Exception{
		
		String sampling ="None";

		DataSource source1 = new DataSource(pathTraining);
		Instances training = source1.getDataSet();
		DataSource source2 = new DataSource(pathTesting);
		Instances testing = source2.getDataSet();
				 				
		int numAttr = training.numAttributes();
		training.setClassIndex(numAttr - 1);
		testing.setClassIndex(numAttr - 1);
				
		classifier.buildClassifier(training);

		Evaluation eval = new Evaluation(testing);	
		eval.evaluateModel(classifier, testing); 
				
		Weka.OutputWeka output = new Weka.OutputWeka(
			eval.areaUnderROC(1),
			eval.kappa(),
			eval.numTrueNegatives(numAttr - 1),
			eval.numTruePositives(numAttr - 1),
			eval.numFalseNegatives(numAttr - 1),
			eval.numFalsePositives(numAttr - 1),
			eval.recall(1)
		);

		output.setPrecision(eval.precision(1));
		output.setSampling(sampling);
			
		return output;
				
	}

	public Weka.OutputWeka evalModelUnderSampling(String pathTraining, String pathTesting, Classifier  classifier) throws Exception{
		String sampling = "Under Sampling";
		SpreadSubsample spread = new SpreadSubsample();

		FilteredClassifier fc = new FilteredClassifier();
		fc.setFilter(spread);
        fc.setClassifier(classifier);

		Weka.OutputWeka output = this.evalModel(pathTraining, pathTesting, fc);
		output.setSampling(sampling);
		
		return output;
				
	}

	public Weka.OutputWeka evalModelOverSampling(String pathTraining, String pathTesting, Classifier  classifier) throws Exception{
		String sampling = "Over Sampling";
		Resample spread = new Resample();

		FilteredClassifier fc = new FilteredClassifier();
		fc.setFilter(spread);
        fc.setClassifier(classifier);

		Weka.OutputWeka output = this.evalModel(pathTraining, pathTesting, fc);
		output.setSampling(sampling);
		
		return output;
				
	}

	public Weka.OutputWeka evalModelSMOTESampling(String pathTraining, String pathTesting, Classifier  classifier) throws Exception{
		String sampling = "SMOTE";
		SMOTE spread = new SMOTE();

		FilteredClassifier fc = new FilteredClassifier();
		fc.setFilter(spread);
        fc.setClassifier(classifier);

		Weka.OutputWeka output = this.evalModel(pathTraining, pathTesting, fc);
		output.setSampling(sampling);
		
		return output;
				
	}

	public static class OutputWeka {
		private double auc;
		private double kappa;
		private double trueNegative;
		private double truePositive;
		private double falseNegative;
		private double falsePositive;
		private double recall;
		private double precision;
		private String sampling;

		private boolean isValid;

		public OutputWeka (double auc, double kappa, double tn, double tp, double fn, double fp, double recall){
			this.auc=auc;
			this.kappa=kappa;
			this.trueNegative=tn;
			this.truePositive=tp;
			this.falseNegative=fn;
			this.falsePositive=fp;
			this.recall=recall;

		}


		public void setPrecision(double precision){
			this.precision= precision;
		}

		public double getAuc(){
			return this.auc;
		}

		public double getKappa(){
			return this.kappa;
		}

		public double getTrueNegative(){
			return this.trueNegative;
		}

		public double getTruePositive(){
			return this.truePositive;
		}

		public double getFalseNegative(){
			return this.falseNegative;
		}

		public double getFalsePositive(){
			return this.falsePositive;
		}

		public double getRecall(){
			return this.recall;
		}

		public double getPrecision(){
			return this.precision;
		}

		public String getSampling(){
			return this.sampling;
		}

		public void setSampling(String sampling){
			 this.sampling= sampling;
		}

		public boolean isValid(){
			return this.isValid;
		}

		public String toString(){
			String s ;
			s= "auc "+ auc + "\nkappa " + kappa + "\nrecall "+ recall+ "\nprecision "+precision+"\n------";
			return s;
		}


	}
}


