package cloudsim.ext.datacenter;

public class PSOVmLoadBalancer extends VmLoadBalancer {
    public PSOVmLoadBalancer(DatacenterController datacenterController) {
    }

    @Override
    public int getNextAvailableVm() {
        return 0;
    }
    // TODO 1. round robin, 2. PSO, 3. RedBlack Tree
}



