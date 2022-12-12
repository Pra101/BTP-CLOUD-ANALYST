package cloudsim.ext.datacenter;


public class VMObjectAVL implements Comparable<VMObjectAVL> {
    public int vmID;
    public double preOccupancy;

    public VMObjectAVL(double currentPreOccupancy, int index) {
        this.vmID = index;
        this.preOccupancy = currentPreOccupancy;
    }

    public VMObjectAVL(VMObjectAVL ob) {
        this.vmID = ob.vmID;
        this.preOccupancy = ob.preOccupancy;
    }

    @Override
    public int compareTo(VMObjectAVL o) {
        if(preOccupancy > o.preOccupancy)
        {
            return 1;
        }
        else
        {
            return -1;
        }
    }
}

