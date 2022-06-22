package com.fiscariello.dataset;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.fiscariello.dataset.DatasetCreator.NameColumnDataset;

import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;
import tech.tablesaw.columns.Column;

public class Dataset extends Table {

    protected Dataset(String name, Collection<Column<?>> columns) {
        super(name, columns);
    }

    protected Dataset(String name){
        super(name);
    }

    public int getNumberRow(){
        return this.column(0).size();
    }

    public int getNumberBuggyClassByRelease(String release){
        Table tableFilt = this.where( 
            this.stringColumn(DatasetCreator.NameColumnDataset.RELEASE.toString()).isEqualTo(release)
            .and(this.stringColumn(DatasetCreator.NameColumnDataset.BUGGY.toString()).isEqualTo("YES")));

        return tableFilt.column(0).size();
    }

    public void writeArff(String nameFile) throws IOException{

        Table table = this.copy();
        table.removeColumns(NameColumnDataset.PATH_CLASS.toString(),NameColumnDataset.RELEASE.toString());

        String relation ="@relation "+nameFile+"\n\n";
        String attribute="@attribute ";
        String data= "@data\n";

        String toWrite;
        List<String> columnNames =table.columnNames();
        
        File file = new File(nameFile);
        
        try ( FileWriter fw = new FileWriter(file);) {
            fw.append(relation);

            for (String name : columnNames) {

                if(table.column(name).get(0) instanceof Integer || table.column(name).get(0) instanceof Double)
                    toWrite=attribute+name+" numeric \n";
                else
                    toWrite=attribute+name+"{NO,YES} \n";

                fw.append(toWrite);
            }

            fw.write("\n"+ data);

            for (Row row : table) {
                List<Object> value = new ArrayList<>();

                for (String name : columnNames)
                    value.add(row.getObject(name));

                toWrite= value.toString().replace("[", "").replace("]", "").replace("null", "?").replace(" ", "");

                fw.write(toWrite+"\n");
            }

            fw.flush();
            
        }
       

        
    }

    public void writeArffComplex(String nameFile) throws IOException{

        this.removeColumns(NameColumnDataset.PATH_CLASS.toString(),NameColumnDataset.RELEASE.toString());

        String relation ="@relation "+nameFile+"\n\n";
        String attribute="@attribute ";
        String data= "@data\n";

        String toWrite;
        String uniqueValuesString;
        List<String> columnNames =this.columnNames();

        File file = new File(nameFile);
        try ( FileWriter fw = new FileWriter(file);) {
            fw.append(relation);

            for (String name : columnNames) {
    
                HashSet<Object> uniqueValues = new HashSet<>();
    
                for (Object object : this.column(name)) {
                    uniqueValues.add(object);
                }
    
                uniqueValuesString= uniqueValues.toString();
                uniqueValuesString= uniqueValuesString.replace("[", "{").replace("]", "}").replace(" ", "");
                toWrite=attribute+name+" "+uniqueValuesString+"\n";
                fw.append(toWrite);
            }
    
            fw.write("\n"+ data);
    
            for (Row row : this) {
                List<Object> value = new ArrayList<>();
    
                for (String name : columnNames)
                    value.add(row.getObject(name));
    
                toWrite= value.toString().replace("[", "").replace("]", "").replace("null", "?").replace(" ", "");
    
                fw.write(toWrite+"\n");
            }
    
    
            fw.flush();
            
        }

        }



}
