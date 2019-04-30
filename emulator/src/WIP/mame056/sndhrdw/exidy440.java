/*
 * ported to v0.56
 * using automatic conversion tool v0.01
 */
package WIP.mame056.sndhrdw;

import arcadeflex056.fucPtr.*;

import static common.ptr.*;

import static mame056.cpuexec.*;
import static mame056.cpuintrfH.*;
import mame056.sndintrfH.MachineSound;
import static mame056.sound.mixerH.*;

import static mame056.sound.streams.*;

public class exidy440 {

    /*TODO*///	#define	FADE_TO_ZERO	1
/*TODO*///	
/*TODO*///	
/*TODO*///	
    /* sample rates for each chip */
    public static final int SAMPLE_RATE_FAST = (12979200 / 256);/* FCLK */
    public static final int SAMPLE_RATE_SLOW = (12979200 / 512);/* SCLK */
 /*TODO*///	
/*TODO*///	/* internal caching */
/*TODO*///	#define	MAX_CACHE_ENTRIES		1024				/* maximum separate samples we expect to ever see */
/*TODO*///	#define	SAMPLE_BUFFER_LENGTH	1024				/* size of temporary decode buffer on the stack */
/*TODO*///	
/*TODO*///	/* FIR digital filter parameters */
/*TODO*///	#define	FIR_HISTORY_LENGTH		57					/* number of FIR coefficients */
/*TODO*///	
/*TODO*///	/* CVSD decoding parameters */
/*TODO*///	#define	INTEGRATOR_LEAK_TC		(10e3 * 0.1e-6)
/*TODO*///	#define	FILTER_DECAY_TC			((18e3 + 3.3e3) * 0.33e-6)
/*TODO*///	#define	FILTER_CHARGE_TC		(18e3 * 0.33e-6)
/*TODO*///	#define	FILTER_MIN				0.0416
/*TODO*///	#define	FILTER_MAX				1.0954
/*TODO*///	#define	SAMPLE_GAIN				10000.0
/*TODO*///	
    /* channel_data structure holds info about each 6844 DMA channel */
    public static class m6844_channel_data {

        int active;
        int address;
        int counter;
        int/*UINT8*/ u8_control;
        int start_address;
        int start_counter;

        public static m6844_channel_data[] create(int n) {
            m6844_channel_data[] a = new m6844_channel_data[n];
            for (int k = 0; k < n; k++) {
                a[k] = new m6844_channel_data();
            }
            return a;
        }
    }

    /* channel_data structure holds info about each active sound channel */
    public static class sound_channel_data {

        ShortPtr base;
        int offset;
        int remaining;

        public static sound_channel_data[] create(int n) {
            sound_channel_data[] a = new sound_channel_data[n];
            for (int k = 0; k < n; k++) {
                a[k] = new sound_channel_data();
            }
            return a;
        }
    }

    /*TODO*///	
/*TODO*///	/* sound_cache_entry structure contains info on each decoded sample */
/*TODO*///	typedef struct sound_cache_entry
/*TODO*///	{
/*TODO*///		struct sound_cache_entry *next;
/*TODO*///		int address;
/*TODO*///		int length;
/*TODO*///		int bits;
/*TODO*///		int frequency;
/*TODO*///		INT16 data[1];
/*TODO*///	} sound_cache_entry;
/*TODO*///	
/*TODO*///	
/*TODO*///	
    /* globals */
    public static UBytePtr exidy440_m6844_data = new UBytePtr();
    public static UBytePtr exidy440_sound_banks = new UBytePtr();
    public static UBytePtr exidy440_sound_volume = new UBytePtr();
    public static int /*UINT8*/ u8_exidy440_sound_command;
    public static int /*UINT8*/ u8_exidy440_sound_command_ack;

    /*TODO*///	/* local allocated storage */
/*TODO*///	static INT32 *mixer_buffer_left;
/*TODO*///	static INT32 *mixer_buffer_right;
/*TODO*///	static sound_cache_entry *sound_cache;
/*TODO*///	static sound_cache_entry *sound_cache_end;
/*TODO*///	static sound_cache_entry *sound_cache_max;
/*TODO*///	
    /* 6844 description */
    static m6844_channel_data[] m6844_channel = m6844_channel_data.create(4);
    static int m6844_priority;
    static int m6844_interrupt;
    static int m6844_chain;

    /* sound interface parameters */
    static int sound_stream;
    static sound_channel_data[] sound_channel = sound_channel_data.create(4);

    /* constant channel parameters */
    static int channel_frequency[]
            = {
                SAMPLE_RATE_FAST, SAMPLE_RATE_FAST,/* channels 0 and 1 are run by FCLK */
                SAMPLE_RATE_SLOW, SAMPLE_RATE_SLOW/* channels 2 and 3 are run by SCLK */};
    static int channel_bits[]
            = {
                4, 4,/* channels 0 and 1 are MC3418s, 4-bit CVSD */
                3, 3/* channels 2 and 3 are MC3417s, 3-bit CVSD */};

    /**
     * ***********************************
     *
     * Initialize the sound system
     *
     ************************************
     */
    public static ShStartPtr exidy440_sh_start = new ShStartPtr() {
        public int handler(MachineSound msound) {
            String names[]
                    = {
                        "Exidy 440 sound left",
                        "Exidy 440 sound right"
                    };
            int i, length;
            int[] vol = new int[2];

            /* reset the system */
            u8_exidy440_sound_command = 0;
            u8_exidy440_sound_command_ack = 1;

            /* reset the 6844 */
            for (i = 0; i < 4; i++) {
                m6844_channel[i].active = 0;
                m6844_channel[i].u8_control = 0x00;
            }
            m6844_priority = 0x00;
            m6844_interrupt = 0x00;
            m6844_chain = 0x00;

            /* get stream channels */
            vol[0] = MIXER(100, MIXER_PAN_LEFT);
            vol[1] = MIXER(100, MIXER_PAN_RIGHT);
            sound_stream = stream_init_multi(2, names, vol, SAMPLE_RATE_FAST, 0, channel_update);
            /*TODO*///	
/*TODO*///		/* allocate the sample cache */
/*TODO*///		length = memory_region_length(REGION_SOUND1) * 16 + MAX_CACHE_ENTRIES * sizeof(sound_cache_entry);
/*TODO*///		sound_cache = malloc(length);
/*TODO*///		if (sound_cache == 0)
/*TODO*///			return 1;
/*TODO*///	
/*TODO*///		/* determine the hard end of the cache and reset */
/*TODO*///		sound_cache_max = (sound_cache_entry *)((UINT8 *)sound_cache + length);
/*TODO*///		reset_sound_cache();
/*TODO*///	
/*TODO*///		/* allocate the mixer buffer */
/*TODO*///		mixer_buffer_left = malloc(2 * SAMPLE_RATE_FAST * sizeof(INT32));
/*TODO*///		if (mixer_buffer_left == 0)
/*TODO*///		{
/*TODO*///			free(sound_cache);
/*TODO*///			sound_cache = NULL;
/*TODO*///			return 1;
/*TODO*///		}
/*TODO*///		mixer_buffer_right = mixer_buffer_left + SAMPLE_RATE_FAST;
/*TODO*///	
            return 0;
        }
    };

    /**
     * ***********************************
     *
     * Tear down the sound system
     *
     ************************************
     */
    public static ShStopPtr exidy440_sh_stop = new ShStopPtr() {
        public void handler() {
            /*TODO*///		if (sound_cache)
/*TODO*///			free(sound_cache);
/*TODO*///		sound_cache = NULL;
/*TODO*///	
/*TODO*///		if (mixer_buffer_left)
/*TODO*///			free(mixer_buffer_left);
/*TODO*///		mixer_buffer_left = mixer_buffer_right = NULL;
        }
    };

    /**
     * ***********************************
     *
     * Periodic sound update
     *
     ************************************
     */
    public static ShUpdatePtr exidy440_sh_update = new ShUpdatePtr() {
        public void handler() {

        }
    };

    /**
     * ***********************************
     *
     * Add a bunch of samples to the mix
     *
     ************************************
     */
    /*TODO*///	
/*TODO*///	static void add_and_scale_samples(int ch, INT32 *dest, int samples, int volume)
/*TODO*///	{
/*TODO*///		sound_channel_data *channel = &sound_channel[ch];
/*TODO*///		INT16 *srcdata;
/*TODO*///		int i;
/*TODO*///	
/*TODO*///		/* channels 2 and 3 are half-rate samples */
/*TODO*///		if (ch & 2)
/*TODO*///		{
/*TODO*///			srcdata = &channel->base[channel->offset >> 1];
/*TODO*///	
/*TODO*///			/* handle the edge case */
/*TODO*///			if (channel->offset & 1)
/*TODO*///			{
/*TODO*///				*dest++ += *srcdata++ * volume / 256;
/*TODO*///				samples--;
/*TODO*///			}
/*TODO*///	
/*TODO*///			/* copy 1 for 2 to the destination */
/*TODO*///			for (i = 0; i < samples; i += 2)
/*TODO*///			{
/*TODO*///				INT16 sample = *srcdata++ * volume / 256;
/*TODO*///				*dest++ += sample;
/*TODO*///				*dest++ += sample;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* channels 0 and 1 are full-rate samples */
/*TODO*///		else
/*TODO*///		{
/*TODO*///			srcdata = &channel->base[channel->offset];
/*TODO*///			for (i = 0; i < samples; i++)
/*TODO*///				*dest++ += *srcdata++ * volume / 256;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Mix the result to 16 bits
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	static void mix_to_16(int length, INT16 *dest_left, INT16 *dest_right)
/*TODO*///	{
/*TODO*///		INT32 *mixer_left = mixer_buffer_left;
/*TODO*///		INT32 *mixer_right = mixer_buffer_right;
/*TODO*///		int i, clippers = 0;
/*TODO*///	
/*TODO*///		for (i = 0; i < length; i++)
/*TODO*///		{
/*TODO*///			INT32 sample_left = *mixer_left++;
/*TODO*///			INT32 sample_right = *mixer_right++;
/*TODO*///	
/*TODO*///			if (sample_left < -32768) { sample_left = -32768; clippers++; }
/*TODO*///			else if (sample_left > 32767) { sample_left = 32767; clippers++; }
/*TODO*///			if (sample_right < -32768) { sample_right = -32768; clippers++; }
/*TODO*///			else if (sample_right > 32767) { sample_right = 32767; clippers++; }
/*TODO*///	
/*TODO*///			*dest_left++ = sample_left;
/*TODO*///			*dest_right++ = sample_right;
/*TODO*///		}
/*TODO*///	}
    /**
     * ***********************************
     *
     * Stream callback
     *
     ************************************
     */
    public static StreamInitMultiPtr channel_update = new StreamInitMultiPtr() {
        public void handler(int ch, ShortPtr[] buffer, int length) {
            /*TODO*///		/* reset the mixer buffers */
/*TODO*///		memset(mixer_buffer_left, 0, length * sizeof(INT32));
/*TODO*///		memset(mixer_buffer_right, 0, length * sizeof(INT32));
/*TODO*///	
            /* loop over channels */
            for (ch = 0; ch < 4; ch++) {
                sound_channel_data channel = sound_channel[ch];
                int samples, volume, left = length;
                int effective_offset;

                /* if we're not active, bail */
                if (channel.remaining <= 0) {
                    continue;
                }

                /* see how many samples to copy */
                samples = (left > channel.remaining) ? channel.remaining : left;

                /*TODO*///			/* get a pointer to the sample data and copy to the left */
/*TODO*///			volume = exidy440_sound_volume[2 * ch + 0];
/*TODO*///			if (volume)
/*TODO*///				add_and_scale_samples(ch, mixer_buffer_left, samples, volume);
/*TODO*///	
/*TODO*///			/* get a pointer to the sample data and copy to the left */
/*TODO*///			volume = exidy440_sound_volume[2 * ch + 1];
/*TODO*///			if (volume)
/*TODO*///				add_and_scale_samples(ch, mixer_buffer_right, samples, volume);
/*TODO*///	
                /* update our counters */
                channel.offset += samples;
                channel.remaining -= samples;
                left -= samples;

                /* update the MC6844 */
                effective_offset = (ch & 2) != 0 ? channel.offset / 2 : channel.offset;
                m6844_channel[ch].address = m6844_channel[ch].start_address + effective_offset / 8;
                m6844_channel[ch].counter = m6844_channel[ch].start_counter - effective_offset / 8;
                if (m6844_channel[ch].counter <= 0) {
                    m6844_finished(ch);
                }
            }
            /*TODO*///	
/*TODO*///		/* all done, time to mix it */
/*TODO*///		mix_to_16(length, buffer[0], buffer[1]);
        }
    };
    /**
     * ***********************************
     *
     * Sound command register
     *
     ************************************
     */
    public static ReadHandlerPtr exidy440_sound_command_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            /* clear the FIRQ that got us here and acknowledge the read to the main CPU */
            cpu_set_irq_line(1, 1, CLEAR_LINE);
            u8_exidy440_sound_command_ack = 1;

            return u8_exidy440_sound_command;
        }
    };

    /**
     * ***********************************
     *
     * Sound volume registers
     *
     ************************************
     */
    public static WriteHandlerPtr exidy440_sound_volume_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            /* update the stream */
            stream_update(sound_stream, 0);

            /* set the new volume */
            exidy440_sound_volume.write(offset, ~data);
        }
    };

    /**
     * ***********************************
     *
     * Sound interrupt handling
     *
     ************************************
     */
    public static InterruptPtr exidy440_sound_interrupt = new InterruptPtr() {
        public int handler() {
            cpu_set_irq_line(1, 0, ASSERT_LINE);
            return ignore_interrupt.handler();
        }
    };

    public static WriteHandlerPtr exidy440_sound_interrupt_clear_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            cpu_set_irq_line(1, 0, CLEAR_LINE);
        }
    };

    /**
     * ***********************************
     *
     * MC6844 DMA controller interface
     *
     ************************************
     */
    public static void exidy440_m6844_update() {
        /* update the stream */
        stream_update(sound_stream, 0);
    }

    public static void m6844_finished(int ch) {
        m6844_channel_data channel = m6844_channel[ch];

        /* mark us inactive */
        channel.active = 0;

        /* set the final address and counter */
        channel.counter = 0;
        channel.address = channel.start_address + channel.start_counter;

        /* clear the DMA busy bit and set the DMA end bit */
        channel.u8_control = (channel.u8_control & ~0x40) & 0xFF;
        channel.u8_control = (channel.u8_control | 0x80) & 0xFF;
    }

    /**
     * ***********************************
     *
     * MC6844 DMA controller I/O
     *
     ************************************
     */
    public static ReadHandlerPtr exidy440_m6844_r = new ReadHandlerPtr() {
        public int handler(int offset) {
            int result = 0;

            /* first update the current state of the DMA transfers */
            exidy440_m6844_update();

            /* switch off the offset we were given */
            switch (offset) {
                /* upper byte of address */
                case 0x00:
                case 0x04:
                case 0x08:
                case 0x0c:
                    result = m6844_channel[offset / 4].address >> 8;
                    break;

                /* lower byte of address */
                case 0x01:
                case 0x05:
                case 0x09:
                case 0x0d:
                    result = m6844_channel[offset / 4].address & 0xff;
                    break;

                /* upper byte of counter */
                case 0x02:
                case 0x06:
                case 0x0a:
                case 0x0e:
                    result = m6844_channel[offset / 4].counter >> 8;
                    break;

                /* lower byte of counter */
                case 0x03:
                case 0x07:
                case 0x0b:
                case 0x0f:
                    result = m6844_channel[offset / 4].counter & 0xff;
                    break;

                /* channel control */
                case 0x10:
                case 0x11:
                case 0x12:
                case 0x13:
                    result = m6844_channel[offset - 0x10].u8_control & 0xFF;

                    /* a read here clears the DMA end flag */
                    m6844_channel[offset - 0x10].u8_control = (m6844_channel[offset - 0x10].u8_control & ~0x80) & 0xFF;
                    break;

                /* priority control */
                case 0x14:
                    result = m6844_priority;
                    break;

                /* interrupt control */
                case 0x15:

                    /* update the global DMA end flag */
                    m6844_interrupt &= ~0x80;
                    m6844_interrupt |= (m6844_channel[0].u8_control & 0x80)
                            | (m6844_channel[1].u8_control & 0x80)
                            | (m6844_channel[2].u8_control & 0x80)
                            | (m6844_channel[3].u8_control & 0x80);

                    result = m6844_interrupt;
                    break;

                /* chaining control */
                case 0x16:
                    result = m6844_chain;
                    break;
            }

            return result;
        }
    };

    public static WriteHandlerPtr exidy440_m6844_w = new WriteHandlerPtr() {
        public void handler(int offset, int data) {
            int i;

            /* first update the current state of the DMA transfers */
            exidy440_m6844_update();

            /* switch off the offset we were given */
            switch (offset) {
                /* upper byte of address */
                case 0x00:
                case 0x04:
                case 0x08:
                case 0x0c:
                    m6844_channel[offset / 4].address = (m6844_channel[offset / 4].address & 0xff) | (data << 8);
                    break;

                /* lower byte of address */
                case 0x01:
                case 0x05:
                case 0x09:
                case 0x0d:
                    m6844_channel[offset / 4].address = (m6844_channel[offset / 4].address & 0xff00) | (data & 0xff);
                    break;

                /* upper byte of counter */
                case 0x02:
                case 0x06:
                case 0x0a:
                case 0x0e:
                    m6844_channel[offset / 4].counter = (m6844_channel[offset / 4].counter & 0xff) | (data << 8);
                    break;

                /* lower byte of counter */
                case 0x03:
                case 0x07:
                case 0x0b:
                case 0x0f:
                    m6844_channel[offset / 4].counter = (m6844_channel[offset / 4].counter & 0xff00) | (data & 0xff);
                    break;

                /* channel control */
                case 0x10:
                case 0x11:
                case 0x12:
                case 0x13:
                    m6844_channel[offset - 0x10].u8_control = ((m6844_channel[offset - 0x10].u8_control & 0xc0) | (data & 0x3f)) & 0xFF;
                    break;

                /* priority control */
                case 0x14:
                    m6844_priority = data;

                    /* update the sound playback on each channel */
                    for (i = 0; i < 4; i++) {
                        /* if we're going active... */
                        if (m6844_channel[i].active == 0 && (data & (1 << i)) != 0) {
                            /* mark us active */
                            m6844_channel[i].active = 1;

                            /* set the DMA busy bit and clear the DMA end bit */
                            m6844_channel[i].u8_control = (m6844_channel[i].u8_control | 0x40) & 0xFF;
                            m6844_channel[i].u8_control = (m6844_channel[i].u8_control & ~0x80) & 0xFF;

                            /* set the starting address, counter, and time */
                            m6844_channel[i].start_address = m6844_channel[i].address;
                            m6844_channel[i].start_counter = m6844_channel[i].counter;

                            /* generate and play the sample */
                            play_cvsd(i);
                        } /* if we're going inactive... */ else if (m6844_channel[i].active != 0 && (data & (1 << i)) == 0) {
                            /* mark us inactive */
                            m6844_channel[i].active = 0;

                            /* stop playing the sample */
                            stop_cvsd(i);
                        }
                    }
                    break;

                /* interrupt control */
                case 0x15:
                    m6844_interrupt = (m6844_interrupt & 0x80) | (data & 0x7f);
                    break;

                /* chaining control */
                case 0x16:
                    m6844_chain = data;
                    break;
            }
        }
    };

    /*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Sound cache management
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	void reset_sound_cache(void)
/*TODO*///	{
/*TODO*///		sound_cache_end = sound_cache;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	INT16 *add_to_sound_cache(UINT8 *input, int address, int length, int bits, int frequency)
/*TODO*///	{
/*TODO*///		sound_cache_entry *current = sound_cache_end;
/*TODO*///	
/*TODO*///		/* compute where the end will be once we add this entry */
/*TODO*///		sound_cache_end = (sound_cache_entry *)((UINT8 *)current + sizeof(sound_cache_entry) + length * 16);
/*TODO*///	
/*TODO*///		/* if this will overflow the cache, reset and re-add */
/*TODO*///		if (sound_cache_end > sound_cache_max)
/*TODO*///		{
/*TODO*///			reset_sound_cache();
/*TODO*///			return add_to_sound_cache(input, address, length, bits, frequency);
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* fill in this entry */
/*TODO*///		current->next = sound_cache_end;
/*TODO*///		current->address = address;
/*TODO*///		current->length = length;
/*TODO*///		current->bits = bits;
/*TODO*///		current->frequency = frequency;
/*TODO*///	
/*TODO*///		/* decode the data into the cache */
/*TODO*///		decode_and_filter_cvsd(input, length, bits, frequency, current->data);
/*TODO*///		return current->data;
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	INT16 *find_or_add_to_sound_cache(int address, int length, int bits, int frequency)
/*TODO*///	{
/*TODO*///		sound_cache_entry *current;
/*TODO*///	
/*TODO*///		for (current = sound_cache; current < sound_cache_end; current = current->next)
/*TODO*///			if (current->address == address && current->length == length && current->bits == bits && current->frequency == frequency)
/*TODO*///				return current->data;
/*TODO*///	
/*TODO*///		return add_to_sound_cache(&memory_region(REGION_SOUND1)[address], address, length, bits, frequency);
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	Internal CVSD decoder and player
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
    public static void play_cvsd(int ch) {
        /*TODO*///		sound_channel_data *channel = &sound_channel[ch];
/*TODO*///		int address = m6844_channel[ch].address;
/*TODO*///		int length = m6844_channel[ch].counter;
/*TODO*///		INT16 *base;
/*TODO*///	
/*TODO*///		/* add the bank number to the address */
/*TODO*///		if (exidy440_sound_banks[ch] & 1)
/*TODO*///			address += 0x00000;
/*TODO*///		else if (exidy440_sound_banks[ch] & 2)
/*TODO*///			address += 0x08000;
/*TODO*///		else if (exidy440_sound_banks[ch] & 4)
/*TODO*///			address += 0x10000;
/*TODO*///		else if (exidy440_sound_banks[ch] & 8)
/*TODO*///			address += 0x18000;
/*TODO*///	
/*TODO*///		/* compute the base address in the converted samples array */
/*TODO*///		base = find_or_add_to_sound_cache(address, length, channel_bits[ch], channel_frequency[ch]);
/*TODO*///		if (base == 0)
/*TODO*///			return;
/*TODO*///	
/*TODO*///		/* if the length is 0 or 1, just do an immediate end */
/*TODO*///		if (length <= 3)
/*TODO*///		{
/*TODO*///			channel->base = base;
/*TODO*///			channel->offset = length;
/*TODO*///			channel->remaining = 0;
/*TODO*///			m6844_finished(ch);
/*TODO*///			return;
/*TODO*///		}
/*TODO*///	
/*TODO*///		/* set the pointer and count */
/*TODO*///		channel->base = base;
/*TODO*///		channel->offset = 0;
/*TODO*///		channel->remaining = length * 8;
/*TODO*///	
/*TODO*///		/* channels 2 and 3 play twice as slow, so we need to count twice as many samples */
/*TODO*///		if (ch & 2) channel->remaining *= 2;
    }

    public static void stop_cvsd(int ch) {
        /* the DMA channel is marked inactive; that will kill the audio */
        sound_channel[ch].remaining = 0;
        stream_update(sound_stream, 0);
    }
    	
	
	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	FIR digital filter
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	void fir_filter(INT32 *input, INT16 *output, int count)
/*TODO*///	{
/*TODO*///		while (count--)
/*TODO*///		{
/*TODO*///			INT32 result = (input[-1] - input[-8] - input[-48] + input[-55]) << 2;
/*TODO*///			result += (input[0] + input[-18] + input[-38] + input[-56]) << 3;
/*TODO*///			result += (-input[-2] - input[-4] + input[-5] + input[-51] - input[-52] - input[-54]) << 4;
/*TODO*///			result += (-input[-3] - input[-11] - input[-45] - input[-53]) << 5;
/*TODO*///			result += (input[-6] + input[-7] - input[-9] - input[-15] - input[-41] - input[-47] + input[-49] + input[-50]) << 6;
/*TODO*///			result += (-input[-10] + input[-12] + input[-13] + input[-14] + input[-21] + input[-35] + input[-42] + input[-43] + input[-44] - input[-46]) << 7;
/*TODO*///			result += (-input[-16] - input[-17] + input[-19] + input[-37] - input[-39] - input[-40]) << 8;
/*TODO*///			result += (input[-20] - input[-22] - input[-24] + input[-25] + input[-31] - input[-32] - input[-34] + input[-36]) << 9;
/*TODO*///			result += (-input[-23] - input[-33]) << 10;
/*TODO*///			result += (input[-26] + input[-30]) << 11;
/*TODO*///			result += (input[-27] + input[-28] + input[-29]) << 12;
/*TODO*///			result >>= 14;
/*TODO*///	
/*TODO*///			if (result < -32768)
/*TODO*///				result = -32768;
/*TODO*///			else if (result > 32767)
/*TODO*///				result = 32767;
/*TODO*///	
/*TODO*///			*output++ = result;
/*TODO*///			input++;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
/*TODO*///	/*************************************
/*TODO*///	 *
/*TODO*///	 *	CVSD decoder
/*TODO*///	 *
/*TODO*///	 *************************************/
/*TODO*///	
/*TODO*///	void decode_and_filter_cvsd(UINT8 *input, int bytes, int maskbits, int frequency, INT16 *output)
/*TODO*///	{
/*TODO*///		INT32 buffer[SAMPLE_BUFFER_LENGTH + FIR_HISTORY_LENGTH];
/*TODO*///		int total_samples = bytes * 8;
/*TODO*///		int mask = (1 << maskbits) - 1;
/*TODO*///		double filter, integrator, leak;
/*TODO*///		double charge, decay, gain;
/*TODO*///		int steps;
/*TODO*///		int chunk_start;
/*TODO*///	
/*TODO*///	
/*TODO*///		/* compute the charge, decay, and leak constants */
/*TODO*///		charge = pow(exp(-1), 1.0 / (FILTER_CHARGE_TC * (double)frequency));
/*TODO*///		decay = pow(exp(-1), 1.0 / (FILTER_DECAY_TC * (double)frequency));
/*TODO*///		leak = pow(exp(-1), 1.0 / (INTEGRATOR_LEAK_TC * (double)frequency));
/*TODO*///	
/*TODO*///		/* compute the gain */
/*TODO*///		gain = SAMPLE_GAIN;
/*TODO*///	
/*TODO*///		/* clear the history words for a start */
/*TODO*///		memset(&buffer[0], 0, FIR_HISTORY_LENGTH * sizeof(INT32));
/*TODO*///	
/*TODO*///		/* initialize the CVSD decoder */
/*TODO*///		steps = 0xaa;
/*TODO*///		filter = FILTER_MIN;
/*TODO*///		integrator = 0.0;
/*TODO*///	
/*TODO*///		/* loop over chunks */
/*TODO*///		for (chunk_start = 0; chunk_start < total_samples; chunk_start += SAMPLE_BUFFER_LENGTH)
/*TODO*///		{
/*TODO*///			INT32 *bufptr = &buffer[FIR_HISTORY_LENGTH];
/*TODO*///			int chunk_bytes;
/*TODO*///			int ind;
/*TODO*///	
/*TODO*///			/* how many samples do we generate in this chunk? */
/*TODO*///			if (chunk_start + SAMPLE_BUFFER_LENGTH > total_samples)
/*TODO*///				chunk_bytes = (total_samples - chunk_start) / 8;
/*TODO*///			else
/*TODO*///				chunk_bytes = SAMPLE_BUFFER_LENGTH / 8;
/*TODO*///	
/*TODO*///			/* loop over samples */
/*TODO*///			for (ind = 0; ind < chunk_bytes; ind++)
/*TODO*///			{
/*TODO*///				double temp;
/*TODO*///				int databyte = *input++;
/*TODO*///				int bit;
/*TODO*///				int sample;
/*TODO*///	
/*TODO*///				/* loop over bits in the byte, low to high */
/*TODO*///				for (bit = 0; bit < 8; bit++)
/*TODO*///				{
/*TODO*///					/* move the estimator up or down a step based on the bit */
/*TODO*///					if (databyte & (1 << bit))
/*TODO*///					{
/*TODO*///						integrator += filter;
/*TODO*///						steps = (steps << 1) | 1;
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						integrator -= filter;
/*TODO*///						steps <<= 1;
/*TODO*///					}
/*TODO*///	
/*TODO*///					/* keep track of the last n bits */
/*TODO*///					steps &= mask;
/*TODO*///	
/*TODO*///					/* simulate leakage */
/*TODO*///					integrator *= leak;
/*TODO*///	
/*TODO*///					/* if we got all 0's or all 1's in the last n bits, bump the step up */
/*TODO*///					if (steps == 0 || steps == mask)
/*TODO*///					{
/*TODO*///						filter = FILTER_MAX - ((FILTER_MAX - filter) * charge);
/*TODO*///						if (filter > FILTER_MAX)
/*TODO*///							filter = FILTER_MAX;
/*TODO*///					}
/*TODO*///	
/*TODO*///					/* simulate decay */
/*TODO*///					else
/*TODO*///					{
/*TODO*///						filter *= decay;
/*TODO*///						if (filter < FILTER_MIN)
/*TODO*///							filter = FILTER_MIN;
/*TODO*///					}
/*TODO*///	
/*TODO*///					/* compute the sample as a 32-bit word */
/*TODO*///					temp = integrator * gain;
/*TODO*///	
/*TODO*///					/* compress the sample range to fit better in a 16-bit word */
/*TODO*///					if (temp < 0)
/*TODO*///						sample = (int)(temp / (-temp * (1.0 / 32768.0) + 1.0));
/*TODO*///					else
/*TODO*///						sample = (int)(temp / (temp * (1.0 / 32768.0) + 1.0));
/*TODO*///	
/*TODO*///					/* store the result to our temporary buffer */
/*TODO*///					*bufptr++ = sample;
/*TODO*///				}
/*TODO*///			}
/*TODO*///	
/*TODO*///	
/*TODO*///			/* all done with this chunk, run the filter on it */
/*TODO*///			fir_filter(&buffer[FIR_HISTORY_LENGTH], &output[chunk_start], chunk_bytes * 8);
/*TODO*///	
/*TODO*///			/* copy the last few input samples down to the start for a new history */
/*TODO*///			memcpy(&buffer[0], &buffer[SAMPLE_BUFFER_LENGTH], FIR_HISTORY_LENGTH * sizeof(INT32));
/*TODO*///		}
/*TODO*///	
/*TODO*///	
/*TODO*///		/* make sure the volume goes smoothly to 0 over the last 512 samples */
/*TODO*///		if (FADE_TO_ZERO)
/*TODO*///		{
/*TODO*///			INT16 *data;
/*TODO*///	
/*TODO*///			chunk_start = (total_samples > 512) ? total_samples - 512 : 0;
/*TODO*///			data = output + chunk_start;
/*TODO*///			for ( ; chunk_start < total_samples; chunk_start++)
/*TODO*///			{
/*TODO*///				*data = (*data * ((total_samples - chunk_start) >> 9));
/*TODO*///				data++;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///	
/*TODO*///	
/*TODO*///	
}
