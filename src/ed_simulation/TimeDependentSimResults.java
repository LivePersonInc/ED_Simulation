package ed_simulation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

public class TimeDependentSimResults {

    //Keep statistics per each time bin.
//    private HashMap<Integer, SimResults> simStatisticsPerTimeBin;
    int binSize;
    int numBins;
    private SimResults[] simStatisticsPerTimeBin;

    /**
     * Entry j in the input array is the left edge of timebin j. The bins are assumed to commence at 0 and represent equally spaced intervals.
     *
     * @param
     */
    public TimeDependentSimResults(int binSize, int numBins, int maxTotalCapacity) throws Exception {
//        if( timeBins == null || timeBins.length < 1)
//        {
//            throw new Exception("The time bins array should consist of at least a single element");
//        }
//        if( timeBins[0] != 0 )
//        {
//            throw new Exception("The time bins array should commence in 0");
//        }


        if (binSize <= 0) {
            throw new Exception("binSize must be >= 0!");
        }
        if (numBins <= 0) {
            throw new Exception("numBins must be >= 0");
        }
        this.binSize = binSize;
        this.numBins = numBins;

//        simStatisticsPerTimeBin = new HashMap<Integer, SimResults>(timeBins.length);
        simStatisticsPerTimeBin = new SimResults[numBins];
        for (int i = 0; i < simStatisticsPerTimeBin.length; i++) {
            simStatisticsPerTimeBin[i] = new SimResults(maxTotalCapacity); //TODO: It's completely awkward that I need to pass the capacity to the constructor. Change that later on.
        }

    }

    public SimResults getCurrTimeSimResult(double t) {
        return simStatisticsPerTimeBin[((int) Math.floor((t % binSize * numBins) / binSize))];
    }

    public void writeToFile(String outfile) {
        //Skip this for the time being - just override.
//        File f = new File(outfile);
//        if (f.exists() ) {
//            SimParams smp = new SimParams();
//            //.... Read inputs.
//
//        }
        FileWriter fileWriter = null;
        try {

            fileWriter = new FileWriter(outfile);
            fileWriter.append("#BinSize," + Integer.toString(binSize) + "\n");
            fileWriter.append("TimeBin,MeanServiceQueueLength,MeanHoldingTime,MeanWaitingTime,HoldingProbability,WaitingProbability,MeanTotalInSystem,MeanAllInSystem\n");
            int currTimeBin = 0;
            for( SimResults sr : simStatisticsPerTimeBin  )
            {
                fileWriter.append( currTimeBin*binSize + "," + sr.getMeanServiceQueueLength() + ","+sr.getMeanHoldingTime() + "," + sr.getMeanWaitingTime() + ","+ sr.getHoldingProbability()
                        + ","+ sr.getWaitingProbability() + ","+ sr.getMeanTotalInSystem() +  ","+ sr.getMeanAllInSystem() + "\n" );
                currTimeBin += 1;
            }



        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {

                fileWriter.flush();

                fileWriter.close();

            } catch (IOException e) {

                System.out.println("Error while flushing/closing fileWriter !!!");

                e.printStackTrace();

            }

        }

    }
}
