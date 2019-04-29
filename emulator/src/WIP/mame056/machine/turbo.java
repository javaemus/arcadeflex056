/*************************************************************************

	 Turbo - Sega - 1981

	 Machine Hardware

*************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.machine;

import static common.libc.cstring.*;
import static arcadeflex056.fucPtr.*;
import static mame056.inptport.*;
import static mame056.mame.*;
import static mame056.machine._8255ppiH.*;
import static mame056.machine._8255ppi.*;
import static mame056.sound.samples.*;

// refactor
import static arcadeflex036.osdepend.logerror;
import static WIP.mame056.vidhrdw.turbo.*;

public class turbo
{
	
	/* globals */
	public static int turbo_opa, turbo_opb, turbo_opc;
	public static int turbo_ipa, turbo_ipb, turbo_ipc;
	public static int turbo_fbpla, turbo_fbcol;
	public static int[] turbo_segment_data=new int[32];
	public static int turbo_speed;
	
	/* local data */
	public static int segment_address, segment_increment;
	public static int osel, bsel, accel;
	
	
	/*******************************************
	
		Sample handling
	
	*******************************************/
	
	static void update_samples()
	{
		/* accelerator sounds */
		/* BSEL == 3 --> off */
		/* BSEL == 2 --> standard */
		/* BSEL == 1 --> tunnel */
		/* BSEL == 0 --> ??? */
		if (bsel == 3 && (sample_playing(6)!=0))
			sample_stop(6);
		else if (bsel != 3 && (sample_playing(6)==0))
			sample_start(6, 7, 1);
		if (sample_playing(6) != 0)
	//		sample_set_freq(6, 44100 * (accel & 0x3f) / 7 + 44100);
			sample_set_freq(6, (int) (44100 * (accel & 0x3f) / 5.25 + 44100));
	}
	
	
	/*******************************************
	
		8255 PPI handling
	
	*******************************************/
	/*
		chip index:
		0 = IC75 - CPU Board, Sheet 6, D7
		1 = IC32 - CPU Board, Sheet 6, D6
		2 = IC123 - CPU Board, Sheet 6, D4
		3 = IC6 - CPU Board, Sheet 5, D7
	*/
	
	public static WriteHandlerPtr chip0_portA_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                turbo_opa = data;	/* signals 0PA0 to 0PA7 */
            }
        };
	
	public static WriteHandlerPtr chip0_portB_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                turbo_opb = data;	/* signals 0PB0 to 0PB7 */
            }
        };
	
	public static WriteHandlerPtr chip0_portC_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                turbo_opc = data;	/* signals 0PC0 to 0PC7 */
            }
        };
	
	public static WriteHandlerPtr chip1_portA_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                turbo_ipa = data;	/* signals 1PA0 to 1PA7 */
            }
        };
	
	public static WriteHandlerPtr chip1_portB_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                turbo_ipb = data;	/* signals 1PB0 to 1PB7 */
            }
        };
	
	public static WriteHandlerPtr chip1_portC_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                turbo_ipc = data;	/* signals 1PC0 to 1PC7 */
            }
        };
	
	public static WriteHandlerPtr chip2_portA_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*
			2PA0 = /CRASH
			2PA1 = /TRIG1
			2PA2 = /TRIG2
			2PA3 = /TRIG3
			2PA4 = /TRIG4
			2PA5 = OSEL0
			2PA6 = /SLIP
			2PA7 = /CRASHL
		*/
		/* missing short crash sample, but I've never seen it triggered */
		if ((data & 0x02) == 0) sample_start(0, 0, 0);
		if ((data & 0x04) == 0) sample_start(0, 1, 0);
		if ((data & 0x08) == 0) sample_start(0, 2, 0);
		if ((data & 0x10) == 0) sample_start(0, 3, 0);
		if ((data & 0x40) == 0) sample_start(1, 4, 0);
		if ((data & 0x80) == 0) sample_start(2, 5, 0);
		osel = (osel & 6) | ((data >> 5) & 1);
		update_samples();
            }
        };
	
	public static WriteHandlerPtr chip2_portB_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*
			2PB0 = ACC0
			2PB1 = ACC1
			2PB2 = ACC2
			2PB3 = ACC3
			2PB4 = ACC4
			2PB5 = ACC5
			2PB6 = /AMBU
			2PB7 = /SPIN
		*/
		accel = data & 0x3f;
		update_samples();
		if ((data & 0x40) == 0)
		{
			if (sample_playing(7) == 0)
				sample_start(7, 8, 0);
			else
				logerror("ambu didnt start\n");
		}
		else
			sample_stop(7);
		if ((data & 0x80)==0) sample_start(3, 6, 0);
            }
        };
	
	public static WriteHandlerPtr chip2_portC_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /*
			2PC0 = OSEL1
			2PC1 = OSEL2
			2PC2 = BSEL0
			2PC3 = BSEL1
			2PC4 = SPEED0
			2PC5 = SPEED1
			2PC6 = SPEED2
			2PC7 = SPEED3
		*/
		turbo_speed = (data >> 4) & 0x0f;
		bsel = (data >> 2) & 3;
		osel = (osel & 1) | ((data & 3) << 1);
		update_samples();
            }
        };
	
	public static WriteHandlerPtr chip3_portC_w = new WriteHandlerPtr() {
            public void handler(int offset, int data) {
                /* bit 0-3 = signals PLA0 to PLA3 */
		/* bit 4-6 = signals COL0 to COL2 */
		/* bit 7 = unused */
		turbo_fbpla = data & 0x0f;
		turbo_fbcol = (data & 0x70) >> 4;
            }
        };
	
	static ppi8255_interface intf = new ppi8255_interface
	(
		4, /* 4 chips */
		new ReadHandlerPtr[]{null, null, null, input_port_4_r}, /* Port A read */
		new ReadHandlerPtr[]{null, null, null, input_port_2_r}, /* Port B read */
		new ReadHandlerPtr[]{null, null, null, null}, /* Port C read */
		new WriteHandlerPtr[]{chip0_portA_w, chip1_portA_w, chip2_portA_w, null}, /* Port A write */
		new WriteHandlerPtr[]{chip0_portB_w, chip1_portB_w, chip2_portB_w, null}, /* Port B write */
		new WriteHandlerPtr[]{chip0_portC_w, chip1_portC_w, chip2_portC_w, chip3_portC_w} /* Port C write */
        );
	
	
	
	/*******************************************
	
		Machine Init
	
	*******************************************/
	
	public static InitMachinePtr turbo_init_machine = new InitMachinePtr() { public void handler() 
	{
		ppi8255_init(intf);
		segment_address = segment_increment = 0;
	} };
	
	
	/*******************************************
	
		8279 handling
		IC84 - CPU Board, Sheet 5, C7
	
	*******************************************/
	
	public static ReadHandlerPtr turbo_8279_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		if ((offset & 1) == 0)
			return readinputport(1);  /* DSW 1 */
		else
		{
			logerror("read 0xfc%02x\n", offset);
			return 0x10;
		}
	} };
	
	public static WriteHandlerPtr turbo_8279_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		switch (offset & 1)
		{
			case 0x00:
				turbo_segment_data[segment_address * 2] = data & 15;
				turbo_segment_data[segment_address * 2 + 1] = (data >> 4) & 15;
				segment_address = (segment_address + segment_increment) & 15;
				break;
	
			case 0x01:
				switch (data & 0xe0)
				{
					case 0x80:
						segment_address = data & 15;
						segment_increment = 0;
						break;
					case 0x90:
						segment_address = data & 15;
						segment_increment = 1;
						break;
					case 0xc0:
						memset(turbo_segment_data, 0, 32);
						break;
				}
				break;
		}
	} };
	
	
	/*******************************************
	
		Misc handling
	
	*******************************************/
	
	public static ReadHandlerPtr turbo_collision_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return readinputport(3) | (u8_turbo_collision & 15);
	} };
	
	public static WriteHandlerPtr turbo_collision_clear_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		u8_turbo_collision = 0;
	} };
	
	public static WriteHandlerPtr turbo_coin_and_lamp_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		data &= 1;
		switch (offset & 7)
		{
			case 0:		/* Coin Meter 1 */
			case 1:		/* Coin Meter 2 */
			case 2:		/* n/c */
				break;
	
			case 3:		/* Start Lamp */
				set_led_status(0, data & 1);
				break;
	
			case 4:		/* n/c */
			default:
				break;
		}
	} };
}
