package edu.cmu.pslc.learnsphere.transform.entityset;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;






import org.apache.commons.lang.StringUtils;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

public class EntitySetMain extends AbstractComponent {



    
    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        EntitySetMain tool = new EntitySetMain();
        tool.startComponent(args);

    }

    /**
     * This class runs Join on two files.
     */
    public EntitySetMain() {
        super();


    }

    @Override
    protected void processOptions() {

        this.addMetaData("compressed", 0, META_DATA_LABEL, "label0", 0, null);
    }

    @Override
    protected void runComponent() {
        // Run the program and add the files it generates to the component output.
        File outputDirectory = this.runExternal();
        // Attach the output files to the component output with addOutputFile(..>)
        //#OFFICIAL
        if (outputDirectory.isDirectory() && outputDirectory.canRead()) {
            File file0 = new File(outputDirectory.getAbsolutePath() + "/output.zip");
            if (file0 != null && file0.exists()) {
                Integer nodeIndex0 = 0;
                Integer fileIndex0 = 0;
                String label0 = "compressed";
                this.addOutputFile(file0, nodeIndex0, fileIndex0, label0);
                
            } else {
                this.addErrorMessage("An unknown error has occurred.");
            }
        }
        
        // Send the component output back to the workflow.
        System.out.println(this.getOutput());
    }
}
