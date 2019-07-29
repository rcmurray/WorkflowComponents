package edu.cmu.learnsphere.d3m.data;

import java.io.File;

import edu.cmu.pslc.datashop.workflows.AbstractComponent;

/**
 * Workflow component: template source for a component
 */
public class ProblemCreatorMain extends AbstractComponent {


    /**
     * Main method.
     * @param args the arguments
     */
    public static void main(String[] args) {

        ProblemCreatorMain tool = new ProblemCreatorMain();
        tool.startComponent(args);
    }

    /**
     * Constructor.
     */
    public ProblemCreatorMain() {
        super();
    }

    @Override
    protected void processOptions() {
        logger.info("Processing Options");

        // The addMetaData* methods make the meta data available to downstream components.


    }

    @Override
    protected void parseOptions() {


    }

    /**
     * Processes the input file(s) and option(s) to generate inputs to next component(s).
     */
    @Override
    protected void runComponent() {

	// Run the program...
	File outputDirectory = this.runExternal();


	File outputFile0 = new File(outputDirectory.getAbsolutePath() + "/session.json");
	File outputFile1 = new File(outputDirectory.getAbsolutePath() + "/ui.html");

		this.addOutputFile(outputFile0, 0, 0, "problem");
		this.addOutputFile(outputFile1, 1, 0, "inline-html");


        System.out.println(this.getOutput());

    }
}
