/***************************************************************************

  machine.c

  Written by Kenneth Lin (kenneth_lin@ai.vancouver.bc.ca)

  Functions to emulate general aspects of the machine (RAM, ROM, interrupts,
  I/O ports)

***************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.machine;

import static mame056.usrintrf.usrintf_showmessage;
import static arcadeflex056.fucPtr.*;
import static common.ptr.*;
import static mame056.commonH.*;
import static mame056.common.*;
import static mame056.memoryH.*;
import static mame056.memory.*;
import static mame056.vidhrdw.generic.dirtybuffer;

public class jackal
{
	
	static UBytePtr jackal_rambank = null;
	static UBytePtr jackal_spritebank = null;
	
	
	public static InitMachinePtr jackal_init_machine = new InitMachinePtr() { public void handler() 
	{
		cpu_setbank(1,new UBytePtr((memory_region(REGION_CPU1)),0x4000));
	 	jackal_rambank = new UBytePtr((memory_region(REGION_CPU1)),0);
		jackal_spritebank = new UBytePtr((memory_region(REGION_CPU1)),0);
	} };
	
	
	
	public static ReadHandlerPtr jackal_zram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return jackal_rambank.read(0x0020+offset);
	} };
	
	
	public static ReadHandlerPtr jackal_commonram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return jackal_rambank.read(0x0060+offset);
	} };
	
	
	public static ReadHandlerPtr jackal_commonram1_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return (memory_region(REGION_CPU1)).read(0x0060+offset);
	} };
	
	
	public static ReadHandlerPtr jackal_voram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return jackal_rambank.read(0x2000+offset);
	} };
	
	
	public static ReadHandlerPtr jackal_spriteram_r  = new ReadHandlerPtr() { public int handler(int offset)
	{
		return jackal_spritebank.read(0x3000+offset);
	} };
	
	
	public static WriteHandlerPtr jackal_rambank_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
	if ((data & 0xc4)!=0) usrintf_showmessage("jackal_rambank_w %02x",data);
		coin_counter_w.handler(0,data & 0x01);
		coin_counter_w.handler(1,data & 0x02);
		jackal_rambank = new UBytePtr((memory_region(REGION_CPU1)),((data & 0x10) << 12));
		jackal_spritebank = new UBytePtr((memory_region(REGION_CPU1)),((data & 0x08) << 13));
		cpu_setbank(1,new UBytePtr((memory_region(REGION_CPU1)),((data & 0x20) << 11) + 0x4000));
	} };
	
	
	public static WriteHandlerPtr jackal_zram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		jackal_rambank.write(0x0020+offset, data);
	} };
	
	
	public static WriteHandlerPtr jackal_commonram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		jackal_rambank.write(0x0060+offset, data);
	} };
	
	
	public static WriteHandlerPtr jackal_commonram1_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		(memory_region(REGION_CPU1)).write(0x0060+offset, data);
		(memory_region(REGION_CPU2)).write(0x6060+offset, data);
	} };
	
	
	public static WriteHandlerPtr jackal_voram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if ((offset & 0xF800) == 0)
		{
			dirtybuffer[offset & 0x3FF] = 1;
		}
		jackal_rambank.write(0x2000+offset, data);
	} };
	
	
	public static WriteHandlerPtr jackal_spriteram_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		jackal_spritebank.write(0x3000+offset, data);
	} };
}
