/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.cpuexec.*;
import static mame056.cpuexecH.*;
import static mame056.cpuintrfH.*;

// refactor
import static arcadeflex036.osdepend.logerror;

public class taitosnd
{
	
	
	/**********************************************************************************************
	
		It seems like 1 nibble commands are only for control purposes.
		2 nibble commands are the real messages passed from one board to the other.
	
	**********************************************************************************************/
	
	public static int TC0140SYT_PORT01_FULL = (0x01);
	public static int TC0140SYT_PORT23_FULL           = (0x02);
	public static int TC0140SYT_PORT01_FULL_MASTER    = (0x04);
	public static int TC0140SYT_PORT23_FULL_MASTER    = (0x08);
	
	public static class TC0140SYT
	{
		public int[] slavedata = new int[4];	/* Data on master->slave port (4 nibbles) */
		public int[] masterdata = new int[4];/* Data on slave->master port (4 nibbles) */
		public int mainmode;		/* Access mode on master cpu side */
		public int submode;		/* Access mode on slave cpu side */
		public int status;		/* Status data */
		public int nmi_enabled;	/* 1 if slave cpu has nmi's enabled */
		public int nmi_req;		/* 1 if slave cpu has a pending nmi */
	};
	
	static TC0140SYT tc0140syt = new TC0140SYT();
	
	
	static void Interrupt_Controller()
	{
		if ( tc0140syt.nmi_req!=0 && tc0140syt.nmi_enabled!=0 )
		{
			cpu_cause_interrupt( 1, Z80_NMI_INT );
			tc0140syt.nmi_req = 0;
		}
	}
	
	public static WriteHandlerPtr taitosound_port_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		data &= 0x0f;
	
		tc0140syt.mainmode = data;
		//logerror("taitosnd: Master cpu mode [%02x]\n", data);
		if (data > 4)
		{
			logerror("tc0140syt : error Master entering unknown mode[%02x]\n", data);
		}
	} };
	
	public static WriteHandlerPtr taitosound_comm_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	
		data &= 0x0f;	/*this is important, otherwise ballbros won't work*/
	
		switch( tc0140syt.mainmode )
		{
			case 0x00:		// mode #0
				tc0140syt.slavedata[tc0140syt.mainmode ++] = data;
				//logerror("taitosnd: Master cpu written port 0, data %01x\n", data);
				break;
				
			case 0x01:		// mode #1
				tc0140syt.slavedata[tc0140syt.mainmode ++] = data;
				tc0140syt.status |= TC0140SYT_PORT01_FULL;
				tc0140syt.nmi_req = 1;
				//logerror("taitosnd: Master cpu sends 0/1 : %01x%01x\n",tc0140syt.slavedata[1],tc0140syt.slavedata[0]);
	        	break;
				
			case 0x02:		// mode #2
				tc0140syt.slavedata[tc0140syt.mainmode ++] = data;
				//logerror("taitosnd: Master cpu written port 2, data %01\n", data);
				break;
				
			case 0x03:		// mode #3
				tc0140syt.slavedata[tc0140syt.mainmode ++] = data;
				tc0140syt.status |= TC0140SYT_PORT23_FULL;
				tc0140syt.nmi_req = 1;
				//logerror("taitosnd: Master cpu sends 2/3 : %01x%01x\n",tc0140syt.slavedata[3],tc0140syt.slavedata[2]);
				break;
				
			case 0x04:		// port status
	//#ifdef REPORT_DATA_FLOW
				//logerror("taitosnd: Master issued control value %02x (PC = %08x) \n",data, cpu_get_pc() );
	//#endif
				/* this does a hi-lo transition to reset the sound cpu */
				if (data != 0)
					cpu_set_reset_line(1,ASSERT_LINE);
				else
				{
					cpu_set_reset_line(1,CLEAR_LINE);
	                cpu_spin(); /* otherwise no sound in driftout */
	            }
				break;
				
			default:
				logerror("taitosnd: Master cpu written in mode [%02x] data[%02x]\n",tc0140syt.mainmode, data);
		}
	
	} };
	
	public static ReadHandlerPtr taitosound_comm_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		switch( tc0140syt.mainmode )
		{
			case 0x00:		// mode #0
				//logerror("taitosnd: Master cpu read portdata %01x\n", tc0140syt.masterdata[0]);
				return tc0140syt.masterdata[tc0140syt.mainmode ++];
				//break;
				
			case 0x01:		// mode #1
				//logerror("taitosnd: Master cpu receives 0/1 : %01x%01x\n", tc0140syt.masterdata[1],tc0140syt.masterdata[0]);
				tc0140syt.status &= ~TC0140SYT_PORT01_FULL_MASTER;
				return tc0140syt.masterdata[tc0140syt.mainmode ++];
				//break;
				
			case 0x02:		// mode #2
				//logerror("taitosnd: Master cpu read masterdata %01x\n", tc0140syt.masterdata[2]);
				return tc0140syt.masterdata[tc0140syt.mainmode ++];
				//break;
				
			case 0x03:		// mode #3
				//logerror("taitosnd: Master cpu receives 2/3 : %01x%01x\n", tc0140syt.masterdata[3],tc0140syt.masterdata[2]);
				tc0140syt.status &= ~TC0140SYT_PORT23_FULL_MASTER;
				return tc0140syt.masterdata[tc0140syt.mainmode ++];
				//break;
				
			case 0x04:		// port status
				//logerror("tc0140syt : Master cpu read status : %02x\n", tc0140syt.status);
				return tc0140syt.status;
				//break;
				
			default:
				logerror("tc0140syt : Master cpu read in mode [%02x]\n", tc0140syt.mainmode);
				return 0;
		}
	} };
	
	//SLAVE SIDE
	
	public static WriteHandlerPtr taitosound_slave_port_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		data &= 0x0f;
		tc0140syt.submode = data;
		//logerror("taitosnd: Slave cpu mode [%02x]\n", data);
		if (data > 6)
			logerror("tc0140syt error : Slave cpu unknown mode[%02x]\n", data);
	} };
	
	public static WriteHandlerPtr taitosound_slave_comm_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		data &= 0x0f;
	
		switch ( tc0140syt.submode )
		{
			case 0x00:		// mode #0
				tc0140syt.masterdata[tc0140syt.submode ++] = data;
				//logerror("taitosnd: Slave cpu written port 0, data %01x\n", data);
				break;
				
			case 0x01:		// mode #1
				tc0140syt.masterdata[tc0140syt.submode ++] = data;
				tc0140syt.status |= TC0140SYT_PORT01_FULL_MASTER;
				//logerror("taitosnd: Slave cpu sends 0/1 : %01x%01x\n",tc0140syt.masterdata[1],tc0140syt.masterdata[0]);
				cpu_spin(); /* writing should take longer than emulated, so spin */
				break;
				
			case 0x02:		// mode #2
				//logerror("taitosnd: Slave cpu written port 2, data %01x\n", data);
				tc0140syt.masterdata[tc0140syt.submode ++] = data;
				break;
				
			case 0x03:		// mode #3
				tc0140syt.masterdata[tc0140syt.submode ++] = data;
				tc0140syt.status |= TC0140SYT_PORT23_FULL_MASTER;
				//logerror("taitosnd: Slave cpu sends 2/3 : %01x%01x\n",tc0140syt.masterdata[3],tc0140syt.masterdata[2]);
				cpu_spin(); /* writing should take longer than emulated, so spin */
				break;
				
			case 0x04:		// port status
				//tc0140syt.status = TC0140SYT_SET_OK;
				//logerror("tc0140syt : Slave cpu status ok.\n");
				break;
				
			case 0x05:		// nmi disable
				tc0140syt.nmi_enabled = 0;
				break;
				
			case 0x06:		// nmi enable
				tc0140syt.nmi_enabled = 1;
				break;
				
			default:
				logerror("tc0140syt: Slave cpu written in mode [%02x] data[%02x]\n",tc0140syt.submode, data & 0xff);
		}
	
		Interrupt_Controller();
	
	} };
	
	public static ReadHandlerPtr taitosound_slave_comm_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		int res = 0;
	
		switch ( tc0140syt.submode )
		{
			case 0x00:		// mode #0
				//logerror("taitosnd: Slave cpu read slavedata %01x\n", tc0140syt.slavedata[0]);
				res = tc0140syt.slavedata[tc0140syt.submode ++];
				break;
				
			case 0x01:		// mode #1
				//logerror("taitosnd: Slave cpu receives 0/1 : %01x%01x PC=%4x\n", tc0140syt.slavedata[1],tc0140syt.slavedata[0],cpu_get_pc());
				tc0140syt.status &= ~TC0140SYT_PORT01_FULL;
				res = tc0140syt.slavedata[tc0140syt.submode ++];
				break;
				
			case 0x02:		// mode #2
				//logerror("taitosnd: Slave cpu read slavedata %01x\n", tc0140syt.slavedata[2]);
				res = tc0140syt.slavedata[tc0140syt.submode ++];
				break;
				
			case 0x03:		// mode #3
				//logerror("taitosnd: Slave cpu receives 2/3 : %01x%01x\n", tc0140syt.slavedata[3],tc0140syt.slavedata[2]);
				tc0140syt.status &= ~TC0140SYT_PORT23_FULL;
				res = tc0140syt.slavedata[tc0140syt.submode ++];
				break;
				
			case 0x04:		// port status
				//logerror("tc0140syt : Slave cpu read status : %02x\n", tc0140syt.status);
				res = tc0140syt.status;
				break;
				
			default:
				logerror("tc0140syt : Slave cpu read in mode [%02x]\n", tc0140syt.submode);
				res = 0;
		}
	
		Interrupt_Controller();
	
	    return res;
	} };
	
	
	
	
	
	
	
	/* wrapper functions for 16bit handlers */
	
	public static WriteHandlerPtr taitosound_port16_lsb_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*TODO*///if (ACCESSING_LSB)
			taitosound_port_w.handler(0,data & 0xff);
            }
        };
	
	public static WriteHandlerPtr taitosound_comm16_lsb_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		/*TODO*///if (ACCESSING_LSB)
			taitosound_comm_w.handler(0,data & 0xff);
            }
	};
        
	public static ReadHandlerPtr taitosound_comm16_lsb_r = new ReadHandlerPtr() {
            public int handler(int offset) {
		return taitosound_comm_r.handler(0);
            }
	};
	
	
	public static WriteHandlerPtr taitosound_port16_msb_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		/*TODO*///if (ACCESSING_MSB)
			taitosound_port_w.handler(0,data >> 8);
            }
	};
        
	public static WriteHandlerPtr taitosound_comm16_msb_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
		/*TODO*///if (ACCESSING_MSB)
			taitosound_comm_w.handler(0,data >> 8);
            }
	};
        
	public static ReadHandlerPtr taitosound_comm16_msb_r = new ReadHandlerPtr() {
            public int handler(int offset) {
                return taitosound_comm_r.handler(0) << 8;
            }
        };
	
}
