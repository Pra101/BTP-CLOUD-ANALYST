package cloudsim.ext.datacenter;
import cloudsim.ext.datacenter.DatacenterController;

import java.util.*;

import cloudsim.ext.Constants;
import cloudsim.ext.event.CloudSimEvent;
import cloudsim.ext.event.CloudSimEventListener;
import cloudsim.ext.event.CloudSimEvents;

public class HoneyBeeVMLoadBalancer extends VmLoadBalancer implements CloudSimEventListener {
	private int cutoff = 1;
	private int scoutBee = -1;

	// there are 2 possible states
	// busy and available
	private Map<Integer, VirtualMachineState> vmStatesList;
	Map<Integer, Integer> vmAllocationCounts = new HashMap<>();
	Map<Integer, Integer> fitness = new HashMap<>();
	
	
	public HoneyBeeVMLoadBalancer(DatacenterController dcb){
		this.vmStatesList = dcb.getVmStatesList();
		dcb.addCloudSimEventListener(this);
	}

	
	@Override
	public int getNextAvailableVm(){

		// initialize the next VMID to be fetched by -1
		int vmId = -1;
		vmId = getScoutBee();
		scoutBee = vmId;

		// mark the VM as allocated
		allocatedVm(vmId);
		System.out.println("allocated "+vmId);
		DatacenterController.allocatedTask();
		return vmId;
	}
	
	public void cloudSimEventFired(CloudSimEvent e)
	{
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
		
	private boolean isSendScoutBees(int scoutBee)
	{
		if((vmAllocationCounts.get(scoutBee)==null)||(vmAllocationCounts.get(scoutBee) < cutoff))
			return false;
		else
			return true;
	}
	
	/* This will return food source */
	int getScoutBee()
	{
		if(scoutBee==-1)
		{
			if(vmStatesList.size()>0)
				return 0;
			else
				return -1;
		}
		else
		{
			if(isSendScoutBees(scoutBee)==false)
				return scoutBee;
			else
			{
				// process the possible VMs for availability
				SendEmployedBees();

				// check using an OnLooker Bee for the final available VM
				return SendOnlookerBees();
			}
		}
	}
	
	int MemorizeBestSource() 
	{
		return waggleDance();
	}
	
	/* These are the bees which will observe Waggle Dance and give us best source */
	int SendOnlookerBees()
	{
		return MemorizeBestSource();
	}
	
	// Calculation to get the fitness value of VM
	void calculation()
	{
		  /*Employed Bee Phase*/
		  for (int i=0; i<vmStatesList.size(); i++)
		  {
			   if(vmAllocationCounts.get(i)==null)
				   fitness.put(i, 0);
			   else
				   fitness.put(i, calculateFitness(vmAllocationCounts.get(i)));
		  }
	}
	
	int calculateFitness(int solValue)
	{
		solValue = 1/(1-solValue);
		return solValue;
//		return solValue;	// taskLength/(VM capacity)
	}
	
	// Bees went in search & finding all the fitness
	void SendEmployedBees()
	{
	  fitness.clear();
	  calculation();
	}

	
	// By waggle Dance, we are getting best VM available
	private int waggleDance() 
	{
		int Min = 0;
		int globalMinimumFitness = fitness.get(0);

		for(int i=1; i<vmStatesList.size();i++)
		{
				if(fitness.get(i)< globalMinimumFitness)
				{
					globalMinimumFitness = fitness.get(i);
					Min = i;
				}
		}
		return Min;
	}
}