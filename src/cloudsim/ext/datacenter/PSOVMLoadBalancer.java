package cloudsim.ext.datacenter;

import cloudsim.ext.Constants;
import cloudsim.ext.event.CloudSimEvent;
import cloudsim.ext.event.CloudSimEventListener;
import cloudsim.ext.event.CloudSimEvents;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class PSOVMLoadBalancer extends VmLoadBalancer implements CloudSimEventListener {
    DatacenterController dcbLocal;
    int virtualMachineCount = 0;
    int index = 0;
    int totalTasks = 0;
    // hyper parameters
    double c1 = 2.1;
    double c2 = 2.2;
    double c3 = 2.0;
    double cutoff = 0.7;

    // for ordering the Virtual machines according to their pre-occupancy
    AVLTreeBalancer<VMObjectAVL> vmTree = new AVLTreeBalancer<>();
    Map<Integer, Integer> occupancy = new HashMap<>();

    boolean firstIteration = true;
    Map<Integer, VirtualMachineState> vmStatesList = null;
    public PSOVMLoadBalancer(DatacenterController dcb) {
        super();
        dcbLocal = dcb;
        virtualMachineCount = dcbLocal.vmlist.size();
        totalTasks = dcb.getAllRequestsProcessed();
        vmStatesList = dcb.getVmStatesList();
    }

    // DPSO
    // Number of particles in the swarm
    private static final int NUM_PARTICLES = 100;

    // position values
    // behaving as particles of dpso
    int[][][] X = new int[NUM_PARTICLES][virtualMachineCount][totalTasks];

    // local best matrix for each particle
    int[][][] X_local_best = new int[NUM_PARTICLES][virtualMachineCount][totalTasks];
    // global best position matrix (same for all particles)
    int[][] P_global_best = new int[virtualMachineCount][totalTasks];

    // velocity matrices
    double[][][] V = new double[NUM_PARTICLES][virtualMachineCount][totalTasks];


    public void randomMatricesInitialization(int[][][] X, double[][][] V, int numParticles, int vmCount, int totalTasks) {
        Random random = new Random();

        for (int i = 0; i < numParticles; i++) {
            for (int j = 0; j < vmCount; j++) {
                for (int k = 0; k < totalTasks; k++) {
                    if (random.nextDouble() < 0.5) {
                        X[i][j][k] = 1;
                    } else {
                        X[i][j][k] = 0;
                    }
                }
            }
        }

        for (int i = 0; i < numParticles; i++) {
            for (int j = 0; j < vmCount; j++) {
                for (int k = 0; k < totalTasks; k++) {
                    if (random.nextDouble() < 0.5) {
                        V[i][j][k] = 1;
                    } else {
                        V[i][j][k] = 0;
                    }
                }
            }
        }

    }

    // Maximum number of iterations

    private static final int MAX_ITERATIONS = 1000;
    private int bestParticle = -1;
    private static int lastVMReturned = -1;
    @Override
    public int getNextAvailableVm() {

        if (firstIteration) {
            // Create the swarm
            randomMatricesInitialization(X, V, NUM_PARTICLES, virtualMachineCount, totalTasks);

            // Run the DPSO algorithm
            for (int i = 0; i < MAX_ITERATIONS; i++)
            {
                // updating the particle velocities
                for (int k = 0; k < NUM_PARTICLES; k++) {
                    V[k] = addtn(mult(c1, V[k], virtualMachineCount, totalTasks),
                            addtn(mult(c2, diff(X_local_best[k], X[k], virtualMachineCount, totalTasks), virtualMachineCount, totalTasks),
                                    mult(c3, diff(P_global_best, X[k], virtualMachineCount, totalTasks), virtualMachineCount, totalTasks)
                                    , virtualMachineCount, totalTasks), virtualMachineCount, totalTasks);

                    for (int j = 0; j < virtualMachineCount; j++) {
                        int maxIndex = 0;
                        for (int l = 0; l < totalTasks; l++) {
                            if (V[k][j][l] > V[k][j][maxIndex]) {
                                maxIndex = l;
                            }
                        }

                        for (int l = 0; l < totalTasks; l++) {
                            if (l == maxIndex)
                                X[k][j][l] = 1;
                            else
                                X[k][j][l] = 0;
                        }
                    }
                }
            }

            bestParticle = evaluateParticles(X,NUM_PARTICLES, virtualMachineCount, totalTasks);
        }

        int ansIndex = 0;
        for(int z = 0; z < totalTasks; z++)
        {
            if(X[bestParticle][lastVMReturned][z]!=0)
            {
                ansIndex = z;
                break;
            }
        }
        return X[bestParticle][lastVMReturned][ansIndex];
    }

    private int evaluateParticles(int[][][] x, int NUM_PARTICLES,int virtualMachineCount, int totalTasks) {

        int mincostIndex = 0;
        double minCost = 100000;
        Random random = new Random();
        double lambda = random.nextDouble();

        for(int k = 0; k<NUM_PARTICLES; k++)
        {
            double currCost = lambda*dcbLocal.getDataTransferCost() + (1-lambda)* dcbLocal.getTotalCost();
            if(currCost<minCost)
            {
                mincostIndex = k;
            }
        }
        return mincostIndex;
    }

    @Override
    public void cloudSimEventFired(CloudSimEvent e) {
        if (e.getId() == CloudSimEvents.EVENT_CLOUDLET_ALLOCATED_TO_VM)
        {
            int vmId = (Integer) e.getParameter(Constants.PARAM_VM_ID);

            int countCloudlets;

            if(vmAllocationCounts.get(vmId)==null)
                countCloudlets = 0;
            else
                countCloudlets = vmAllocationCounts.get(vmId);
            vmAllocationCounts.put(vmId,countCloudlets+1);

            if(vmAllocationCounts.get(vmId)>cutoff)
                vmStatesList.put(vmId, VirtualMachineState.BUSY);
        }

        else if (e.getId() == CloudSimEvents.EVENT_VM_FINISHED_CLOUDLET)
        {
            int vmId = (Integer) e.getParameter(Constants.PARAM_VM_ID);
            int countCloudlets = vmAllocationCounts.get(vmId);

            vmAllocationCounts.put(vmId,countCloudlets-1);

            if(vmAllocationCounts.get(vmId)<cutoff)
                vmStatesList.put(vmId, VirtualMachineState.AVAILABLE);
        }
    }

    private double[][] mult(double c, double[][] V, int virtualMachineCount, int totalTasks)
    {
        for (int i = 0; i<virtualMachineCount; i++)
        {
            for(int j = 0; j<totalTasks; j++)
            {
                V[i][j] *= c;
            }
        }

        return V;
    }

    private double[][] addtn(double[][]A, double[][]B, int virtualMachineCount, int totalTasks)
    {
        double[][] res = new double[virtualMachineCount][totalTasks];
        for (int i = 0; i<virtualMachineCount; i++)
        {
            for(int j = 0; j<totalTasks; j++)
            {
                res[i][j] = A[i][j]+B[i][j];
            }
        }
        return res;
    }

    private double[][] diff(int[][]A, int[][]B, int virtualMachineCount, int totalTasks)
    {
        double[][]res = new double[virtualMachineCount][totalTasks];
        for (int i = 0; i<virtualMachineCount; i++)
        {
            for(int j = 0; j<totalTasks; j++)
            {
                res[i][j] = A[i][j]-B[i][j];
            }
        }

        return res;
    }

}
