/*****************************************************************************
 *
 * Asteroids Analog Sound system interface into discrete sound emulation
 * input mapping system.
 *
 *****************************************************************************/

/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package WIP.mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;

import static common.ptr.*;

import static mame056.sndintrfH.*;
import static mame056.mame.Machine;
import static mame056.common.*;
import static mame056.commonH.*;

import static mame056.sound.mixer.*;
import static mame037b11.sound.mixer.mixer_play_sample;

public class asteroid
{
	
	/************************************************************************/
	/* Asteroids Sound System Analog emulation by K.Wilkins Nov 2000        */
	/* Questions/Suggestions to mame@dysfunction.demon.co.uk                */
	/************************************************************************/
	
	/*TODO*///public static int ASTEROID_THUMP_ENAB = NODE_01;
	/*TODO*///public static int ASTEROID_THUMP_FREQ = NODE_02;
	/*TODO*///public static int ASTEROID_SAUCER_ENAB = NODE_03;
        /*TODO*///public static int ASTEROID_SAUCER_FIRE = NODE_04;
	/*TODO*///public static int ASTEROID_SAUCER_SEL = NODE_05;
	/*TODO*///public static int ASTEROID_THRUST_ENAB = NODE_06;
	/*TODO*///public static int ASTEROID_FIRE_ENAB = NODE_07;
	/*TODO*///public static int ASTEROID_LIFE_ENAB = NODE_08;
	/*TODO*///public static int ASTEROID_EXPLODE_NODE = NODE_09;
	/*TODO*///public static int ASTEROID_EXPLODE_PITCH = NODE_17;
	/*TODO*///public static int ASTEROID_THUMP_DUTY = NODE_18;
	
	/*TODO*///DISCRETE_SOUND_START(asteroid_sound_interface)
		/************************************************/
		/* Input register mapping for asteroids ,the    */
		/* registers are lumped in three groups for no  */
		/* other reason than they are controlled by 3   */
		/* registers on the schematics                  */
		/* Address values are also arbitary in here.    */
		/************************************************/
		/*                   NODE              ADDR   MASK INIT   GAIN        OFFSET */
		/*TODO*///DISCRETE_INPUT (ASTEROID_SAUCER_ENAB  ,0x00,0x003f,0)
		/*TODO*///DISCRETE_INPUT (ASTEROID_SAUCER_FIRE  ,0x01,0x003f,0)
		/*TODO*///DISCRETE_INPUT (ASTEROID_SAUCER_SEL   ,0x02,0x003f,0)
		/*TODO*///DISCRETE_INPUT (ASTEROID_THRUST_ENAB  ,0x03,0x003f,0)
		/*TODO*///DISCRETE_INPUT (ASTEROID_FIRE_ENAB    ,0x04,0x003f,0)
		/*TODO*///DISCRETE_INPUT (ASTEROID_LIFE_ENAB    ,0x05,0x003f,0)
	
		/*TODO*///DISCRETE_INPUT (ASTEROID_THUMP_ENAB   ,0x10,0x003f,0)
		/*TODO*///DISCRETE_INPUTX(ASTEROID_THUMP_FREQ   ,0x11,0x003f,(70.0/15.0)       ,20.0    ,0)
		/*TODO*///DISCRETE_INPUTX(ASTEROID_THUMP_DUTY   ,0x12,0x003f,(55.0/15.0)       ,33.0    ,0)
	
		/*TODO*///DISCRETE_INPUTX(ASTEROID_EXPLODE_NODE ,0x20,0x003f,((1.0/15.0)*100.0),0.0     ,0)
		/*TODO*///DISCRETE_INPUTX(ASTEROID_EXPLODE_PITCH,0x21,0x003f,1000.0            ,0.0     ,0)
	
		/************************************************/
		/* Thump circuit is based on a VCO with the     */
		/* VCO control fed from the 4 low order bits    */
		/* from /THUMP bit 4 controls the osc enable.   */
		/* A resistor ladder network is used to convert */
		/* the 4 bit value to an analog value.          */
		/*                                              */
		/* The VCO is implemented with a 555 timer and  */
		/* an RC filter to perform smoothing on the     */
		/* output                                       */
		/*                                              */
		/* The sound can be tweaked with the gain and   */
		/* adder constants in the 2 lines below         */
		/************************************************/
		/*TODO*///DISCRETE_SQUAREWAVE(NODE_11,ASTEROID_THUMP_ENAB,ASTEROID_THUMP_FREQ,100.0,ASTEROID_THUMP_DUTY,0)
		/*TODO*///DISCRETE_RCFILTER(NODE_10,1,NODE_11,3300,0.1e-6)
	
		/************************************************/
		/* The SAUCER sound is based on two VCOs, a     */
		/* slow VCO feed the input to a higher freq VCO */
		/* with the SAUCERSEL switch being used to move */
		/* the frequency ranges of both VCOs            */
		/*                                              */
		/* The slow VCO is implemented with a 555 timer */
		/* and a 566 is used for the higher VCO.        */
		/*                                              */
		/* The sound can be tweaked with the gain and   */
		/* adder constants in the 2 lines below         */
		/************************************************/
		/*TODO*///DISCRETE_GAIN(NODE_25,ASTEROID_SAUCER_SEL,-1000.0)		// Freq Pitch jump
		/*TODO*///DISCRETE_GAIN(NODE_24,ASTEROID_SAUCER_SEL,-4.0)			// Freq Warble jump
		/*TODO*///DISCRETE_ADDER2(NODE_23,1,NODE_24,7.0)
		/*TODO*///DISCRETE_SINEWAVE(NODE_22,1,NODE_23,1000.0,0)
		/*TODO*///DISCRETE_ADDER3(NODE_21,1,NODE_22,NODE_25,2500.0)
		/*TODO*///DISCRETE_SINEWAVE(NODE_20,ASTEROID_SAUCER_ENAB,NODE_22,100.0,0)
	
		/************************************************/
		/* The Fire sound is produced by a 555 based    */
		/* VCO where the frequency rapidly decays with  */
		/* time.                                        */
		/*                                              */
		/* An RC filter is used for the decay with the  */
		/* inverse of the enable signal used            */
		/************************************************/
		/*TODO*///DISCRETE_RCFILTER(NODE_34,1,ASTEROID_FIRE_ENAB,10000,1.0e-6)
		/*TODO*///DISCRETE_ADDER2(NODE_33,1,NODE_34,-1.0)
		/*TODO*///DISCRETE_INVERT(NODE_32,NODE_33)
		/*TODO*///DISCRETE_GAIN(NODE_31,NODE_32,2000.0)
		/*TODO*///DISCRETE_SINEWAVE(NODE_30,ASTEROID_FIRE_ENAB,NODE_31,100.0,0)
	
		/************************************************/
		/* The Fire sound is produced by a 555 based    */
		/* VCO where the frequency rapidly decays with  */
		/* time.                                        */
		/*                                              */
		/* An RC filter is used for the decay with the  */
		/* inverse of the enable signal used            */
		/************************************************/
		/*TODO*///DISCRETE_RCFILTER(NODE_44,1,ASTEROID_SAUCER_FIRE,10000,1.0e-6)
		/*TODO*///DISCRETE_ADDER2(NODE_43,1,NODE_44,-1.0)
		/*TODO*///DISCRETE_INVERT(NODE_42,NODE_43)
		/*TODO*///DISCRETE_GAIN(NODE_41,NODE_42,4000.0)
		/*TODO*///DISCRETE_SINEWAVE(NODE_40,ASTEROID_SAUCER_FIRE,NODE_41,100.0,0)
	
		/************************************************/
		/* Thrust noise is just a gated noise source    */
		/* fed into a low pass RC filter                */
		/************************************************/
		/*TODO*///DISCRETE_NOISE(NODE_51,ASTEROID_THRUST_ENAB,12000,100.0)
		/*TODO*///DISCRETE_RCFILTER(NODE_50,1,NODE_51,400,1e-6)
	
		/************************************************/
		/* Explosion generation circuit, pitch and vol  */
		/* are variable                                 */
		/************************************************/
		/*TODO*///DISCRETE_NOISE(NODE_61,1,ASTEROID_EXPLODE_PITCH,ASTEROID_EXPLODE_NODE)
		/*TODO*///DISCRETE_RCFILTER(NODE_60,1,NODE_61,400,1e-6)
	
		/************************************************/
		/* Life enable is just 3Khz tone from the clock */
		/* generation cct according to schematics       */
		/************************************************/
		/*TODO*///DISCRETE_SINEWAVE(NODE_70,ASTEROID_LIFE_ENAB,3000,100.0,0)
	
		/************************************************/
		/* Combine all 7 sound sources with a double    */
		/* adder circuit                                */
		/************************************************/
		/*TODO*///DISCRETE_ADDER4(NODE_91,1,NODE_10,NODE_20,NODE_30,NODE_40)
		/*TODO*///DISCRETE_ADDER4(NODE_92,1,NODE_50,NODE_60,NODE_70,NODE_91)
		/*TODO*///DISCRETE_GAIN(NODE_90,NODE_92,40.0)
	
		/*TODO*///DISCRETE_OUTPUT(NODE_90)														// Take the output from the mixer
	/*TODO*///DISCRETE_SOUND_END
	
	
	/*TODO*///DISCRETE_SOUND_START(astdelux_sound_interface)
		/************************************************/
		/* Asteroid delux sound hardware is mostly done */
		/* in the Pokey chip except for the thrust and  */
		/* explosion sounds that are a direct lift of   */
		/* the asteroids hardware hence is a clone of   */
		/* the circuit above apart from gain scaling.   */
		/*                                              */
		/* Note that the thrust enable signal is invert */
		/************************************************/
		/*TODO*///DISCRETE_INPUTX(ASTEROID_THRUST_ENAB  ,0x03,0x003f,-1.0              ,1.0,1.0)
		/*TODO*///DISCRETE_INPUTX(ASTEROID_EXPLODE_NODE ,0x20,0x003f,((1.0/15.0)*100.0),0.0,0.0)
		/*TODO*///DISCRETE_INPUTX(ASTEROID_EXPLODE_PITCH,0x21,0x003f,1000.0            ,0.0,0.0)
	
		/************************************************/
		/* Thrust noise is just a gated noise source    */
		/* fed into a low pass RC filter                */
		/************************************************/
		/*TODO*///DISCRETE_NOISE(NODE_51,ASTEROID_THRUST_ENAB,12000,100.0)
		/*TODO*///DISCRETE_RCFILTER(NODE_50,1,NODE_51,400,1e-6)
	
		/************************************************/
		/* Explosion generation circuit, pitch and vol  */
		/* are variable                                 */
		/************************************************/
		/*TODO*///DISCRETE_NOISE(NODE_61,1,ASTEROID_EXPLODE_PITCH,ASTEROID_EXPLODE_NODE)
		/*TODO*///DISCRETE_RCFILTER(NODE_60,1,NODE_61,400,1e-6)
	
		/************************************************/
		/* Combine all 7 sound sources with a double    */
		/* adder circuit                                */
		/************************************************/
		/*TODO*///DISCRETE_ADDER2(NODE_91,1,NODE_50,NODE_60)
		/*TODO*///DISCRETE_GAIN(NODE_90,NODE_91,160.0)
	
		/*TODO*///DISCRETE_OUTPUT(NODE_90)														// Take the output from the mixer
	/*TODO*///DISCRETE_SOUND_END
	
	
	public static WriteHandlerPtr asteroid_explode_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///discrete_sound_w(0x20,(data&0x3c)>>2);				// Volume
		/*TODO*///discrete_sound_w(0x21,12/(((data&0xc0)>>6)+1));		// Noise Pitch divider 12KHz / (1+Value) in KHz
	} };
	
	public static WriteHandlerPtr asteroid_thump_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///discrete_sound_w(0x10,data&0x10);			//Thump enable
		/*TODO*///discrete_sound_w(0x11,(data&0x0f)^0x0f);	//Thump frequency
		/*TODO*///discrete_sound_w(0x12,data&0x0f);			//Thump duty
	} };
	
	public static WriteHandlerPtr asteroid_sounds_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/*TODO*///discrete_sound_w(0x00+offset,(data&0x80)?1:0);
	} };
	
	public static WriteHandlerPtr astdelux_sounds_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		/* Only ever activates the thrusters in Astdelux */
		/*TODO*///discrete_sound_w(0x03,(data&0x80)?1:0);
	} };
	
	
}
