package cloudsim.ext.datacenter;

import java.util.*;

public class HybridVMLoadBalancer extends VmLoadBalancer{

    DatacenterController dcbLocal;
    int virtualMachineCount = 0;
    int index = 0;

    // for ordering the Virtual machines according to their pre-occupancy
    AVLTreeBalancer<VMObjectAVL> vmTree = new AVLTreeBalancer<>();
    Map<Integer, Integer> occupancy = new HashMap<>();

    boolean firstIteration = true;
    public HybridVMLoadBalancer(DatacenterController dcb) {
        super();
        dcbLocal = dcb;
        virtualMachineCount = dcbLocal.vmlist.size();

    }
    // Number of particles in the swarm
    private static final int NUM_PARTICLES = 100;
    // Maximum number of iterations
    private static final int MAX_ITERATIONS = 1000;

    @Override
    public int getNextAvailableVm() {

        if(firstIteration)
        {
            // Create the swarm
            List<Particle> swarm = new ArrayList<>(NUM_PARTICLES);
            for (int i = 0; i < NUM_PARTICLES; i++) {
                swarm.set(i, new Particle());
            }

            Particle globalBest;
            // Run the DPSO algorithm
            for (int i = 0; i < MAX_ITERATIONS; i++) {
                // Update the global best position
                for (Particle p : swarm) {
                    if (p.getFitness() > p.getBestFitness()) {
                        p.setBestPosition(p.getPosition());
                    }
                }

                // Update the global best position
                globalBest = swarm.get(0);
                for (Particle p : swarm) {
                    if (p.getBestFitness() > globalBest.getBestFitness()) {
                        globalBest = p;
                    }
                }

                // Update the velocity and position of each particle
                for (Particle p : swarm) {
                    p.updateVelocity(globalBest);
                    p.updatePosition();
                }
            }

            swarm.sort(new sortByBestPosition());
        }

            int ans = (index)%virtualMachineCount +1;
            index = (index+1)%NUM_PARTICLES;

            int tasklen = (int)(Math.random()*(1000-10+1)+10);
            double currentPreOccupancy = occupancy.get(index);
//            double currentPreOccupancy = calculatePreoccupancy(index, tasklen);
            VMObjectAVL targetOb = new VMObjectAVL(currentPreOccupancy, index);
            VMObjectAVL searchresult = vmTree.search(targetOb).getVirtualMachinePreoccupancy();
            if(searchresult != null)
            {
                VMObjectAVL leftMostDesc = vmTree.getLeftMostDescendant(searchresult).getVmID();
                ans = leftMostDesc.vmID;
            }
            else
            {
                VMObjectAVL toBeInserted = new VMObjectAVL(searchresult);
                toBeInserted.preOccupancy += tasklen;
                ans = searchresult.vmID;
                vmTree.insert(toBeInserted);
            }

            allocatedVm(ans);
            return ans;
    }

    private double calculatePreoccupancy(int index, int tasklen) {
        int currentTaskLen = occupancy.get(index);
        occupancy.replace(index, currentTaskLen+tasklen);
        return currentTaskLen+tasklen;
    }

    private static class sortByBestPosition implements Comparator<Particle>
    {
        public int compare(Particle p1, Particle p2)
        {
            return Double.compare(p1.getBestPosition()[0], p2.getBestPosition()[0]);
        }
    }
        // Print the global best position

    private static class Particle {
        // Position of the particle
        private double[] position;
        // Velocity of the particle
        private double[] velocity;
        // Best position found by the particle
        private double[] bestPosition;
        // Current fitness of the particle
        private double fitness;
        // Best fitness found by the particle
        private double bestFitness;
        // Random number generator
        private Random rand = new Random();

        public Particle() {
            // Initialize the position, velocity, and best position randomly
            position = new double[2];
            velocity = new double[2];
            bestPosition = new double[2];
            for (int i = 0; i < 2; i++) {
                position[i] = rand.nextDouble();
                velocity[i] = rand.nextDouble();
                bestPosition[i] = position[i];
            }
            // Calculate the initial fitness and best fitness
            fitness = calculateFitness(position);
            bestFitness = fitness;
        }

        public double[] getPosition() {
            return position;
        }

        public double[] getVelocity() {
            return velocity;
        }

        public double[] getBestPosition() {
            return bestPosition;
        }

        public double getFitness() {
            return fitness;
        }

        public double getBestFitness() {
            return bestFitness;
        }

        public void setBestPosition(double[] position) {
            bestPosition = position;
            bestFitness = calculateFitness(position);
        }

        public void updateVelocity(Particle globalBest) {
            double w = 0.729; // Inertia weight
            double c1 = 1.49445; // Cognitive weight
            double c2 = 1.49445; // Social weight

            for (int i = 0; i < 2; i++) {
                // Update the velocity using DPSO's velocity update equation
                velocity[i] = (w * velocity[i]) + (c1 * rand.nextDouble() * (bestPosition[i] - position[i]))
                        + (c2 * rand.nextDouble() * (globalBest.getBestPosition()[i] - position[i]));
            }
        }

        public void updatePosition() {
            for (int i = 0; i < 2; i++) {
                // Update the position using DPSO's position update equation
                position[i] = position[i] + velocity[i];
            }
            // Update the fitness value
            fitness = calculateFitness(position);
        }

        // Calculates the fitness value of a given position
        // Calculates the fitness value of a given position
        private double calculateFitness(double[] position) {
            // The position array should have two elements: the number of tasks assigned
            // to the first machine and the number of tasks assigned to the second machine.
            double numTasks1 = position[0];
            double numTasks2 = position[1];

            // Calculate the difference in the number of tasks assigned to each machine
            double diff = Math.abs(numTasks1 - numTasks2);
            // The fitness value is the inverse of the difference, so that a smaller
            // difference results in a higher fitness value
            return 1.0 / (1.0 + diff);
        }

    }
}

