package com.fiscariello;

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

import weka.classifiers.Evaluation;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.converters.ConverterUtils.DataSource;




public class Weka{
	public static void main(String args[]) throws Exception{
		//load datasets
				DataSource source1 = new DataSource("C:\\Users\\lucaf\\Desktop\\Luca\\Magistrale\\isw2\\deliverable3\\Training.arff");
				Instances training = source1.getDataSet();
				DataSource source2 = new DataSource("C:\\Users\\lucaf\\Desktop\\Luca\\Magistrale\\isw2\\deliverable3\\Testing.arff");
				Instances testing = source2.getDataSet();
				 				
				int numAttr = training.numAttributes();
				training.setClassIndex(numAttr - 1);
				testing.setClassIndex(numAttr - 1);
				

				NaiveBayes classifier = new NaiveBayes();

				classifier.buildClassifier(training);

				Evaluation eval = new Evaluation(testing);	
				
				eval.evaluateModel(classifier, testing); 
				
				System.out.println("AUC = "+eval.areaUnderROC(1));
				System.out.println("kappa = "+eval.kappa());
                
			
				
	}
}
