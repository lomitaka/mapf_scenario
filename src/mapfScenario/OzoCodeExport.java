package mapfScenario;

import helpout.QuestionAgentCount;
import helpout.methods;
import javafx.stage.FileChooser;
import mapfScenario.picat.SolverProcess;
import mapfScenario.simulation.Solution;
import org.apache.log4j.Logger;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;

/**
 * This class handles export solution to insturction file for ozobot.
 * */
public class OzoCodeExport{
    private final Logger logger = Logger.getLogger(OzoCodeExport.class);

    private boolean isInError = false;
    private String errorString = null;

    /** performs export to file.
     * propmpt user for file to save, and perform export */
    public  void doExport(Solution s){

        //ask agent count
        QuestionAgentCount qac = new QuestionAgentCount();
        qac.setQuestionData(s.getAglist(),(Integer)0);
        methods.showDialogQuestion(qac);

        if (!qac.getConfirmed()){
            return;
        }
        int answer = qac.getAnswer();

        // ask output file

        if (answer >= s.getAglist().size()) { answer = -1;}

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose target file");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("OZOCODE", "*.ozocode")
        );
        File targetFile = fileChooser.showSaveDialog(DataStore.mainWindow);

        if (targetFile == null) { return; }

        if (!targetFile.getName().toLowerCase().endsWith(".ozocode")){
            targetFile = new File(targetFile.getAbsolutePath()+".ozocode");
        }

        doExport(s.getSolutionFileName(),targetFile,answer);



    }

    private String getTemplateFileName(){
        File f = methods.fileToAbsolute(DataStore.settings.ozobotTemplateFile);
        //File f = new File(DataStore.settings.ozobotTemplateFile);
        if (!f.exists()) {
            methods.showDialog(Consts.errorMissingTemplateFile);
            return null;
        }

        return f.getAbsolutePath();

    }

    /** export is performed by calling external executable OzocodeGenerator.
     * That executable takes four argumets<br/>
     * 1. file with solution generated by picat<br/>
     * 2. file with ozocode template<br/>
     * 3. file where in which code will be writen<br/>
     * 4. number of agents which code will be written. if agnet number if -1 or all, then ozocode instruction<br/>
     * file will contain switch for agent number, and each branch will have code for its agent*/

    public void doExport(String inputFile, File outputFile, int agentNum ){

        String libDirStr =  Consts.libDir;
        File libdir = methods.fileToAbsolute(libDirStr);
        //File libdir = new File(libDirStr);

        String templateFile = getTemplateFileName();
        if (templateFile == null) { return;}

        /* calling convention Jar fileIn templateFile fileOut agentCount  */
        /*String calledProg =
                String.format("java -jar %s%sOzoCodeGenerator.jar %s %s %s %s",
                        libdir.getAbsolutePath(),
                        File.separator,
                        inputFile,
                       templateFile,
                        outputFile,
                        agentNum);*/
        String[] calledProg = {
                "java",
                "-jar",
                String.format("%s%sOzoCodeGenerator.jar",libdir.getAbsolutePath(), File.separator),
                inputFile,
                templateFile,
                outputFile.getAbsolutePath(),
                agentNum+""
        };

        StringBuilder logTmp = new StringBuilder();
        for (String s : calledProg){ logTmp.append(s);logTmp.append(" "); }
        logger.debug("picat argument:" + logTmp.toString());
        logger.debug("ozocode generator argument:" + logTmp);

        StringBuilder ozoCodeOutput = new StringBuilder();
        try {

            Process proc = Runtime.getRuntime().exec(calledProg);

            InputStream in = proc.getInputStream();

            BufferedInputStream bis = new BufferedInputStream(in);
            //this waits anyway.

            int znak = 0;
            while ((znak = bis.read())!= -1) {
                System.out.print((char) znak);
                ozoCodeOutput.append((char) znak);
            }


            proc.waitFor();

            // check for error
            if(!outputFile.exists()){

                isInError = true;
                errorString = ozoCodeOutput.toString();
                logger.error("no result file written" + errorString );
                return;
            }


        } catch (Exception e){
            methods.showDialog("FAILED\n" + e.getMessage() );
        }

    }

}
