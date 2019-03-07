/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */ 
package mame056.sndhrdw;

import static arcadeflex056.fucPtr.*;
import mame056.sndintrfH;
import static mame056.sound.mixer.*;
import static mame056.sound.streams.*;
import static mame056.timer.*;
import static mame056.timerH.*;

public class galaxian
{
/*TODO*///	
/*TODO*///	#define VERBOSE 0
/*TODO*///	
/*TODO*///	#define NEW_LFO 0
/*TODO*///	#define NEW_SHOOT 1
/*TODO*///	
/*TODO*///	#define XTAL		18432000
/*TODO*///	
/*TODO*///	#define SOUND_CLOCK (XTAL/6/2)			/* 1.536 MHz */
/*TODO*///	
/*TODO*///	#define SAMPLES 1
/*TODO*///	
/*TODO*///	#define RNG_RATE	(XTAL/3)			/* RNG clock is XTAL/3 */
/*TODO*///	#define NOISE_RATE	(XTAL/3/192/2/2)	/* 2V = 8kHz */
/*TODO*///	#define NOISE_LENGTH (NOISE_RATE*4) 	/* four seconds of noise */
/*TODO*///	
/*TODO*///	#define SHOOT_RATE 2672
/*TODO*///	#define SHOOT_LENGTH 13000
/*TODO*///	
/*TODO*///	#define TOOTHSAW_LENGTH 16
/*TODO*///	#define TOOTHSAW_VOLUME 36
/*TODO*///	#define STEPS 16
/*TODO*///	#define LFO_VOLUME 6
	public static int SHOOT_VOLUME = 50;
/*TODO*///	#define NOISE_VOLUME 50
/*TODO*///	#define NOISE_AMPLITUDE 70*256
/*TODO*///	#define TOOTHSAW_AMPLITUDE 64
/*TODO*///	
/*TODO*///	/* see comments in galaxian_sh_update() */
        public static int MINFREQ = (139-139/3);
	public static int MAXFREQ = (139+139/3);
/*TODO*///	
/*TODO*///	#if VERBOSE
/*TODO*///	#define LOG(x) logerror x
/*TODO*///	#else
/*TODO*///	#define LOG(x)
/*TODO*///	#endif
/*TODO*///	
	static timer_entry lfotimer = null;
	static int freq = MAXFREQ;
/*TODO*///	
/*TODO*///	#define STEP 1
/*TODO*///	
	static timer_entry noisetimer = null;
	static int noisevolume;
/*TODO*///	static INT16 *noisewave;
/*TODO*///	static INT16 *shootwave;
/*TODO*///	#if NEW_SHOOT
/*TODO*///	static int shoot_length;
/*TODO*///	static int shoot_rate;
/*TODO*///	#endif
/*TODO*///	
/*TODO*///	#if SAMPLES
/*TODO*///	static int shootsampleloaded = 0;
/*TODO*///	static int deathsampleloaded = 0;
/*TODO*///	static int last_port1=0;
/*TODO*///	#endif
	static int last_port2=0;
/*TODO*///	
/*TODO*///	static INT8 tonewave[4][TOOTHSAW_LENGTH];
	static int pitch,vol;
/*TODO*///	
/*TODO*///	static INT16 backgroundwave[32] =
/*TODO*///	{
/*TODO*///	   0x4000, 0x4000, 0x4000, 0x4000, 0x4000, 0x4000, 0x4000, 0x4000,
/*TODO*///	   0x4000, 0x4000, 0x4000, 0x4000, 0x4000, 0x4000, 0x4000, 0x4000,
/*TODO*///	  -0x4000,-0x4000,-0x4000,-0x4000,-0x4000,-0x4000,-0x4000,-0x4000,
/*TODO*///	  -0x4000,-0x4000,-0x4000,-0x4000,-0x4000,-0x4000,-0x4000,-0x4000,
/*TODO*///	};
/*TODO*///	
	static int channelnoise,channelshoot,channellfo;
	static int tone_stream;
/*TODO*///	
/*TODO*///	static void tone_update(int ch, INT16 *buffer, int length)
/*TODO*///	{
/*TODO*///		int i,j;
/*TODO*///		INT8 *w = tonewave[vol];
/*TODO*///		static int counter, countdown;
/*TODO*///	
/*TODO*///		/* only update if we have non-zero volume and frequency */
/*TODO*///		if( pitch != 0xff )
/*TODO*///		{
/*TODO*///			for (i = 0; i < length; i++)
/*TODO*///			{
/*TODO*///				int mix = 0;
/*TODO*///	
/*TODO*///				for (j = 0;j < STEPS;j++)
/*TODO*///				{
/*TODO*///					if (countdown >= 256)
/*TODO*///					{
/*TODO*///						counter = (counter + 1) % TOOTHSAW_LENGTH;
/*TODO*///						countdown = pitch;
/*TODO*///					}
/*TODO*///					countdown++;
/*TODO*///	
/*TODO*///					mix += w[counter];
/*TODO*///				}
/*TODO*///				*buffer++ = (mix << 8) / STEPS;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			for( i = 0; i < length; i++ )
/*TODO*///				*buffer++ = 0;
/*TODO*///		}
/*TODO*///	}
	
	public static WriteHandlerPtr galaxian_pitch_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		stream_update(tone_stream,0);
	
		pitch = data;
	} };
	
	public static WriteHandlerPtr galaxian_vol_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		stream_update(tone_stream,0);
	
		/* offset 0 = bit 0, offset 1 = bit 1 */
		vol = (vol & ~(1 << offset)) | ((data & 1) << offset);
	} };
	
	
	public static timer_callback noise_timer_cb = new timer_callback() {
            public void handler(int i) {
                if( noisevolume > 0 )
		{
			noisevolume -= (noisevolume / 10) + 1;
			mixer_set_volume(channelnoise,noisevolume);
		}
            }
        };
	
	public static WriteHandlerPtr galaxian_noise_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
/*TODO*///	#if SAMPLES
/*TODO*///		if (deathsampleloaded)
/*TODO*///		{
/*TODO*///			if (data & 1 && !(last_port1 & 1))
/*TODO*///				mixer_play_sample(channelnoise,Machine->samples->sample[1]->data,
/*TODO*///						Machine->samples->sample[1]->length,
/*TODO*///						Machine->samples->sample[1]->smpfreq,
/*TODO*///						0);
/*TODO*///			last_port1=data;
/*TODO*///		}
/*TODO*///		else
/*TODO*///	#endif
/*TODO*///		{
			if(( data & 1 ) != 0)
			{
				if( noisetimer != null)
				{
					timer_remove(noisetimer);
					noisetimer = null;
				}
				noisevolume = 100;
				mixer_set_volume(channelnoise,noisevolume);
			}
			else
			{
				/* discharge C21, 22uF via 150k+22k R35/R36 */
				if (noisevolume == 100)
				{
					noisetimer = timer_pulse(TIME_IN_USEC(0.693*(155000+22000)*22 / 100), 0, noise_timer_cb);
				}
			}
/*TODO*///		}
	} };
	
	public static WriteHandlerPtr galaxian_shoot_enable_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
		if( ((data & 1) != 0) && ((last_port2 & 1)==0) )
		{
/*TODO*///	#if SAMPLES
/*TODO*///			if( shootsampleloaded )
/*TODO*///			{
/*TODO*///				mixer_play_sample(channelshoot,Machine->samples->sample[0]->data,
/*TODO*///						Machine->samples->sample[0]->length,
/*TODO*///						Machine->samples->sample[0]->smpfreq,
/*TODO*///						0);
/*TODO*///			}
/*TODO*///			else
/*TODO*///	#endif
/*TODO*///			{
/*TODO*///	#if NEW_SHOOT
/*TODO*///				mixer_play_sample_16(channelshoot, shootwave, shoot_length, shoot_rate, 0);
/*TODO*///	#else
/*TODO*///				mixer_play_sample_16(channelshoot, shootwave, SHOOT_LENGTH, 10*SHOOT_RATE, 0);
/*TODO*///	#endif
				mixer_set_volume(channelshoot,SHOOT_VOLUME);
/*TODO*///			}
		}
		last_port2=data;
	} };
/*TODO*///	
/*TODO*///	
/*TODO*///	#if SAMPLES
/*TODO*///	static const char *galaxian_sample_names[] =
/*TODO*///	{
/*TODO*///		"*galaxian",
/*TODO*///		"shot.wav",
/*TODO*///		"death.wav",
/*TODO*///		0	/* end of array */
/*TODO*///	};
/*TODO*///	#endif
/*TODO*///	
	public static ShStartPtr galaxian_sh_start = new ShStartPtr() {
            public int handler(sndintrfH.MachineSound msound) {
                return 0;
            }
        };
/*TODO*///	{
/*TODO*///		int i, j, sweep, charge, countdown, generator, bit1, bit2;
/*TODO*///		int lfovol[3] = {LFO_VOLUME,LFO_VOLUME,LFO_VOLUME};
/*TODO*///	
/*TODO*///	#if SAMPLES
/*TODO*///		Machine->samples = readsamples(galaxian_sample_names,Machine->gamedrv->name);
/*TODO*///	#endif
/*TODO*///	
/*TODO*///		channelnoise = mixer_allocate_channel(NOISE_VOLUME);
/*TODO*///		mixer_set_name(channelnoise,"Noise");
/*TODO*///		channelshoot = mixer_allocate_channel(SHOOT_VOLUME);
/*TODO*///		mixer_set_name(channelshoot,"Shoot");
/*TODO*///		channellfo = mixer_allocate_channels(3,lfovol);
/*TODO*///		mixer_set_name(channellfo+0,"Background #0");
/*TODO*///		mixer_set_name(channellfo+1,"Background #1");
/*TODO*///		mixer_set_name(channellfo+2,"Background #2");
/*TODO*///	
/*TODO*///	#if SAMPLES
/*TODO*///		if (Machine->samples != 0 && Machine->samples->sample[0] != 0)	/* We should check also that Samplename[0] = 0 */
/*TODO*///			shootsampleloaded = 1;
/*TODO*///		else
/*TODO*///			shootsampleloaded = 0;
/*TODO*///	
/*TODO*///		if (Machine->samples != 0 && Machine->samples->sample[1] != 0)	/* We should check also that Samplename[0] = 0 */
/*TODO*///			deathsampleloaded = 1;
/*TODO*///		else
/*TODO*///			deathsampleloaded = 0;
/*TODO*///	#endif
/*TODO*///	
/*TODO*///		if( (noisewave = malloc(NOISE_LENGTH * sizeof(INT16))) == 0 )
/*TODO*///		{
/*TODO*///			return 1;
/*TODO*///		}
/*TODO*///	
/*TODO*///	#if NEW_SHOOT
/*TODO*///	#define SHOOT_SEC 2
/*TODO*///		shoot_rate = Machine->sample_rate;
/*TODO*///		shoot_length = SHOOT_SEC * shoot_rate;
/*TODO*///		if ((shootwave = malloc(shoot_length * sizeof(INT16))) == 0)
/*TODO*///	#else
/*TODO*///		if( (shootwave = malloc(SHOOT_LENGTH * sizeof(INT16))) == 0 )
/*TODO*///	#endif
/*TODO*///		{
/*TODO*///			free(noisewave);
/*TODO*///			return 1;
/*TODO*///		}
/*TODO*///	
/*TODO*///		/*
/*TODO*///		 * The RNG shifter is clocked with RNG_RATE, bit 17 is
/*TODO*///		 * latched every 2V cycles (every 2nd scanline).
/*TODO*///		 * This signal is used as a noise source.
/*TODO*///		 */
/*TODO*///		generator = 0;
/*TODO*///		countdown = NOISE_RATE / 2;
/*TODO*///		for( i = 0; i < NOISE_LENGTH; i++ )
/*TODO*///		{
/*TODO*///			countdown -= RNG_RATE;
/*TODO*///			while( countdown < 0 )
/*TODO*///			{
/*TODO*///				generator <<= 1;
/*TODO*///				bit1 = (~generator >> 17) & 1;
/*TODO*///				bit2 = (generator >> 5) & 1;
/*TODO*///				if (bit1 ^ bit2) generator |= 1;
/*TODO*///				countdown += NOISE_RATE;
/*TODO*///			}
/*TODO*///			noisewave[i] = ((generator >> 17) & 1) ? NOISE_AMPLITUDE : -NOISE_AMPLITUDE;
/*TODO*///		}
/*TODO*///	
/*TODO*///	#if NEW_SHOOT
/*TODO*///	
/*TODO*///		/* dummy */
/*TODO*///		sweep = 100;
/*TODO*///		charge = +2;
/*TODO*///		j=0;
/*TODO*///		{
/*TODO*///	#define R41__ 100000
/*TODO*///	#define R44__ 10000
/*TODO*///	#define R45__ 22000
/*TODO*///	#define R46__ 10000
/*TODO*///	#define R47__ 2200
/*TODO*///	#define R48__ 2200
/*TODO*///	#define C25__ 0.000001
/*TODO*///	#define C27__ 0.00000001
/*TODO*///	#define C28__ 0.000047
/*TODO*///	#define C29__ 0.00000001
/*TODO*///	#define IC8L3_L 0.2   /* 7400 L level */
/*TODO*///	#define IC8L3_H 4.5   /* 7400 H level */
/*TODO*///	#define NOISE_L 0.2   /* 7474 L level */
/*TODO*///	#define NOISE_H 4.5   /* 7474 H level */
/*TODO*///	/*
/*TODO*///		key on/off time is programmable
/*TODO*///		Therefore,  it is necessary to make separate sample with key on/off.
/*TODO*///		And,  calculate the playback point according to the voltage of c28.
/*TODO*///	*/
/*TODO*///	#define SHOOT_KEYON_TIME 0.1  /* second */
/*TODO*///	/*
/*TODO*///		NE555-FM input calculation is wrong.
/*TODO*///		The frequency is not proportional to the voltage of FM input.
/*TODO*///		And,  duty will be changed,too.
/*TODO*///	*/
/*TODO*///	#define NE555_FM_ADJUST_RATE 0.80
/*TODO*///			/* discharge : 100K * 1uF */
/*TODO*///			double v  = 5.0;
/*TODO*///			double vK = (shoot_rate) ? exp(-1 / (R41__*C25__) / shoot_rate) : 0;
/*TODO*///			/* -- SHOOT KEY port -- */
/*TODO*///			double IC8L3 = IC8L3_L; /* key on */
/*TODO*///			int IC8Lcnt = SHOOT_KEYON_TIME * shoot_rate; /* count for key off */
/*TODO*///			/* C28 : KEY port capacity */
/*TODO*///			/*       connection : 8L-3 - R47(2.2K) - C28(47u) - R48(2.2K) - C29 */
/*TODO*///			double c28v = IC8L3_H - (IC8L3_H-(NOISE_H+NOISE_L)/2)/(R46__+R47__+R48__)*R47__;
/*TODO*///			double c28K = (shoot_rate) ? exp(-1 / (22000 * 0.000047 ) / shoot_rate) : 0;
/*TODO*///			/* C29 : NOISE capacity */
/*TODO*///			/*       connection : NOISE - R46(10K) - C29(0.1u) - R48(2.2K) - C28 */
/*TODO*///			double c29v  = IC8L3_H - (IC8L3_H-(NOISE_H+NOISE_L)/2)/(R46__+R47__+R48__)*(R47__+R48__);
/*TODO*///			double c29K1 = (shoot_rate) ? exp(-1 / (22000  * 0.00000001 ) / shoot_rate) : 0; /* form C28   */
/*TODO*///			double c29K2 = (shoot_rate) ? exp(-1 / (100000 * 0.00000001 ) / shoot_rate) : 0; /* from noise */
/*TODO*///			/* NE555 timer */
/*TODO*///			/* RA = 10K , RB = 22K , C=.01u ,FM = C29 */
/*TODO*///			double ne555cnt = 0;
/*TODO*///			double ne555step = (shoot_rate) ? ((1.44/((R44__+R45__*2)*C27__)) / shoot_rate) : 0;
/*TODO*///			double ne555duty = (double)(R44__+R45__)/(R44__+R45__*2); /* t1 duty */
/*TODO*///			double ne555sr;		/* threshold (FM) rate */
/*TODO*///			/* NOISE source */
/*TODO*///			double ncnt  = 0.0;
/*TODO*///			double nstep = (shoot_rate) ? ((double)NOISE_RATE / shoot_rate) : 0;
/*TODO*///			double noise_sh2; /* voltage level */
/*TODO*///	
/*TODO*///			for( i = 0; i < shoot_length; i++ )
/*TODO*///			{
/*TODO*///				/* noise port */
/*TODO*///				noise_sh2 = noisewave[(int)ncnt % NOISE_LENGTH] == NOISE_AMPLITUDE ? NOISE_H : NOISE_L;
/*TODO*///				ncnt+=nstep;
/*TODO*///				/* calculate NE555 threshold level by FM input */
/*TODO*///				ne555sr = c29v*NE555_FM_ADJUST_RATE / (5.0*2/3);
/*TODO*///				/* calc output */
/*TODO*///				ne555cnt += ne555step;
/*TODO*///				if( ne555cnt >= ne555sr) ne555cnt -= ne555sr;
/*TODO*///				if( ne555cnt < ne555sr*ne555duty )
/*TODO*///				{
/*TODO*///					 /* t1 time */
/*TODO*///					shootwave[i] = v/5*0x7fff;
/*TODO*///					/* discharge output level */
/*TODO*///					if(IC8L3==IC8L3_H)
/*TODO*///						v *= vK;
/*TODO*///				}
/*TODO*///				else
/*TODO*///					shootwave[i] = 0;
/*TODO*///				/* C28 charge/discharge */
/*TODO*///				c28v += (IC8L3-c28v) - (IC8L3-c28v)*c28K;	/* from R47 */
/*TODO*///				c28v += (c29v-c28v) - (c29v-c28v)*c28K;		/* from R48 */
/*TODO*///				/* C29 charge/discharge */
/*TODO*///				c29v += (c28v-c29v) - (c28v-c29v)*c29K1;	/* from R48 */
/*TODO*///				c29v += (noise_sh2-c29v) - (noise_sh2-c29v)*c29K2;	/* from R46 */
/*TODO*///				/* key off */
/*TODO*///				if(IC8L3==IC8L3_L && --IC8Lcnt==0)
/*TODO*///					IC8L3=IC8L3_H;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	#else
/*TODO*///		/*
/*TODO*///		 * Ra is 10k, Rb is 22k, C is 0.01uF
/*TODO*///		 * charge time t1 = 0.693 * (Ra + Rb) * C -> 221.76us
/*TODO*///		 * discharge time t2 = 0.693 * (Rb) *  C -> 152.46us
/*TODO*///		 * average period 374.22us -> 2672Hz
/*TODO*///		 * I use an array of 10 values to define some points
/*TODO*///		 * of the charge/discharge curve. The wave is modulated
/*TODO*///		 * using the charge/discharge timing of C28, a 47uF capacitor,
/*TODO*///		 * over a 2k2 resistor. This will change the frequency from
/*TODO*///		 * approx. Favg-Favg/3 up to Favg+Favg/3 down to Favg-Favg/3 again.
/*TODO*///		 */
/*TODO*///		sweep = 100;
/*TODO*///		charge = +2;
/*TODO*///		countdown = sweep / 2;
/*TODO*///		for( i = 0, j = 0; i < SHOOT_LENGTH; i++ )
/*TODO*///		{
/*TODO*///			#define AMP(n)	(n)*0x8000/100-0x8000
/*TODO*///			static int charge_discharge[10] = {
/*TODO*///				AMP( 0), AMP(25), AMP(45), AMP(60), AMP(70), AMP(85),
/*TODO*///				AMP(70), AMP(50), AMP(25), AMP( 0)
/*TODO*///			};
/*TODO*///			shootwave[i] = charge_discharge[j];
/*TODO*///			LOG(("shoot[%5d] $%04x (sweep: %3d, j:%d)\n", i, shootwave[i] & 0xffff, sweep, j));
/*TODO*///			/*
/*TODO*///			 * The current sweep and a 2200/10000 fraction (R45 and R48)
/*TODO*///			 * of the noise are frequency modulating the NE555 chip.
/*TODO*///			 */
/*TODO*///			countdown -= sweep + noisewave[i % NOISE_LENGTH] / (2200*NOISE_AMPLITUDE/10000);
/*TODO*///			while( countdown < 0 )
/*TODO*///			{
/*TODO*///				countdown += 100;
/*TODO*///				j = ++j % 10;
/*TODO*///			}
/*TODO*///			/* sweep from 100 to 133 and down to 66 over the time of SHOOT_LENGTH */
/*TODO*///			if( i % (SHOOT_LENGTH / 33 / 3 ) == 0 )
/*TODO*///			{
/*TODO*///				sweep += charge;
/*TODO*///				if( sweep >= 133 )
/*TODO*///					charge = -1;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	#endif
/*TODO*///	
/*TODO*///		memset(tonewave, 0, sizeof(tonewave));
/*TODO*///	
/*TODO*///		for( i = 0; i < TOOTHSAW_LENGTH; i++ )
/*TODO*///		{
/*TODO*///			#define V(r0,r1) 2*TOOTHSAW_AMPLITUDE*(r0)/(r0+r1)-TOOTHSAW_AMPLITUDE
/*TODO*///			double r0a = 1.0/1e12, r1a = 1.0/1e12;
/*TODO*///			double r0b = 1.0/1e12, r1b = 1.0/1e12;
/*TODO*///	
/*TODO*///			/* #0: VOL1=0 and VOL2=0
/*TODO*///			 * only the 33k and the 22k resistors R51 and R50
/*TODO*///			 */
/*TODO*///			if( i & 1 )
/*TODO*///			{
/*TODO*///				r1a += 1.0/33000;
/*TODO*///				r1b += 1.0/33000;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				r0a += 1.0/33000;
/*TODO*///				r0b += 1.0/33000;
/*TODO*///			}
/*TODO*///			if( i & 4 )
/*TODO*///			{
/*TODO*///				r1a += 1.0/22000;
/*TODO*///				r1b += 1.0/22000;
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				r0a += 1.0/22000;
/*TODO*///				r0b += 1.0/22000;
/*TODO*///			}
/*TODO*///			tonewave[0][i] = V(1.0/r0a, 1.0/r1a);
/*TODO*///	
/*TODO*///			/* #1: VOL1=1 and VOL2=0
/*TODO*///			 * add the 10k resistor R49 for bit QC
/*TODO*///			 */
/*TODO*///			if( i & 4 )
/*TODO*///				r1a += 1.0/10000;
/*TODO*///			else
/*TODO*///				r0a += 1.0/10000;
/*TODO*///			tonewave[1][i] = V(1.0/r0a, 1.0/r1a);
/*TODO*///	
/*TODO*///			/* #2: VOL1=0 and VOL2=1
/*TODO*///			 * add the 15k resistor R52 for bit QD
/*TODO*///			 */
/*TODO*///			if( i & 8 )
/*TODO*///				r1b += 1.0/15000;
/*TODO*///			else
/*TODO*///				r0b += 1.0/15000;
/*TODO*///			tonewave[2][i] = V(1.0/r0b, 1.0/r1b);
/*TODO*///	
/*TODO*///			/* #3: VOL1=1 and VOL2=1
/*TODO*///			 * add the 10k resistor R49 for QC
/*TODO*///			 */
/*TODO*///			if( i & 4 )
/*TODO*///				r0b += 1.0/10000;
/*TODO*///			else
/*TODO*///				r1b += 1.0/10000;
/*TODO*///			tonewave[3][i] = V(1.0/r0b, 1.0/r1b);
/*TODO*///			LOG(("tone[%2d]: $%02x $%02x $%02x $%02x\n", i, tonewave[0][i], tonewave[1][i], tonewave[2][i], tonewave[3][i]));
/*TODO*///		}
/*TODO*///	
/*TODO*///		pitch = 0;
/*TODO*///		vol = 0;
/*TODO*///	
/*TODO*///		tone_stream = stream_init("Tone",TOOTHSAW_VOLUME,SOUND_CLOCK/STEPS,0,tone_update);
/*TODO*///	
/*TODO*///	#if SAMPLES
/*TODO*///		if (deathsampleloaded == 0)
/*TODO*///	#endif
/*TODO*///		{
/*TODO*///			mixer_set_volume(channelnoise,0);
/*TODO*///			mixer_play_sample_16(channelnoise,noisewave,NOISE_LENGTH,NOISE_RATE,1);
/*TODO*///		}
/*TODO*///	#if SAMPLES
/*TODO*///		if (shootsampleloaded == 0)
/*TODO*///	#endif
/*TODO*///		{
/*TODO*///			mixer_set_volume(channelshoot,0);
/*TODO*///			mixer_play_sample_16(channelshoot,shootwave,SHOOT_LENGTH,SHOOT_RATE,1);
/*TODO*///		}
/*TODO*///	
/*TODO*///		mixer_set_volume(channellfo+0,0);
/*TODO*///		mixer_play_sample_16(channellfo+0,backgroundwave,sizeof(backgroundwave),1000,1);
/*TODO*///		mixer_set_volume(channellfo+1,0);
/*TODO*///		mixer_play_sample_16(channellfo+1,backgroundwave,sizeof(backgroundwave),1000,1);
/*TODO*///		mixer_set_volume(channellfo+2,0);
/*TODO*///		mixer_play_sample_16(channellfo+2,backgroundwave,sizeof(backgroundwave),1000,1);
/*TODO*///	
/*TODO*///		return 0;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
	public static ShStopPtr galaxian_sh_stop = new ShStopPtr() {
            public void handler() {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
/*TODO*///	{
/*TODO*///		if( lfotimer )
/*TODO*///		{
/*TODO*///			timer_remove( lfotimer );
/*TODO*///			lfotimer = 0;
/*TODO*///		}
/*TODO*///		if( noisetimer )
/*TODO*///		{
/*TODO*///			timer_remove(noisetimer);
/*TODO*///			noisetimer = 0;
/*TODO*///		}
/*TODO*///		mixer_stop_sample(channelnoise);
/*TODO*///		mixer_stop_sample(channelshoot);
/*TODO*///		mixer_stop_sample(channellfo+0);
/*TODO*///		mixer_stop_sample(channellfo+1);
/*TODO*///		mixer_stop_sample(channellfo+2);
/*TODO*///		free(noisewave);
/*TODO*///		noisewave = 0;
/*TODO*///		free(shootwave);
/*TODO*///		shootwave = 0;
/*TODO*///	}
/*TODO*///	
	public static WriteHandlerPtr galaxian_background_enable_w = new WriteHandlerPtr() {
            public void handler(int offset, int data){
		mixer_set_volume(channellfo+offset,((data & 1)!=0) ? 100 : 0);
	} };
	
	public static timer_callback lfo_timer_cb = new timer_callback() {
            public void handler(int i) {
                if( freq > MINFREQ )
			freq--;
		else
			freq = MAXFREQ;
            }
        };
	
	public static WriteHandlerPtr galaxian_lfo_freq_w = new WriteHandlerPtr() {public void handler(int offset, int data)
	{
/*TODO*///	#if NEW_LFO
/*TODO*///		static int lfobit[4];
/*TODO*///	
/*TODO*///		/* R18 1M,R17 470K,R16 220K,R15 100K */
/*TODO*///		const int rv[4] = { 1000000,470000,220000,100000};
/*TODO*///		double r1,r2,Re,td;
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		if( (data & 1) == lfobit[offset] )
/*TODO*///			return;
/*TODO*///	
/*TODO*///		/*
/*TODO*///		 * NE555 9R is setup as astable multivibrator
/*TODO*///		 * - this circuit looks LINEAR RAMP V-F converter
/*TODO*///		   I  = 1/Re * ( R1/(R1+R2)-Vbe)
/*TODO*///		   td = (2/3VCC*Re*(R1+R2)*C) / (R1*VCC-Vbe*(R1+R2))
/*TODO*///		  parts assign
/*TODO*///		   R1  : (R15* L1)|(R16* L2)|(R17* L3)|(R18* L1)
/*TODO*///		   R2  : (R15*~L1)|(R16*~L2)|(R17*~L3)|(R18*~L4)|R??(330K)
/*TODO*///		   Re  : R21(100K)
/*TODO*///		   Vbe : Q2(2SA1015)-Vbe
/*TODO*///		 * - R20(15K) and Q1 is unknown,maybe current booster.
/*TODO*///		*/
/*TODO*///	
/*TODO*///		lfobit[offset] = data & 1;
/*TODO*///	
/*TODO*///		/* R20 15K */
/*TODO*///		r1 = 1e12;
/*TODO*///		/* R19? 330k to gnd */
/*TODO*///		r2 = 330000;
/*TODO*///		//r1 = 15000;
/*TODO*///		/* R21 100K */
/*TODO*///		Re = 100000;
/*TODO*///		/* register calculation */
/*TODO*///		for(i=0;i<4;i++)
/*TODO*///		{
/*TODO*///			if(lfobit[i])
/*TODO*///				r1 = (r1*rv[i])/(r1+rv[i]); /* Hi  */
/*TODO*///			else
/*TODO*///				r2 = (r2*rv[i])/(r2+rv[i]); /* Low */
/*TODO*///		}
/*TODO*///	
/*TODO*///		if( lfotimer )
/*TODO*///		{
/*TODO*///			timer_remove( lfotimer );
/*TODO*///			lfotimer = 0;
/*TODO*///		}
/*TODO*///	
/*TODO*///	#define Vcc 5.0
/*TODO*///	#define Vbe 0.65		/* 2SA1015 */
/*TODO*///	#define Cap 0.000001	/* C15 1uF */
/*TODO*///		td = (Vcc*2/3*Re*(r1+r2)*Cap) / (r1*Vcc - Vbe*(r1+r2) );
/*TODO*///	#undef Cap
/*TODO*///	#undef Vbe
/*TODO*///	#undef Vcc
/*TODO*///		logerror("lfo timer bits:%d%d%d%d r1:%d, r2:%d, re: %d, td: %9.2fsec\n", lfobit[0], lfobit[1], lfobit[2], lfobit[3], (int)r1, (int)r2, (int)Re, td);
/*TODO*///		lfotimer = timer_pulse( TIME_IN_SEC(td / (MAXFREQ-MINFREQ)), 0, lfo_timer_cb);
/*TODO*///	#else
		int[] lfobit=new int[4];
		double r0, r1, rx = 100000.0;
	
		if( (data & 1) == lfobit[offset] )
			return;
	
		/*
		 * NE555 9R is setup as astable multivibrator
		 * - Ra is between 100k and ??? (open?)
		 * - Rb is zero here (bridge between pins 6 and 7)
		 * - C is 1uF
		 * charge time t1 = 0.693 * (Ra + Rb) * C
		 * discharge time t2 = 0.693 * (Rb) *  C
		 * period T = t1 + t2 = 0.693 * (Ra + 2 * Rb) * C
		 * -> min period: 0.693 * 100 kOhm * 1uF -> 69300 us = 14.4Hz
		 * -> max period: no idea, since I don't know the max. value for Ra :(
		 */
	
		lfobit[offset] = data & 1;
	
		/* R?? 330k to gnd */
		r0 = 1.0/330000;
		/* open is a very high value really ;-) */
		r1 = 1.0/1e12;
	
		/* R18 1M */
		if( lfobit[0] != 0)
			r1 += 1.0/1000000;
		else
			r0 += 1.0/1000000;
	
		/* R17 470k */
		if( lfobit[1] != 0)
			r1 += 1.0/470000;
		else
			r0 += 1.0/470000;
	
		/* R16 220k */
		if( lfobit[2] != 0)
			r1 += 1.0/220000;
		else
			r0 += 1.0/220000;
	
		/* R15 100k */
		if( lfobit[3] != 0)
			r1 += 1.0/100000;
		else
			r0 += 1.0/100000;
	
		if( lfotimer != null)
		{
			timer_remove( lfotimer );
			lfotimer = null;
		}
	
		r0 = 1.0/r0;
		r1 = 1.0/r1;
	
		/* I used an arbitrary value for max. Ra of 2M */
		rx = rx + 2000000.0 * r0 / (r0+r1);
	
		/*TODO*///LOG(("lfotimer bits:%d%d%d%d r0:%d, r1:%d, rx: %d, time: %9.2fus\n", lfobit[3], lfobit[2], lfobit[1], lfobit[0], (int)r0, (int)r1, (int)rx, 0.639 * rx));
		lfotimer = timer_pulse( TIME_IN_USEC(0.639 * rx / (MAXFREQ-MINFREQ)), 0, lfo_timer_cb);
	/*TODO*///#endif
	} };
/*TODO*///	
	public static ShUpdatePtr galaxian_sh_update = new ShUpdatePtr() {
            public void handler() {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
            }
        };
/*TODO*///	{
/*TODO*///		/*
/*TODO*///		 * NE555 8R, 8S and 8T are used as pulse position modulators
/*TODO*///		 * FS1 Ra=100k, Rb=470k and C=0.01uF
/*TODO*///		 *	-> 0.693 * 1040k * 0.01uF -> 7207.2us = 139Hz
/*TODO*///		 * FS2 Ra=100k, Rb=330k and C=0.01uF
/*TODO*///		 *	-> 0.693 * 760k * 0.01uF -> 5266.8us = 190Hz
/*TODO*///		 * FS2 Ra=100k, Rb=220k and C=0.01uF
/*TODO*///		 *	-> 0.693 * 540k * 0.01uF -> 3742.2us = 267Hz
/*TODO*///		 */
/*TODO*///	
/*TODO*///		mixer_set_sample_frequency(channellfo+0, sizeof(backgroundwave)*freq*(100+2*470)/(100+2*470) );
/*TODO*///		mixer_set_sample_frequency(channellfo+1, sizeof(backgroundwave)*freq*(100+2*330)/(100+2*470) );
/*TODO*///		mixer_set_sample_frequency(channellfo+2, sizeof(backgroundwave)*freq*(100+2*220)/(100+2*470) );
/*TODO*///	}
}
