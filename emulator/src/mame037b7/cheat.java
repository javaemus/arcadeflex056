/*
 * ported to v0.37b7
 * 
 */
package mame037b7;

import static common.libc.cstdio.sprintf;
import arcadeflex037b7.util.cheatFileParser;
import java.util.ArrayList;

import static mame056.commonH.*;
import static mame056.ui_text.*;
import static mame056.ui_textH.*;
import static mame056.inptportH.*;
import static mame056.mame.*;
import static mame056.cpuintrf.cpunum_address_mask;
import static mame056.cpuintrf.cpunum_read_byte;
import static mame056.cpuintrf.cpunum_write_byte;
import static mame056.cpuintrfH.cpu_gettotalcpu;
import static mame056.input.*;
import static mame056.inputH.*;
import static mame056.usrintrf.*;

public class cheat {

    /*TODO*///#include "driver.h"
/*TODO*///#include "ui_text.h"
/*TODO*///
/*TODO*///#ifndef MESS
/*TODO*///#ifndef TINY_COMPILE
/*TODO*///extern struct GameDriver driver_neogeo;
/*TODO*///#endif
/*TODO*///#endif
/*TODO*///
/*TODO*///extern unsigned char *memory_find_base (int cpu, int offset);
/*TODO*///
/*TODO*////******************************************
/*TODO*/// *
/*TODO*/// * Cheats
/*TODO*/// *
/*TODO*/// */
/*TODO*///
    public static int MAX_LOADEDCHEATS = 200;

    /*TODO*///#define CHEAT_FILENAME_MAXLEN	255
    public static int SUBCHEAT_FLAG_DONE = 0x0001;
    public static int SUBCHEAT_FLAG_TIMED = 0x0002;

    public static class subcheat_struct {

        int cpu;
        int address;
        int data;
        int backup;
        /* The original value of the memory location, checked against the current */
        int/*UINT32*/ code;
        int/*UINT16*/ flags;
        int min;
        int max;
        int/*UINT32*/ frames_til_trigger;
        /* the number of frames until this cheat fires (does not change) */
        int/*UINT32*/ frame_count;
        /* decrementing frame counter to determine if cheat should fire */
    }

    public static final int CHEAT_FLAG_ACTIVE = 0x01;
    public static final int CHEAT_FLAG_WATCH = 0x02;
    public static final int CHEAT_FLAG_COMMENT = 0x04;

    public static class cheat_struct {

        String name;
        String comment;
        int flags;
        /* bit 0 = active, 1 = watchpoint, 2 = comment */
        int num_sub;
        /* number of cheat cpu/address/data/code combos for this one cheat */
        subcheat_struct[] subcheat = new subcheat_struct[MAX_LOADEDCHEATS + 1];
        /* a variable-number of subcheats are attached to each "master" cheat */
    }
    /*TODO*///
/*TODO*///struct memory_struct
/*TODO*///{
/*TODO*///	int Enabled;
/*TODO*///	char name[40];
/*TODO*///	mem_write_handler handler;
/*TODO*///};

    public static final int kCheatSpecial_Poke = 0;
    public static final int kCheatSpecial_Poke1 = 2;
    public static final int kCheatSpecial_Poke2 = 3;
    public static final int kCheatSpecial_Poke5 = 4;
    public static final int kCheatSpecial_Delay1 = 5;
    public static final int kCheatSpecial_Delay2 = 6;
    public static final int kCheatSpecial_Delay5 = 7;
    public static final int kCheatSpecial_Backup1 = 8;
    public static final int kCheatSpecial_Backup4 = 11;
    public static final int kCheatSpecial_SetBit1 = 22;
    public static final int kCheatSpecial_SetBit2 = 23;
    public static final int kCheatSpecial_SetBit5 = 24;
    public static final int kCheatSpecial_ResetBit1 = 42;
    public static final int kCheatSpecial_ResetBit2 = 43;
    public static final int kCheatSpecial_ResetBit5 = 44;
    public static final int kCheatSpecial_UserFirst = 60;
    public static final int kCheatSpecial_m0d0c = 60;
    /* minimum value 0, display range 0 to byte, poke when changed */
    public static final int kCheatSpecial_m0d1c = 61;
    /* minimum value 0, display range 1 to byte+1, poke when changed */
    public static final int kCheatSpecial_m1d1c = 62;
    /* minimum value 1, display range 1 to byte, poke when changed */
    public static final int kCheatSpecial_m0d0bcdc = 63;
    /* BCD, minimum value 0, display range 0 to byte, poke when changed */
    public static final int kCheatSpecial_m0d1bcdc = 64;
    /* BCD, minimum value 0, display range 1 to byte+1, poke when changed */
    public static final int kCheatSpecial_m1d1bcdc = 65;
    /* BCD, minimum value 1, display range 1 to byte, poke when changed */
    public static final int kCheatSpecial_m0d0 = 70;
    /* minimum value 0, display range 0 to byte */
    public static final int kCheatSpecial_m0d1 = 71;
    /* minimum value 0, display range 1 to byte+1 */
    public static final int kCheatSpecial_m1d1 = 72;
    /* minimum value 1, display range 1 to byte */
    public static final int kCheatSpecial_m0d0bcd = 73;
    /* BCD, minimum value 0, display range 0 to byte */
    public static final int kCheatSpecial_m0d1bcd = 74;
    /* BCD, minimum value 0, display range 1 to byte+1 */
    public static final int kCheatSpecial_m1d1bcd = 75;
    /* BCD, minimum value 1, display range 1 to byte */
    public static final int kCheatSpecial_UserLast = 75;
    public static final int kCheatSpecial_Last = 99;
    public static final int kCheatSpecial_LinkStart = 500;
    /* only used when loading the database */
    public static final int kCheatSpecial_LinkEnd = 599;
    /* only used when loading the database */
    public static final int kCheatSpecial_Watch = 998;
    public static final int kCheatSpecial_Comment = 999;
    public static final int kCheatSpecial_Timed = 1000;

    /*TODO*///char *cheatfile = "cheat.dat";
/*TODO*///
/*TODO*///char database[CHEAT_FILENAME_MAXLEN+1];
    public static int he_did_cheat;

    /*TODO*///
/*TODO*////******************************************
/*TODO*/// *
/*TODO*/// * Searches
/*TODO*/// *
/*TODO*/// */
/*TODO*///
/*TODO*////* Defines */
/*TODO*///#define MAX_SEARCHES 500
/*TODO*///
/*TODO*///enum {
/*TODO*///	kSearch_None = 0,
/*TODO*///	kSearch_Value =	1,
/*TODO*///	kSearch_Time,
/*TODO*///	kSearch_Energy,
/*TODO*///	kSearch_Bit,
/*TODO*///	kSearch_Byte
/*TODO*///};
/*TODO*///
    public static final int kRestore_NoInit = 1;
    public static final int kRestore_NoSave = 2;
    public static final int kRestore_Done = 3;
    public static final int kRestore_OK = 4;

    /*TODO*///
/*TODO*////* Local variables */
/*TODO*///static int searchType;
/*TODO*///static int searchCPU;
/*TODO*/////static int priorSearchCPU;	/* Steph */
/*TODO*///static int searchValue;
    static int restoreStatus;
    /*TODO*///
/*TODO*///static int fastsearch = 2; /* ?? */
/*TODO*///
/*TODO*///static struct ExtMemory StartRam[MAX_EXT_MEMORY];
/*TODO*///static struct ExtMemory BackupRam[MAX_EXT_MEMORY];
/*TODO*///static struct ExtMemory FlagTable[MAX_EXT_MEMORY];
/*TODO*///
/*TODO*///static struct ExtMemory OldBackupRam[MAX_EXT_MEMORY];
/*TODO*///static struct ExtMemory OldFlagTable[MAX_EXT_MEMORY];
/*TODO*///
/*TODO*////* Local prototypes */
/*TODO*///static void reset_table (struct ExtMemory *table);
/*TODO*///
/*TODO*////******************************************
/*TODO*/// *
/*TODO*/// * Watchpoints
/*TODO*/// *
/*TODO*/// */

    public static final int MAX_WATCHES = 20;

    static class watch_struct {
        /*TODO*///	int cheat_num;		/* if this watchpoint is tied to a cheat, this is the index into the cheat array. -1 if none */
/*TODO*///	UINT32 address;
/*TODO*///	INT16 cpu;
/*TODO*///	UINT8 num_bytes;	/* number of consecutive bytes to display */
/*TODO*///	UINT8 label_type;	/* none, address, text */
/*TODO*///	char label[255];	/* optional text label */
/*TODO*///	UINT16 x, y;		/* position of watchpoint on screen */
    }

    static watch_struct[] watches = new watch_struct[MAX_WATCHES];
    static int is_watch_active;
    /* true if at least one watchpoint is active */
    static int is_watch_visible;
    /* we can toggle the visibility for all on or off */

 /*TODO*////* in hiscore.c */
/*TODO*///int cpunum_read_byte(int cpu, int addr);
/*TODO*///void cpunum_write_byte(int cpu, int addr, int value);
/*TODO*///
/*TODO*////* Some macros to simplify the code */
/*TODO*///#define READ_CHEAT		cpunum_read_byte (subcheat.cpu, subcheat.address)
/*TODO*///#define WRITE_CHEAT		cpunum_write_byte (subcheat.cpu, subcheat.address, subcheat.data)
/*TODO*///#define COMPARE_CHEAT		(cpunum_read_byte (subcheat->cpu, subcheat->address) != subcheat->data)
/*TODO*///#define CPU_AUDIO_OFF(index)	((Machine->drv->cpu[index].cpu_type & CPU_AUDIO_CPU) && (Machine->sample_rate == 0))
/*TODO*///
/*TODO*////* Steph */
/*TODO*///#ifdef MESS
/*TODO*///#define WRITE_OLD_CHEAT		cpunum_write_byte (subcheat->cpu, subcheat->address, subcheat->olddata)
/*TODO*///#endif
/*TODO*///
/*TODO*////* Local prototypes */
/*TODO*///static INT32 DisplayHelpFile (INT32 selected);
/*TODO*///static INT32 EditCheatMenu (struct osd_bitmap *bitmap, INT32 selected, UINT8 cheatnum);
/*TODO*///static INT32 CommentMenu (struct osd_bitmap *bitmap, INT32 selected, int cheat_index);
/*TODO*///static int SkipBank(int CpuToScan, int *BankToScanTable, mem_write_handler handler);	/* Steph */
/*TODO*///
/*TODO*////* Local variables */
/*TODO*////* static int	search_started = 0; */
/*TODO*///
    static int ActiveCheatTotal;
    /* number of cheats currently active */
    static int LoadedCheatTotal;
    /* total number of cheats */
    static cheat_struct[] CheatTable = new cheat_struct[MAX_LOADEDCHEATS + 1];

    static int CheatEnabled;

    /*TODO*///
/*TODO*///
/*TODO*///
/*TODO*///
/*TODO*////* Function to test if a value is BCD (returns 1) or not (returns 0) */
/*TODO*///int IsBCD(int ParamValue)
/*TODO*///{
/*TODO*///	return(((ParamValue % 0x10 <= 9) & (ParamValue <= 0x99)) ? 1 : 0);
/*TODO*///}
/*TODO*///
/*TODO*////* return a format specifier for printf based on cpu address range */
/*TODO*///static char *FormatAddr(int cpu, int addtext)
/*TODO*///{
/*TODO*///	static char bufadr[10];
/*TODO*///	static char buffer[18];
/*TODO*/////	int i;
/*TODO*///
/*TODO*///	memset (buffer, '\0', strlen(buffer));
/*TODO*///	switch (cpunum_address_bits(cpu) >> 2)
/*TODO*///	{
/*TODO*///		case 4:
/*TODO*///			strcpy (bufadr, "%04X");
/*TODO*///			break;
/*TODO*///		case 5:
/*TODO*///			strcpy (bufadr, "%05X");
/*TODO*///			break;
/*TODO*///		case 6:
/*TODO*///			strcpy (bufadr, "%06X");
/*TODO*///			break;
/*TODO*///		case 7:
/*TODO*///			strcpy (bufadr, "%07X");
/*TODO*///			break;
/*TODO*///		case 8:
/*TODO*///			strcpy (bufadr, "%08X");
/*TODO*///			break;
/*TODO*///		default:
/*TODO*///			strcpy (bufadr, "%X");
/*TODO*///			break;
/*TODO*///	}
/*TODO*///	strcat (buffer,bufadr);
/*TODO*///	return buffer;
/*TODO*///}
/*TODO*///
/*TODO*////* Function to rename the cheatfile (returns 1 if the file has been renamed else 0)*/
/*TODO*///int RenameCheatFile(int merge, int DisplayFileName, char *filename)
/*TODO*///{
/*TODO*///	return 0;
/*TODO*///}
/*TODO*///
/*TODO*////* Function who loads the cheats for a game */
/*TODO*///int SaveCheat(int NoCheat)
/*TODO*///{
/*TODO*///	return 0;
/*TODO*///}
    /**
     * *************************************************************************
     *
     * cheat_set_code
     *
     * Given a cheat code, sets the various attribues of the cheat structure.
     * This is to aid in making the cheat engine more flexible in the event that
     * someday the codes are restructured or the case statement in DoCheat is
     * simplified from its current form.
     *
     **************************************************************************
     */
    public static void cheat_set_code(subcheat_struct subcheat, int code, int cheat_num) {
        switch (code) {
            case kCheatSpecial_Poke1:
            case kCheatSpecial_Delay1:
            case kCheatSpecial_SetBit1:
            case kCheatSpecial_ResetBit1:
                subcheat.frames_til_trigger = (int) (1 * Machine.drv.frames_per_second);
                /* was 60 */
                break;
            case kCheatSpecial_Poke2:
            case kCheatSpecial_Delay2:
            case kCheatSpecial_SetBit2:
            case kCheatSpecial_ResetBit2:
                subcheat.frames_til_trigger = (int) (2 * Machine.drv.frames_per_second);
                /* was 60 */
                break;
            case kCheatSpecial_Poke5:
            case kCheatSpecial_Delay5:
            case kCheatSpecial_SetBit5:
            case kCheatSpecial_ResetBit5:
                subcheat.frames_til_trigger = (int) (5 * Machine.drv.frames_per_second);
                /* was 60 */
                break;
            case kCheatSpecial_Comment:
                subcheat.frames_til_trigger = 0;
                subcheat.address = 0;
                subcheat.data = 0;
                CheatTable[cheat_num].flags |= CHEAT_FLAG_COMMENT;
                break;
            case kCheatSpecial_Watch:
                subcheat.frames_til_trigger = 0;
                subcheat.data = 0;
                CheatTable[cheat_num].flags |= CHEAT_FLAG_WATCH;
                break;
            default:
                subcheat.frames_til_trigger = 0;
                break;
        }

        /* Set the minimum value */
        if ((code == kCheatSpecial_m1d1c)
                || (code == kCheatSpecial_m1d1bcdc)
                || (code == kCheatSpecial_m1d1)
                || (code == kCheatSpecial_m1d1bcd)) {
            subcheat.min = 1;
        } else {
            subcheat.min = 0;
        }

        /* Set the maximum value */
        if ((code >= kCheatSpecial_UserFirst)
                && (code <= kCheatSpecial_UserLast)) {
            subcheat.max = subcheat.data;
            subcheat.data = 0;
        } else {
            subcheat.max = 0xff;
        }

        subcheat.code = code;
    }

    /**
     * *************************************************************************
     *
     * cheat_set_status
     *
     * Given an index into the cheat table array, make the selected cheat either
     * active or inactive.
     *
     * TODO: possibly support converting to a watchpoint in here.
     *
     **************************************************************************
     */
    public static void cheat_set_status(int cheat_num, int active) {
        int i;

        if (active != 0) /* enable the cheat */ {
            for (i = 0; i <= CheatTable[cheat_num].num_sub; i++) {
                /* Reset the active variables */
                CheatTable[cheat_num].subcheat[i].frame_count = 0;
                CheatTable[cheat_num].subcheat[i].backup = 0;
            }

            /* only add if there's a cheat active already */
            if ((CheatTable[cheat_num].flags & CHEAT_FLAG_ACTIVE) == 0) {
                CheatTable[cheat_num].flags |= CHEAT_FLAG_ACTIVE;
                ActiveCheatTotal++;
            }

            /* tell the MAME core that we're cheaters! */
            he_did_cheat = 1;
        } else /* disable the cheat (case 0, 2) */ {
            for (i = 0; i <= CheatTable[cheat_num].num_sub; i++) {
//			struct subcheat_struct *subcheat = &CheatTable[cheat_num].subcheat[i];	/* Steph */

                /* Reset the active variables */
                CheatTable[cheat_num].subcheat[i].frame_count = 0;
                CheatTable[cheat_num].subcheat[i].backup = 0;

            }

            /* only add if there's a cheat active already */
            if ((CheatTable[cheat_num].flags & CHEAT_FLAG_ACTIVE) != 0) {
                CheatTable[cheat_num].flags &= ~CHEAT_FLAG_ACTIVE;
                ActiveCheatTotal--;
            }
        }
    }

    /*TODO*///void cheat_insert_new (int cheat_num)
/*TODO*///{
/*TODO*///	/* if list is full, bail */
/*TODO*///	if (LoadedCheatTotal == MAX_LOADEDCHEATS) return;
/*TODO*///
/*TODO*///	/* if the index is off the end of the list, fix it */
/*TODO*///	if (cheat_num > LoadedCheatTotal) cheat_num = LoadedCheatTotal;
/*TODO*///
/*TODO*///	/* clear space in the middle of the table if needed */
/*TODO*///	if (cheat_num < LoadedCheatTotal)
/*TODO*///		memmove (&CheatTable[cheat_num+1], &CheatTable[cheat_num], sizeof (struct cheat_struct) * (LoadedCheatTotal - cheat_num));
/*TODO*///
/*TODO*///	/* clear the new entry */
/*TODO*///	memset (&CheatTable[cheat_num], 0, sizeof (struct cheat_struct));
/*TODO*///
/*TODO*///	CheatTable[cheat_num].name = malloc (strlen (ui_getstring(UI_none)) + 1);
/*TODO*///	strcpy (CheatTable[cheat_num].name, ui_getstring(UI_none));
/*TODO*///
/*TODO*///	CheatTable[cheat_num].subcheat = calloc (1, sizeof (struct subcheat_struct));
/*TODO*///
/*TODO*///	/*add one to the total */
/*TODO*///	LoadedCheatTotal ++;
/*TODO*///}
/*TODO*///
/*TODO*///void cheat_delete (int cheat_num)
/*TODO*///{
/*TODO*///	/* if the index is off the end, make it the last one */
/*TODO*///	if (cheat_num >= LoadedCheatTotal) cheat_num = LoadedCheatTotal - 1;
/*TODO*///
/*TODO*///	/* deallocate storage for the cheat */
/*TODO*///	free (CheatTable[cheat_num].name);
/*TODO*///	free (CheatTable[cheat_num].comment);
/*TODO*///	free (CheatTable[cheat_num].subcheat);
/*TODO*///
/*TODO*///	/* If it's active, decrease the count */
/*TODO*///	if (CheatTable[cheat_num].flags & CHEAT_FLAG_ACTIVE)
/*TODO*///		ActiveCheatTotal --;
/*TODO*///
/*TODO*///	/* move all the elements after this one up one slot if there are more than 1 and it's not the last */
/*TODO*///	if ((LoadedCheatTotal > 1) && (cheat_num < LoadedCheatTotal - 1))
/*TODO*///		memmove (&CheatTable[cheat_num], &CheatTable[cheat_num+1], sizeof (struct cheat_struct) * (LoadedCheatTotal - (cheat_num + 1)));
/*TODO*///
/*TODO*///	/* knock one off the total */
/*TODO*///	LoadedCheatTotal --;
/*TODO*///}
/*TODO*///
/*TODO*///void subcheat_insert_new (int cheat_num, int subcheat_num)
/*TODO*///{
/*TODO*///	/* if the index is off the end of the list, fix it */
/*TODO*///	if (subcheat_num > CheatTable[cheat_num].num_sub) subcheat_num = CheatTable[cheat_num].num_sub + 1;
/*TODO*///
/*TODO*///	/* grow the subcheat table allocation */
/*TODO*///	CheatTable[cheat_num].subcheat = realloc (CheatTable[cheat_num].subcheat, sizeof (struct subcheat_struct) * (CheatTable[cheat_num].num_sub + 2));
/*TODO*///	if (CheatTable[cheat_num].subcheat == NULL) return;
/*TODO*///
/*TODO*///	/* insert space in the middle of the table if needed */
/*TODO*///	if ((subcheat_num < CheatTable[cheat_num].num_sub) || (subcheat_num == 0))
/*TODO*///		memmove (&CheatTable[cheat_num].subcheat[subcheat_num+1], &CheatTable[cheat_num].subcheat[subcheat_num],
/*TODO*///			sizeof (struct subcheat_struct) * (CheatTable[cheat_num].num_sub + 1 - subcheat_num));
/*TODO*///
/*TODO*///	/* clear the new entry */
/*TODO*///	memset (&CheatTable[cheat_num].subcheat[subcheat_num], 0, sizeof (struct subcheat_struct));
/*TODO*///
/*TODO*///	/*add one to the total */
/*TODO*///	CheatTable[cheat_num].num_sub ++;
/*TODO*///}
/*TODO*///
/*TODO*///void subcheat_delete (int cheat_num, int subcheat_num)
/*TODO*///{
/*TODO*///	if (CheatTable[cheat_num].num_sub < 1) return;
/*TODO*///	/* if the index is off the end, make it the last one */
/*TODO*///	if (subcheat_num > CheatTable[cheat_num].num_sub) subcheat_num = CheatTable[cheat_num].num_sub;
/*TODO*///
/*TODO*///	/* remove the element in the middle if it's not the last */
/*TODO*///	if (subcheat_num < CheatTable[cheat_num].num_sub)
/*TODO*///		memmove (&CheatTable[cheat_num].subcheat[subcheat_num], &CheatTable[cheat_num].subcheat[subcheat_num+1],
/*TODO*///			sizeof (struct subcheat_struct) * (CheatTable[cheat_num].num_sub - subcheat_num));
/*TODO*///
/*TODO*///	/* shrink the subcheat table allocation */
/*TODO*///	CheatTable[cheat_num].subcheat = realloc (CheatTable[cheat_num].subcheat, sizeof (struct subcheat_struct) * (CheatTable[cheat_num].num_sub));
/*TODO*///	if (CheatTable[cheat_num].subcheat == NULL) return;
/*TODO*///
/*TODO*///	/* knock one off the total */
/*TODO*///	CheatTable[cheat_num].num_sub --;
/*TODO*///}
/*TODO*///
/* Function to load the cheats for a game from a single database */
    public static void LoadCheatFile(int merge, String filename) {
        subcheat_struct subcheat;
        int sub = 0;

        if (merge == 0) {
            ActiveCheatTotal = 0;
            LoadedCheatTotal = 0;
        }
        if (cheatFileParser.loadCheatFile(filename) == 0) {
            return;
        }
        ArrayList<String> lines = cheatFileParser.read(Machine.gamedrv.name);
        if (!lines.isEmpty()) {
            for (String line : lines) {
                String[] curline = line.split(":");
                int temp_cpu;
                int temp_address;
                int temp_data;
                int temp_code;
                if (!curline[0].matches(Machine.gamedrv.name)) {
                    //should already be checked but just in case
                    System.out.println("cheat line doesn't match gamename!");
                    continue;
                }
                /* CPU number */
                temp_cpu = Integer.parseInt(curline[1]);
//		/* skip if it's a sound cpu and the audio is off */
//		if (CPU_AUDIO_OFF(temp_cpu)) continue;
                /* skip if this is a bogus CPU */
                if (temp_cpu >= cpu_gettotalcpu()) {
                    continue;
                }

                /* Address */
                temp_address = Integer.parseInt(curline[2], 16);//sscanf(ptr,"%X", &temp_address);
                temp_address &= cpunum_address_mask(temp_cpu);

                /* data byte */
                temp_data = Integer.parseInt(curline[3], 16);//sscanf(ptr,"%x", &temp_data);
                temp_data &= 0xff;

                /* special code */
                temp_code = Integer.parseInt(curline[4]);//sscanf(ptr,"%d", &temp_code);

                /* Is this a subcheat? */
                if ((temp_code >= kCheatSpecial_LinkStart)
                        && (temp_code <= kCheatSpecial_LinkEnd)) {
                    sub++;

                    /* Adjust the special flag */
                    temp_code -= kCheatSpecial_LinkStart;

                    /* point to the last valid main cheat entry */
                    LoadedCheatTotal--;
                } else {
                    /* no, make this the first cheat in the series */
                    sub = 0;
                }

                /* Store the current number of subcheats embodied by this code */
                CheatTable[LoadedCheatTotal].num_sub = sub;


                /* Reset the cheat */
                CheatTable[LoadedCheatTotal].subcheat[sub] = new subcheat_struct();
                CheatTable[LoadedCheatTotal].subcheat[sub].frames_til_trigger = 0;
                CheatTable[LoadedCheatTotal].subcheat[sub].frame_count = 0;
                CheatTable[LoadedCheatTotal].subcheat[sub].backup = 0;
                CheatTable[LoadedCheatTotal].subcheat[sub].flags = 0;

                /* Copy the cheat data */
                CheatTable[LoadedCheatTotal].subcheat[sub].cpu = temp_cpu;

                CheatTable[LoadedCheatTotal].subcheat[sub].address = temp_address;
                CheatTable[LoadedCheatTotal].subcheat[sub].data = temp_data;
                CheatTable[LoadedCheatTotal].subcheat[sub].code = temp_code;

                cheat_set_code(CheatTable[LoadedCheatTotal].subcheat[sub], temp_code, LoadedCheatTotal);

                /* don't bother with the names & comments for subcheats */
                if (sub != 0) {
                    //goto next;
                    LoadedCheatTotal++;
                    continue;
                }

                /* Disable the cheat */
                CheatTable[LoadedCheatTotal].flags &= ~CHEAT_FLAG_ACTIVE;

                /* cheat name */
                CheatTable[LoadedCheatTotal].name = curline[5].trim();
                /* read the "comment" field if there */
                try {
                    CheatTable[LoadedCheatTotal].comment = curline[6].trim();

                } catch (ArrayIndexOutOfBoundsException e) {
                    CheatTable[LoadedCheatTotal].comment = "";
                }

                LoadedCheatTotal++;
            }//end of foreach line
        }//end of lines !=empty
    }

    /* Function who loads the cheats for a game from many databases */
    public static void LoadCheatFiles() {
        /*TODO*///	char *ptr;
/*TODO*///	char str[CHEAT_FILENAME_MAXLEN+1];
/*TODO*///	char filename[CHEAT_FILENAME_MAXLEN+1];
/*TODO*///
/*TODO*///	int pos1, pos2;
/*TODO*///
        ActiveCheatTotal = 0;
        LoadedCheatTotal = 0;
        /*TODO*///
/*TODO*///	/* start off with the default cheat file, cheat.dat */
/*TODO*///	strcpy (str, cheatfile);
/*TODO*///	ptr = strtok (str, ";");
/*TODO*///
/*TODO*///	/* append any additional cheat files */
/*TODO*///	strcpy (database, ptr);
/*TODO*///	strcpy (str, cheatfile);
/*TODO*///	str[strlen (str) + 1] = 0;
/*TODO*///	pos1 = 0;
/*TODO*///	while (str[pos1])
/*TODO*///	{
/*TODO*///		pos2 = pos1;
/*TODO*///		while ((str[pos2]) && (str[pos2] != ';'))
/*TODO*///			pos2++;
/*TODO*///		if (pos1 != pos2)
/*TODO*///		{
/*TODO*///			memset (filename, '\0', sizeof(filename));
/*TODO*///			strncpy (filename, &str[pos1], (pos2 - pos1));
/*TODO*///			LoadCheatFile (1, filename);
/*TODO*///			pos1 = pos2 + 1;
/*TODO*///		}
/*TODO*///	}
        //for now load only the main cheat file cheat.dat
        LoadCheatFile(1, "cheat.dat");
    }

    /*TODO*///

    /* Init some variables */
    public static void InitCheat() {
        int i;

        he_did_cheat = 0;
        CheatEnabled = 1;
        /*TODO*///
/*TODO*///	/* set up the search tables */
/*TODO*///	reset_table (StartRam);
/*TODO*///	reset_table (BackupRam);
/*TODO*///	reset_table (FlagTable);
/*TODO*///	reset_table (OldBackupRam);
/*TODO*///	reset_table (OldFlagTable);
/*TODO*///
        restoreStatus = kRestore_NoInit;

        /* Reset the watchpoints to their defaults */
        is_watch_active = 0;
        is_watch_visible = 1;

        for (i = 0; i < MAX_WATCHES; i++) {
            watches[i] = new watch_struct();
            /*TODO*///		/* disable this watchpoint */
/*TODO*///		watches[i].num_bytes = 0;
/*TODO*///
/*TODO*///		watches[i].cpu = 0;
/*TODO*///		watches[i].label[0] = 0x00;
/*TODO*///		watches[i].label_type = 0;
/*TODO*///		watches[i].address = 0;
/*TODO*///
/*TODO*///		/* set the screen position */
/*TODO*///		watches[i].x = 0;
/*TODO*///		watches[i].y = i * Machine->uifontheight;
        }
        //intiallaze cheat table
        for (i = 0; i < MAX_LOADEDCHEATS + 1; i++) {
            CheatTable[i] = new cheat_struct();
        }
        LoadCheatFiles();
    }
    static int ed_submenu_choice;
    static int[] ed_tag = new int[MAX_LOADEDCHEATS];

    static int EnableDisableCheatMenu(mame_bitmap bitmap, int selected) {
        int sel;
        String[] menu_item = new String[MAX_LOADEDCHEATS + 2];
        String[] menu_subitem = new String[MAX_LOADEDCHEATS];
        String[] buf = new String[MAX_LOADEDCHEATS];
        String[] buf2 = new String[MAX_LOADEDCHEATS];

        int i, total = 0;

        sel = selected - 1;

        /* If a submenu has been selected, go there */
        if (ed_submenu_choice != 0) {
            ed_submenu_choice = CommentMenu(bitmap, ed_submenu_choice, ed_tag[sel]);
            if (ed_submenu_choice == -1) {
                ed_submenu_choice = 0;
                sel = -2;
            }

            return sel + 1;
        }

        /* No submenu active, do the watchpoint menu */
        for (i = 0; i < LoadedCheatTotal; i++) {
            int string_num;

            if (!CheatTable[i].comment.isEmpty() && (CheatTable[i].comment.charAt(0) != 0x00)) {
                buf[total] = sprintf("%s (%s...)", CheatTable[i].name, ui_getstring(UI_moreinfo));
            } else {
                buf[total] = sprintf("%s", CheatTable[i].name);
            }

            ed_tag[total] = i;
            menu_item[total] = buf[total];

            /* add submenu options for all cheats that are not comments */
            if ((CheatTable[i].flags & CHEAT_FLAG_COMMENT) == 0) {
                if ((CheatTable[i].flags & CHEAT_FLAG_ACTIVE) != 0) {
                    string_num = UI_on;
                } else {
                    string_num = UI_off;
                }
                buf2[total] = sprintf("%s", ui_getstring(string_num));
                menu_subitem[total] = buf2[total];
            } else {
                menu_subitem[total] = null;
            }
            total++;
        }

        menu_item[total] = ui_getstring(UI_returntoprior);
        menu_subitem[total++] = null;
        menu_item[total] = null;
        /* terminate array */

        ui_displaymenu(bitmap, menu_item, menu_subitem, null, sel, 0);

        if (input_ui_pressed_repeat(IPT_UI_DOWN, 8) != 0) {
            sel = (sel + 1) % total;
        }

        if (input_ui_pressed_repeat(IPT_UI_UP, 8) != 0) {
            sel = (sel + total - 1) % total;
        }

        if ((input_ui_pressed_repeat(IPT_UI_LEFT, 8) != 0) || (input_ui_pressed_repeat(IPT_UI_RIGHT, 8) != 0)) {
            if ((CheatTable[ed_tag[sel]].flags & CHEAT_FLAG_COMMENT) == 0) {
                int active = CheatTable[ed_tag[sel]].flags & CHEAT_FLAG_ACTIVE;

                active ^= 0x01;

                cheat_set_status(ed_tag[sel], active);
                CheatEnabled = 1;
            }
        }
        if (input_ui_pressed(IPT_UI_SELECT) != 0) {
            if (sel == (total - 1)) {
                /* return to prior menu */
                ed_submenu_choice = 0;
                sel = -1;
            } else {
                if (!CheatTable[ed_tag[sel]].comment.isEmpty() && (CheatTable[ed_tag[sel]].comment.charAt(0) != 0x00)) {
                    ed_submenu_choice = 1;
                    /* tell updatescreen() to clean after us */
                   schedule_full_refresh();
                }
            }
        }

        /* Cancel pops us up a menu level */
        if (input_ui_pressed(IPT_UI_CANCEL) != 0) {
            sel = -1;
        }

        /* The UI key takes us all the way back out */
        if (input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
            sel = -2;
        }

        if (sel == -1 || sel == -2) {
            /* tell updatescreen() to clean after us */
            schedule_full_refresh();
        }

        return sel + 1;
    }

    static int CommentMenu(mame_bitmap bitmap, int selected, int cheat_index) {
        String buf;
        String buf2;

        int sel;

        sel = selected - 1;

        buf = "";

        if (CheatTable[cheat_index].comment.charAt(0) == 0x00) {
            sel = -1;
            buf = "";
        } else {
            buf2 = sprintf("\t%s\n\t%s\n\n", ui_getstring(UI_moreinfoheader), CheatTable[cheat_index].name);
            buf = buf2;//strcpy (buf, buf2);
            buf += CheatTable[cheat_index].comment;
        }

        /* menu system, use the normal menu keys */
        buf += "\n\n\t";
        buf += ui_getstring(UI_lefthilight);
        buf += " ";
        buf += ui_getstring(UI_returntoprior);
        buf += " ";
        buf += ui_getstring(UI_righthilight);

        ui_displaymessagewindow(bitmap, buf);

        if (input_ui_pressed(IPT_UI_SELECT) != 0) {
            sel = -1;
        }

        if (input_ui_pressed(IPT_UI_CANCEL) != 0) {
            sel = -1;
        }

        if (input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
            sel = -2;
        }

        if (sel == -1 || sel == -2) {
            /* tell updatescreen() to clean after us */
            schedule_full_refresh();
        }

        return sel + 1;
    }

    /*TODO*///
/*TODO*///INT32 AddEditCheatMenu (struct osd_bitmap *bitmap, INT32 selected)
/*TODO*///{
/*TODO*///	int sel;
/*TODO*///	static INT8 submenu_choice;
/*TODO*///	const char *menu_item[MAX_LOADEDCHEATS + 4];
/*TODO*/////	char buf[MAX_LOADEDCHEATS][80];
/*TODO*/////	char buf2[MAX_LOADEDCHEATS][10];
/*TODO*///	int tag[MAX_LOADEDCHEATS];
/*TODO*///	int i, total = 0;
/*TODO*///
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///	/* Set up the "tag" table so we know which cheats belong in the menu */
/*TODO*///	for (i = 0; i < LoadedCheatTotal; i ++)
/*TODO*///	{
/*TODO*///		/* add menu listings for all cheats that are not comments */
/*TODO*///		if (((CheatTable[i].flags & CHEAT_FLAG_COMMENT) == 0)
/*TODO*///#ifdef MESS
/*TODO*///		/* only data patches can be edited within the cheat engine */
/*TODO*///		&& (CheatTable[i].patch == 'D')
/*TODO*///#endif
/*TODO*///		)
/*TODO*///		{
/*TODO*///			tag[total] = i;
/*TODO*///			menu_item[total++] = CheatTable[i].name;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* If a submenu has been selected, go there */
/*TODO*///	if (submenu_choice)
/*TODO*///	{
/*TODO*///		submenu_choice = EditCheatMenu (bitmap, submenu_choice, tag[sel]);
/*TODO*///		if (submenu_choice == -1)
/*TODO*///		{
/*TODO*///			submenu_choice = 0;
/*TODO*///			sel = -2;
/*TODO*///		}
/*TODO*///
/*TODO*///		return sel + 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* No submenu active, do the watchpoint menu */
/*TODO*///	menu_item[total++] = ui_getstring (UI_returntoprior);
/*TODO*///	menu_item[total] = NULL; /* TODO: add help string */
/*TODO*///	menu_item[total+1] = 0;	/* terminate array */
/*TODO*///
/*TODO*///	ui_displaymenu(bitmap,menu_item,0,0,sel,0);
/*TODO*///
/*TODO*///	if (code_pressed_memory_repeat (KEYCODE_INSERT, 8))
/*TODO*///	{
/*TODO*///		/* add a new cheat at the current position (or the end) */
/*TODO*///		if (sel < total - 1)
/*TODO*///			cheat_insert_new (tag[sel]);
/*TODO*///		else
/*TODO*///			cheat_insert_new (LoadedCheatTotal);
/*TODO*///	}
/*TODO*///
/*TODO*///	if (code_pressed_memory_repeat (KEYCODE_DEL, 8))
/*TODO*///	{
/*TODO*///		if (LoadedCheatTotal)
/*TODO*///		{
/*TODO*///			/* delete the selected cheat (or the last one) */
/*TODO*///			if (sel < total - 1)
/*TODO*///				cheat_delete (tag[sel]);
/*TODO*///			else
/*TODO*///			{
/*TODO*///				cheat_delete (LoadedCheatTotal - 1);
/*TODO*///				sel = total - 2;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///		sel = (sel + 1) % total;
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///		sel = (sel + total - 1) % total;
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///	{
/*TODO*///		if (sel == (total - 1))
/*TODO*///		{
/*TODO*///			/* return to prior menu */
/*TODO*///			submenu_choice = 0;
/*TODO*///			sel = -1;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			submenu_choice = 1;
/*TODO*///			/* tell updatescreen() to clean after us */
/*TODO*///			need_to_clear_bitmap = 1;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* Cancel pops us up a menu level */
/*TODO*///	if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		sel = -1;
/*TODO*///
/*TODO*///	/* The UI key takes us all the way back out */
/*TODO*///	if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///		sel = -2;
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		/* tell updatescreen() to clean after us */
/*TODO*///		need_to_clear_bitmap = 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*///static INT32 EditCheatMenu (struct osd_bitmap *bitmap, INT32 selected, UINT8 cheat_num)
/*TODO*///{
/*TODO*///	int sel;
/*TODO*///	int total, total2;
/*TODO*///	struct subcheat_struct *subcheat;
/*TODO*///	static INT8 submenu_choice;
/*TODO*///	static UINT8 textedit_active;
/*TODO*///	const char *menu_item[40];
/*TODO*///	const char *menu_subitem[40];
/*TODO*///	char setting[40][30];
/*TODO*///	char flag[40];
/*TODO*///	int arrowize;
/*TODO*///	int subcheat_num;
/*TODO*///	int i;
/*TODO*///
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///	total = 0;
/*TODO*///	/* No submenu active, display the main cheat menu */
/*TODO*///	menu_item[total++] = ui_getstring (UI_cheatname);
/*TODO*///	menu_item[total++] = ui_getstring (UI_cheatdescription);
/*TODO*///	for (i = 0; i <= CheatTable[cheat_num].num_sub; i ++)
/*TODO*///	{
/*TODO*///		menu_item[total++] = ui_getstring (UI_cpu);
/*TODO*///		menu_item[total++] = ui_getstring (UI_address);
/*TODO*///		menu_item[total++] = ui_getstring (UI_value);
/*TODO*///		menu_item[total++] = ui_getstring (UI_code);
/*TODO*///	}
/*TODO*///	menu_item[total++] = ui_getstring (UI_returntoprior);
/*TODO*///	menu_item[total] = 0;
/*TODO*///
/*TODO*///	arrowize = 0;
/*TODO*///
/*TODO*///	/* set up the submenu selections */
/*TODO*///	total2 = 0;
/*TODO*///	for (i = 0; i < 40; i ++)
/*TODO*///		flag[i] = 0;
/*TODO*///
/*TODO*///	/* if we're editing the label, make it inverse */
/*TODO*///	if (textedit_active)
/*TODO*///		flag[sel] = 1;
/*TODO*///
/*TODO*///	/* name */
/*TODO*///	if (CheatTable[cheat_num].name != 0x00)
/*TODO*///		sprintf (setting[total2], "%s", CheatTable[cheat_num].name);
/*TODO*///	else
/*TODO*///		strcpy (setting[total2], ui_getstring (UI_none));
/*TODO*///	menu_subitem[total2] = setting[total2]; total2++;
/*TODO*///
/*TODO*///	/* comment */
/*TODO*///	if (CheatTable[cheat_num].comment && CheatTable[cheat_num].comment != 0x00)
/*TODO*///		sprintf (setting[total2], "%s...", ui_getstring (UI_moreinfo));
/*TODO*///	else
/*TODO*///		strcpy (setting[total2], ui_getstring (UI_none));
/*TODO*///	menu_subitem[total2] = setting[total2]; total2++;
/*TODO*///
/*TODO*///	/* Subcheats */
/*TODO*///	for (i = 0; i <= CheatTable[cheat_num].num_sub; i ++)
/*TODO*///	{
/*TODO*///		subcheat = &CheatTable[cheat_num].subcheat[i];
/*TODO*///
/*TODO*///		/* cpu number */
/*TODO*///		sprintf (setting[total2], "%d", subcheat->cpu);
/*TODO*///		menu_subitem[total2] = setting[total2]; total2++;
/*TODO*///
/*TODO*///		/* address */
/*TODO*///		if (cpunum_address_bits(subcheat->cpu) <= 16)
/*TODO*///		{
/*TODO*///			sprintf (setting[total2], "%04X", subcheat->address);
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			sprintf (setting[total2], "%08X", subcheat->address);
/*TODO*///		}
/*TODO*///		menu_subitem[total2] = setting[total2]; total2++;
/*TODO*///
/*TODO*///		/* value */
/*TODO*///		sprintf (setting[total2], "%d", subcheat->data);
/*TODO*///		menu_subitem[total2] = setting[total2]; total2++;
/*TODO*///
/*TODO*///		/* code */
/*TODO*///		sprintf (setting[total2], "%d", subcheat->code);
/*TODO*///		menu_subitem[total2] = setting[total2]; total2++;
/*TODO*///
/*TODO*///		menu_subitem[total2] = NULL;
/*TODO*///	}
/*TODO*///
/*TODO*///	ui_displaymenu(bitmap,menu_item,menu_subitem,flag,sel,arrowize);
/*TODO*///
/*TODO*///	if (code_pressed_memory_repeat (KEYCODE_INSERT, 8))
/*TODO*///	{
/*TODO*///		if ((sel >= 2) && (sel <= ((CheatTable[cheat_num].num_sub + 1) * 4) + 1))
/*TODO*///		{
/*TODO*///			subcheat_num = (sel - 2) % 4;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			subcheat_num = CheatTable[cheat_num].num_sub + 1;
/*TODO*///		}
/*TODO*///
/*TODO*///		/* add a new subcheat at the current position (or the end) */
/*TODO*///		subcheat_insert_new (cheat_num, subcheat_num);
/*TODO*///	}
/*TODO*///
/*TODO*///	if (code_pressed_memory_repeat (KEYCODE_DEL, 8))
/*TODO*///	{
/*TODO*///		if ((sel >= 2) && (sel <= ((CheatTable[cheat_num].num_sub + 1) * 4) + 1))
/*TODO*///		{
/*TODO*///			subcheat_num = (sel - 2) % 4;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			subcheat_num = CheatTable[cheat_num].num_sub;
/*TODO*///		}
/*TODO*///
/*TODO*///		if (CheatTable[cheat_num].num_sub != 0)
/*TODO*///			subcheat_delete (cheat_num, subcheat_num);
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///	{
/*TODO*///		textedit_active = 0;
/*TODO*///		sel = (sel + 1) % total;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///	{
/*TODO*///		textedit_active = 0;
/*TODO*///		sel = (sel + total - 1) % total;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_LEFT,8))
/*TODO*///	{
/*TODO*///		if ((sel >= 2) && (sel <= ((CheatTable[cheat_num].num_sub + 1) * 4) + 1))
/*TODO*///		{
/*TODO*///			int newsel;
/*TODO*///
/*TODO*///			subcheat = &CheatTable[cheat_num].subcheat[(sel - 2) / 4];
/*TODO*///			newsel = (sel - 2) % 4;
/*TODO*///
/*TODO*///			switch (newsel)
/*TODO*///			{
/*TODO*///				case 0: /* CPU */
/*TODO*///					subcheat->cpu --;
/*TODO*///					/* skip audio CPUs when the sound is off */
/*TODO*///					if (CPU_AUDIO_OFF(subcheat->cpu))
/*TODO*///						subcheat->cpu --;
/*TODO*///					if (subcheat->cpu < 0)
/*TODO*///						subcheat->cpu = cpu_gettotalcpu() - 1;
/*TODO*///					subcheat->address &= cpunum_address_mask(subcheat->cpu);
/*TODO*///					break;
/*TODO*///				case 1: /* address */
/*TODO*///					textedit_active = 0;
/*TODO*///					subcheat->address --;
/*TODO*///					subcheat->address &= cpunum_address_mask(subcheat->cpu);
/*TODO*///					break;
/*TODO*///				case 2: /* value */
/*TODO*///					textedit_active = 0;
/*TODO*///					subcheat->data --;
/*TODO*///					subcheat->data &= 0xff;
/*TODO*///					break;
/*TODO*///				case 3: /* code */
/*TODO*///					textedit_active = 0;
/*TODO*///					subcheat->code --;
/*TODO*///					break;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_RIGHT,8))
/*TODO*///	{
/*TODO*///		if ((sel >= 2) && (sel <= ((CheatTable[cheat_num].num_sub+1) * 4) + 1))
/*TODO*///		{
/*TODO*///			int newsel;
/*TODO*///
/*TODO*///			subcheat = &CheatTable[cheat_num].subcheat[(sel - 2) / 4];
/*TODO*///			newsel = (sel - 2) % 4;
/*TODO*///
/*TODO*///			switch (newsel)
/*TODO*///			{
/*TODO*///				case 0: /* CPU */
/*TODO*///					subcheat->cpu ++;
/*TODO*///					/* skip audio CPUs when the sound is off */
/*TODO*///					if (CPU_AUDIO_OFF(subcheat->cpu))
/*TODO*///						subcheat->cpu ++;
/*TODO*///					if (subcheat->cpu >= cpu_gettotalcpu())
/*TODO*///						subcheat->cpu = 0;
/*TODO*///					subcheat->address &= cpunum_address_mask(subcheat->cpu);
/*TODO*///					break;
/*TODO*///				case 1: /* address */
/*TODO*///					textedit_active = 0;
/*TODO*///					subcheat->address ++;
/*TODO*///					subcheat->address &= cpunum_address_mask(subcheat->cpu);
/*TODO*///					break;
/*TODO*///				case 2: /* value */
/*TODO*///					textedit_active = 0;
/*TODO*///					subcheat->data ++;
/*TODO*///					subcheat->data &= 0xff;
/*TODO*///					break;
/*TODO*///				case 3: /* code */
/*TODO*///					textedit_active = 0;
/*TODO*///					subcheat->code ++;
/*TODO*///					break;
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///	{
/*TODO*///		if (sel == ((CheatTable[cheat_num].num_sub+1) * 4) + 2)
/*TODO*///		{
/*TODO*///			/* return to main menu */
/*TODO*///			submenu_choice = 0;
/*TODO*///			sel = -1;
/*TODO*///		}
/*TODO*///		else if (/*(sel == 4) ||*/ (sel == 0))
/*TODO*///		{
/*TODO*///			/* wait for key up */
/*TODO*///			while (input_ui_pressed(IPT_UI_SELECT)) {};
/*TODO*///
/*TODO*///			/* flush the text buffer */
/*TODO*///			osd_readkey_unicode (1);
/*TODO*///			textedit_active ^= 1;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			submenu_choice = 1;
/*TODO*///			/* tell updatescreen() to clean after us */
/*TODO*///			need_to_clear_bitmap = 1;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* Cancel pops us up a menu level */
/*TODO*///	if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		sel = -1;
/*TODO*///
/*TODO*///	/* The UI key takes us all the way back out */
/*TODO*///	if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///		sel = -2;
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		textedit_active = 0;
/*TODO*///		/* flush the text buffer */
/*TODO*///		osd_readkey_unicode (1);
/*TODO*///		/* tell updatescreen() to clean after us */
/*TODO*///		need_to_clear_bitmap = 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* After we've weeded out any control characters, look for text */
/*TODO*///	if (textedit_active)
/*TODO*///	{
/*TODO*///		int code;
/*TODO*///
/*TODO*///#if 0
/*TODO*///		/* is this the address field? */
/*TODO*///		if (sel == 1)
/*TODO*///		{
/*TODO*///			INT8 hex_val;
/*TODO*///
/*TODO*///			/* see if a hex digit was typed */
/*TODO*///			hex_val = code_read_hex_async();
/*TODO*///			if (hex_val != -1)
/*TODO*///			{
/*TODO*///				/* shift over one digit, add in the new value and clip */
/*TODO*///				subcheat->address <<= 4;
/*TODO*///				subcheat->address |= hex_val;
/*TODO*///				subcheat->address &= cpunum_address_mask(subcheat->cpu);
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///#endif
/*TODO*///		if (CheatTable[cheat_num].name)
/*TODO*///		{
/*TODO*///			int length = strlen(CheatTable[cheat_num].name);
/*TODO*///
/*TODO*///			code = osd_readkey_unicode(0) & 0xff; /* no 16-bit support */
/*TODO*///
/*TODO*///			if (code)
/*TODO*///			{
/*TODO*///				if (code == 0x08) /* backspace */
/*TODO*///				{
/*TODO*///					/* clear the buffer */
/*TODO*///					CheatTable[cheat_num].name[0] = 0x00;
/*TODO*///				}
/*TODO*///				else
/*TODO*///				{
/*TODO*///					/* append the character */
/*TODO*///					CheatTable[cheat_num].name = realloc (CheatTable[cheat_num].name, length + 2);
/*TODO*///					if (CheatTable[cheat_num].name != NULL)
/*TODO*///					{
/*TODO*///						CheatTable[cheat_num].name[length] = code;
/*TODO*///						CheatTable[cheat_num].name[length+1] = 0x00;
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///#ifdef macintosh
/*TODO*///#pragma mark -
/*TODO*///#endif
/*TODO*///
/*TODO*////* make a copy of a source ram table to a dest. ram table */
/*TODO*///static void copy_ram (struct ExtMemory *dest, struct ExtMemory *src)
/*TODO*///{
/*TODO*///	struct ExtMemory *ext_dest, *ext_src;
/*TODO*///
/*TODO*///	for (ext_src = src, ext_dest = dest; ext_src->data; ext_src++, ext_dest++)
/*TODO*///	{
/*TODO*///		memcpy (ext_dest->data, ext_src->data, ext_src->end - ext_src->start + 1);
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*////* make a copy of each ram area from search CPU ram to the specified table */
/*TODO*///static void backup_ram (struct ExtMemory *table, int cpu)
/*TODO*///{
/*TODO*///	struct ExtMemory *ext;
/*TODO*///	unsigned char *gameram;
/*TODO*///
/*TODO*///	for (ext = table; ext->data; ext++)
/*TODO*///	{
/*TODO*///		int i;
/*TODO*///		gameram = memory_find_base (cpu, ext->start);
/*TODO*///		memcpy (ext->data, gameram, ext->end - ext->start + 1);
/*TODO*///		for (i=0; i <= ext->end - ext->start; i++)
/*TODO*///			ext->data[i] = cpunum_read_byte(cpu, i+ext->start);
/*TODO*///	}
/*TODO*///}
/*TODO*///
/*TODO*////* set every byte in specified table to data */
/*TODO*///static void memset_ram (struct ExtMemory *table, unsigned char data)
/*TODO*///{
/*TODO*///	struct ExtMemory *ext;
/*TODO*///
/*TODO*///	for (ext = table; ext->data; ext++)
/*TODO*///		memset (ext->data, data, ext->end - ext->start + 1);
/*TODO*///}
/*TODO*///
/*TODO*////* free all the memory and init the table */
/*TODO*///static void reset_table (struct ExtMemory *table)
/*TODO*///{
/*TODO*///	struct ExtMemory *ext;
/*TODO*///
/*TODO*///	for (ext = table; ext->data; ext++)
/*TODO*///		free (ext->data);
/*TODO*///	memset (table, 0, sizeof (struct ExtMemory) * MAX_EXT_MEMORY);
/*TODO*///}
/*TODO*///
/*TODO*////* create tables for storing copies of all MWA_RAM areas */
/*TODO*///static int build_tables (int cpu)
/*TODO*///{
/*TODO*///	/* const struct MemoryReadAddress *mra = Machine->drv->cpu[SearchCpuNo].memory_read; */
/*TODO*///	const struct MemoryWriteAddress *mwa = Machine->drv->cpu[cpu].memory_write;
/*TODO*///
/*TODO*///	int region = REGION_CPU1+cpu;
/*TODO*///
/*TODO*///	struct ExtMemory *ext_sr = StartRam;
/*TODO*///	struct ExtMemory *ext_br = BackupRam;
/*TODO*///	struct ExtMemory *ext_ft = FlagTable;
/*TODO*///
/*TODO*///	struct ExtMemory *ext_obr = OldBackupRam;
/*TODO*///	struct ExtMemory *ext_oft = OldFlagTable;
/*TODO*///
/*TODO*///	static int bail = 0; /* set to 1 if this routine fails during startup */
/*TODO*///
/*TODO*///	int i;
/*TODO*///
/*TODO*///	int NoMemArea = 0;
/*TODO*///
/*TODO*///	/* Trap memory allocation errors */
/*TODO*///	int MemoryNeeded = 0;
/*TODO*///
/*TODO*///	/* Search speedup : (the games should be dasmed to confirm this) */
/*TODO*///	/* Games based on Exterminator driver should scan BANK1		   */
/*TODO*///	/* Games based on SmashTV driver should scan BANK2		   */
/*TODO*///	/* NEOGEO games should only scan BANK1 (0x100000 -> 0x01FFFF)    */
/*TODO*///	int CpuToScan = -1;
/*TODO*///	int BankToScanTable[9];	 /* 0 for RAM & 1-8 for Banks 1-8 */
/*TODO*///
/*TODO*///	for (i = 0; i < 9;i ++)
/*TODO*///	BankToScanTable[i] = ( fastsearch != 2 );
/*TODO*///
/*TODO*///#if (HAS_TMS34010)
/*TODO*///	if ((Machine->drv->cpu[1].cpu_type & ~CPU_FLAGS_MASK) == CPU_TMS34010)
/*TODO*///	{
/*TODO*///		/* 2nd CPU is 34010: games based on Exterminator driver */
/*TODO*///		CpuToScan = 0;
/*TODO*///		BankToScanTable[1] = 1;
/*TODO*///	}
/*TODO*///	else if ((Machine->drv->cpu[0].cpu_type & ~CPU_FLAGS_MASK) == CPU_TMS34010)
/*TODO*///	{
/*TODO*///		/* 1st CPU but not 2nd is 34010: games based on SmashTV driver */
/*TODO*///		CpuToScan = 0;
/*TODO*///		BankToScanTable[2] = 1;
/*TODO*///	}
/*TODO*///#endif
/*TODO*///#ifndef MESS
/*TODO*///#ifndef TINY_COMPILE
/*TODO*///	if (Machine->gamedrv->clone_of == &driver_neogeo)
/*TODO*///	{
/*TODO*///		/* games based on NEOGEO driver */
/*TODO*///		CpuToScan = 0;
/*TODO*///		BankToScanTable[1] = 1;
/*TODO*///	}
/*TODO*///#endif
/*TODO*///#endif
/*TODO*///
/*TODO*///	/* No CPU so we scan RAM & BANKn */
/*TODO*///	if ((CpuToScan == -1) && (fastsearch == 2))
/*TODO*///		for (i = 0; i < 9;i ++)
/*TODO*///			BankToScanTable[i] = 1;
/*TODO*///
/*TODO*///	/* free memory that was previously allocated if no error occured */
/*TODO*///	/* it must also be there because mwa varies from one CPU to another */
/*TODO*///	if (!bail)
/*TODO*///	{
/*TODO*///		reset_table (StartRam);
/*TODO*///		reset_table (BackupRam);
/*TODO*///		reset_table (FlagTable);
/*TODO*///
/*TODO*///		reset_table (OldBackupRam);
/*TODO*///		reset_table (OldFlagTable);
/*TODO*///	}
/*TODO*///
/*TODO*///	bail = 0;
/*TODO*///
/*TODO*///#if 0
/*TODO*///	/* Message to show that something is in progress */
/*TODO*///	cheat_clearbitmap();
/*TODO*///	yPos = (MachHeight - FontHeight) / 2;
/*TODO*///	xprintf(0, 0, yPos, "Allocating Memory...");
/*TODO*///#endif
/*TODO*///
/*TODO*///	NoMemArea = 0;
/*TODO*///	while (mwa->start != -1)
/*TODO*///	{
/*TODO*///		/* int (*handler)(int) = mra->handler; */
/*TODO*///		mem_write_handler handler = mwa->handler;
/*TODO*///		int size = (mwa->end - mwa->start) + 1;
/*TODO*///
/*TODO*///		if (SkipBank(CpuToScan, BankToScanTable, handler))
/*TODO*///		{
/*TODO*///			NoMemArea++;
/*TODO*///			mwa++;
/*TODO*///			continue;
/*TODO*///		}
/*TODO*///
/*TODO*///#if 0
/*TODO*///		if ((fastsearch == 3) && (!MemToScanTable[NoMemArea].Enabled))
/*TODO*///		{
/*TODO*///			NoMemArea++;
/*TODO*///			mwa++;
/*TODO*///			continue;
/*TODO*///		}
/*TODO*///#endif
/*TODO*///
/*TODO*///		/* time to allocate */
/*TODO*///		if (!bail)
/*TODO*///		{
/*TODO*///			ext_sr->data = malloc (size);
/*TODO*///			ext_br->data = malloc (size);
/*TODO*///			ext_ft->data = malloc (size);
/*TODO*///
/*TODO*///			ext_obr->data = malloc (size);
/*TODO*///			ext_oft->data = malloc (size);
/*TODO*///
/*TODO*///			if (ext_sr->data == NULL)
/*TODO*///			{
/*TODO*///				bail = 1;
/*TODO*///				MemoryNeeded += size;
/*TODO*///			}
/*TODO*///			if (ext_br->data == NULL)
/*TODO*///			{
/*TODO*///				bail = 1;
/*TODO*///				MemoryNeeded += size;
/*TODO*///			}
/*TODO*///			if (ext_ft->data == NULL)
/*TODO*///			{
/*TODO*///				bail = 1;
/*TODO*///				MemoryNeeded += size;
/*TODO*///			}
/*TODO*///
/*TODO*///			if (ext_obr->data == NULL)
/*TODO*///			{
/*TODO*///				bail = 1;
/*TODO*///				MemoryNeeded += size;
/*TODO*///			}
/*TODO*///			if (ext_oft->data == NULL)
/*TODO*///			{
/*TODO*///				bail = 1;
/*TODO*///				MemoryNeeded += size;
/*TODO*///			}
/*TODO*///
/*TODO*///			if (!bail)
/*TODO*///			{
/*TODO*///				ext_sr->start = ext_br->start = ext_ft->start = mwa->start;
/*TODO*///				ext_sr->end = ext_br->end = ext_ft->end = mwa->end;
/*TODO*///				ext_sr->region = ext_br->region = ext_ft->region = region;
/*TODO*///				ext_sr++, ext_br++, ext_ft++;
/*TODO*///
/*TODO*///				ext_obr->start = ext_oft->start = mwa->start;
/*TODO*///				ext_obr->end = ext_oft->end = mwa->end;
/*TODO*///				ext_obr->region = ext_oft->region = region;
/*TODO*///				ext_obr++, ext_oft++;
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///			MemoryNeeded += (5 * size);
/*TODO*///
/*TODO*///		NoMemArea++;
/*TODO*///		mwa++;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* free memory that was previously allocated if an error occured */
/*TODO*///	if (bail)
/*TODO*///	{
/*TODO*///		reset_table (StartRam);
/*TODO*///		reset_table (BackupRam);
/*TODO*///		reset_table (FlagTable);
/*TODO*///
/*TODO*///		reset_table (OldBackupRam);
/*TODO*///		reset_table (OldFlagTable);
/*TODO*///
/*TODO*///#if 0
/*TODO*///		cheat_clearbitmap();
/*TODO*///		yPos = (MachHeight - 10 * FontHeight) / 2;
/*TODO*///		xprintf(0, 0, yPos, "Error while allocating memory !");
/*TODO*///		yPos += (2 * FontHeight);
/*TODO*///		xprintf(0, 0, yPos, "You need %d more bytes", MemoryNeeded);
/*TODO*///		yPos += FontHeight;
/*TODO*///		xprintf(0, 0, yPos, "(0x%X) of free memory", MemoryNeeded);
/*TODO*///		yPos += (2 * FontHeight);
/*TODO*///		xprintf(0, 0, yPos, "No search available for CPU %d", currentSearchCPU);
/*TODO*///		yPos += (4 * FontHeight);
/*TODO*///		xprintf(0, 0, yPos, "Press A Key To Continue...");
/*TODO*///		key = keyboard_read_sync();
/*TODO*///		while (keyboard_pressed(key)) ; /* wait for key release */
/*TODO*///		cheat_clearbitmap();
/*TODO*///#endif
/*TODO*///	  }
/*TODO*///
/*TODO*/////	ClearTextLine (1, yPos);
/*TODO*///
/*TODO*///	return bail;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////* Returns 1 if memory area has to be skipped */
/*TODO*///static int SkipBank(int CpuToScan, int *BankToScanTable, mem_write_handler handler)
/*TODO*///{
/*TODO*///	int res = 0;
/*TODO*///
/*TODO*///	if ((fastsearch == 1) || (fastsearch == 2))
/*TODO*///	{
/*TODO*///		switch ((FPTR)handler)
/*TODO*///		{
/*TODO*///			case (FPTR)MWA_RAM:
/*TODO*///				res = !BankToScanTable[0];
/*TODO*///				break;
/*TODO*///			case (FPTR)MWA_BANK1:
/*TODO*///				res = !BankToScanTable[1];
/*TODO*///				break;
/*TODO*///			case (FPTR)MWA_BANK2:
/*TODO*///				res = !BankToScanTable[2];
/*TODO*///				break;
/*TODO*///			case (FPTR)MWA_BANK3:
/*TODO*///				res = !BankToScanTable[3];
/*TODO*///				break;
/*TODO*///			case (FPTR)MWA_BANK4:
/*TODO*///				res = !BankToScanTable[4];
/*TODO*///				break;
/*TODO*///			case (FPTR)MWA_BANK5:
/*TODO*///				res = !BankToScanTable[5];
/*TODO*///				break;
/*TODO*///			case (FPTR)MWA_BANK6:
/*TODO*///				res = !BankToScanTable[6];
/*TODO*///				break;
/*TODO*///			case (FPTR)MWA_BANK7:
/*TODO*///				res = !BankToScanTable[7];
/*TODO*///				break;
/*TODO*///			case (FPTR)MWA_BANK8:
/*TODO*///				res = !BankToScanTable[8];
/*TODO*///				break;
/*TODO*///			default:
/*TODO*///				res = 1;
/*TODO*///				break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	return(res);
/*TODO*///}
/*TODO*///
/*TODO*///INT32 PerformSearch (struct osd_bitmap *bitmap, INT32 selected)
/*TODO*///{
/*TODO*///	return -1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*////*****************
/*TODO*/// * Start a cheat search
/*TODO*/// * If the method 1 is selected, ask the user a number
/*TODO*/// * In all cases, backup the ram.
/*TODO*/// *
/*TODO*/// * Ask the user to select one of the following:
/*TODO*/// *	1 - Lives or other number (byte) (exact)        ask a start value, ask new value
/*TODO*/// *	2 - Timers (byte) (+ or - X)                    nothing at start,  ask +-X
/*TODO*/// *	3 - Energy (byte) (less, equal or greater)	    nothing at start,  ask less, equal or greater
/*TODO*/// *	4 - Status (bit)  (true or false)               nothing at start,  ask same or opposite
/*TODO*/// *	5 - Slow but sure (Same as start or different)  nothing at start,  ask same or different
/*TODO*/// *
/*TODO*/// * Another method is used in the Pro action Replay the Energy method
/*TODO*/// *	you can tell that the value is now 25%/50%/75%/100% as the start
/*TODO*/// *	the problem is that I probably cannot search for exactly 50%, so
/*TODO*/// *	that do I do? search +/- 10% ?
/*TODO*/// * If you think of other way to search for codes, let me know.
/*TODO*/// */
/*TODO*///
/*TODO*///INT32 StartSearch (struct osd_bitmap *bitmap, INT32 selected)
/*TODO*///{
/*TODO*///	enum
/*TODO*///	{
/*TODO*///		Menu_CPU = 0,
/*TODO*///		Menu_Value,
/*TODO*///		Menu_Time,
/*TODO*///		Menu_Energy,
/*TODO*///		Menu_Bit,
/*TODO*///		Menu_Byte,
/*TODO*///		Menu_Speed,
/*TODO*///		Menu_Return,
/*TODO*///		Menu_Total
/*TODO*///	};
/*TODO*///
/*TODO*///	const char *menu_item[Menu_Total];
/*TODO*///	const char *menu_subitem[Menu_Total];
/*TODO*///	char setting[Menu_Total][30];
/*TODO*///	INT32 sel;
/*TODO*///	UINT8 total = 0;
/*TODO*///	static INT8 submenu_choice;
/*TODO*///	int i;
/*TODO*/////	char flag[Menu_Total];
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///	/* If a submenu has been selected, go there */
/*TODO*///	if (submenu_choice)
/*TODO*///	{
/*TODO*///		switch (sel)
/*TODO*///		{
/*TODO*///			case Menu_Value:
/*TODO*///				searchType = kSearch_Value;
/*TODO*///				submenu_choice = PerformSearch (bitmap, submenu_choice);
/*TODO*///				break;
/*TODO*///			case Menu_Time:
/*TODO*///				searchType = kSearch_Time;
/*TODO*/////				submenu_choice = PerformSearch (submenu_choice);
/*TODO*///				break;
/*TODO*///			case Menu_Energy:
/*TODO*///				searchType = kSearch_Energy;
/*TODO*/////				submenu_choice = PerformSearch (submenu_choice);
/*TODO*///				break;
/*TODO*///			case Menu_Bit:
/*TODO*///				searchType = kSearch_Bit;
/*TODO*/////				submenu_choice = PerformSearch (submenu_choice);
/*TODO*///				break;
/*TODO*///			case Menu_Byte:
/*TODO*///				searchType = kSearch_Byte;
/*TODO*/////				submenu_choice = PerformSearch (submenu_choice);
/*TODO*///				break;
/*TODO*///			case Menu_Speed:
/*TODO*/////				submenu_choice = RestoreSearch (submenu_choice);
/*TODO*///				break;
/*TODO*///			case Menu_Return:
/*TODO*///				submenu_choice = 0;
/*TODO*///				sel = -1;
/*TODO*///				break;
/*TODO*///		}
/*TODO*///
/*TODO*///		if (submenu_choice == -1)
/*TODO*///			submenu_choice = 0;
/*TODO*///
/*TODO*///		return sel + 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* No submenu active, display the main cheat menu */
/*TODO*///	menu_item[total++] = ui_getstring (UI_cpu);
/*TODO*///	menu_item[total++] = ui_getstring (UI_search_lives);
/*TODO*///	menu_item[total++] = ui_getstring (UI_search_timers);
/*TODO*///	menu_item[total++] = ui_getstring (UI_search_energy);
/*TODO*///	menu_item[total++] = ui_getstring (UI_search_status);
/*TODO*///	menu_item[total++] = ui_getstring (UI_search_slow);
/*TODO*///	menu_item[total++] = ui_getstring (UI_search_speed);
/*TODO*///	menu_item[total++] = ui_getstring (UI_returntoprior);
/*TODO*///	menu_item[total] = 0;
/*TODO*///
/*TODO*///	/* clear out the subitem menu */
/*TODO*///	for (i = 0; i < Menu_Total; i ++)
/*TODO*///		menu_subitem[i] = NULL;
/*TODO*///
/*TODO*///	/* cpu number */
/*TODO*///	sprintf (setting[Menu_CPU], "%d", searchCPU);
/*TODO*///	menu_subitem[Menu_CPU] = setting[Menu_CPU];
/*TODO*///
/*TODO*///	/* lives/byte value */
/*TODO*///	sprintf (setting[Menu_Value], "%d", searchValue);
/*TODO*///	menu_subitem[Menu_Value] = setting[Menu_Value];
/*TODO*///
/*TODO*///
/*TODO*///	ui_displaymenu(bitmap,menu_item,menu_subitem,0,sel,0);
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///		sel = (sel + 1) % total;
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///		sel = (sel + total - 1) % total;
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_LEFT,8))
/*TODO*///	{
/*TODO*///		switch (sel)
/*TODO*///		{
/*TODO*///			case Menu_CPU:
/*TODO*///				searchCPU --;
/*TODO*///				/* skip audio CPUs when the sound is off */
/*TODO*///				if (CPU_AUDIO_OFF(searchCPU))
/*TODO*///					searchCPU --;
/*TODO*///				if (searchCPU < 0)
/*TODO*///					searchCPU = cpu_gettotalcpu() - 1;
/*TODO*///				break;
/*TODO*///			case Menu_Value:
/*TODO*///				searchValue --;
/*TODO*///				if (searchValue < 0)
/*TODO*///					searchValue = 255;
/*TODO*///				break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_RIGHT,8))
/*TODO*///	{
/*TODO*///		switch (sel)
/*TODO*///		{
/*TODO*///			case Menu_CPU:
/*TODO*///				searchCPU ++;
/*TODO*///				/* skip audio CPUs when the sound is off */
/*TODO*///				if (CPU_AUDIO_OFF(searchCPU))
/*TODO*///					searchCPU ++;
/*TODO*///				if (searchCPU >= cpu_gettotalcpu())
/*TODO*///					searchCPU = 0;
/*TODO*///				break;
/*TODO*///			case Menu_Value:
/*TODO*///				searchValue ++;
/*TODO*///				if (searchValue > 255)
/*TODO*///					searchValue = 0;
/*TODO*///				break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///	{
/*TODO*///		if (sel == Menu_Return)
/*TODO*///		{
/*TODO*///			submenu_choice = 0;
/*TODO*///			sel = -1;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			int count = 0;	/* Steph */
/*TODO*///
/*TODO*///			/* set up the search tables */
/*TODO*///			build_tables (searchCPU);
/*TODO*///
/*TODO*///			/* backup RAM */
/*TODO*///			backup_ram (StartRam, searchCPU);
/*TODO*///			backup_ram (BackupRam, searchCPU);
/*TODO*///
/*TODO*///			/* mark all RAM as good */
/*TODO*///			memset_ram (FlagTable, 0xff);
/*TODO*///
/*TODO*///			if (sel == Menu_Value)
/*TODO*///			{
/*TODO*///				/* flag locations that match the starting value */
/*TODO*///				struct ExtMemory *ext;
/*TODO*///				int j;	/* Steph - replaced all instances of 'i' with 'j' */
/*TODO*///
/*TODO*///				count = 0;
/*TODO*///				for (ext = FlagTable; ext->data; ext++)
/*TODO*///				{
/*TODO*///					for (j=0; j <= ext->end - ext->start; j++)
/*TODO*///					if (ext->data[j] != 0)
/*TODO*///					{
/*TODO*///						if ((cpunum_read_byte(searchCPU, j+ext->start) != searchValue) &&
/*TODO*///							((cpunum_read_byte(searchCPU, j+ext->start) != searchValue-1) /*||
/*TODO*///							(searchType != kSearch_Value)*/))
/*TODO*///
/*TODO*///							ext->data[j] = 0;
/*TODO*///						else
/*TODO*///							count ++;
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///
/*TODO*///			/* Copy the tables */
/*TODO*///			copy_ram (OldBackupRam, BackupRam);
/*TODO*///			copy_ram (OldFlagTable, FlagTable);
/*TODO*///
/*TODO*///			restoreStatus = kRestore_NoSave;
/*TODO*///
/*TODO*///			usrintf_showmessage_secs(4, "%s: %d", ui_getstring(UI_search_matches_found), count);
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* Cancel pops us up a menu level */
/*TODO*///	if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		sel = -1;
/*TODO*///
/*TODO*///	/* The UI key takes us all the way back out */
/*TODO*///	if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///		sel = -2;
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		/* tell updatescreen() to clean after us */
/*TODO*///		need_to_clear_bitmap = 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*///INT32 ContinueSearch (INT32 selected)
/*TODO*///{
/*TODO*///	return -1;
/*TODO*///}
/*TODO*///
/*TODO*///INT32 ViewSearchResults (struct osd_bitmap *bitmap, INT32 selected)
/*TODO*///{
/*TODO*///	int sel;
/*TODO*///	static INT8 submenu_choice;
/*TODO*///	const char *menu_item[MAX_SEARCHES + 2];
/*TODO*///	char buf[MAX_SEARCHES][20];
/*TODO*///	int i, total = 0;
/*TODO*///	struct ExtMemory *ext;
/*TODO*///	struct ExtMemory *ext_sr;
/*TODO*///
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///	/* Set up the menu */
/*TODO*///	for (ext = FlagTable, ext_sr = StartRam; ext->data /*&& Continue==0*/; ext++, ext_sr++)
/*TODO*///	{
/*TODO*///		for (i=0; i <= ext->end - ext->start; i++)
/*TODO*///			if (ext->data[i] != 0)
/*TODO*///			{
/*TODO*///				int TrueAddr, TrueData;
/*TODO*///				char fmt[40];
/*TODO*///
/*TODO*///				strcpy(fmt, FormatAddr(searchCPU,0));
/*TODO*///				strcat(fmt," = %02X");
/*TODO*///
/*TODO*///				TrueAddr = i+ext->start;
/*TODO*///				TrueData = ext_sr->data[i];
/*TODO*///				sprintf (buf[total], fmt, TrueAddr, TrueData);
/*TODO*///
/*TODO*///				menu_item[total] = buf[total];
/*TODO*///				total++;
/*TODO*///				if (total >= MAX_SEARCHES)
/*TODO*///				{
/*TODO*/////					Continue = i+ext->start;
/*TODO*///					break;
/*TODO*///				}
/*TODO*///			}
/*TODO*///	}
/*TODO*///
/*TODO*///	menu_item[total++] = ui_getstring (UI_returntoprior);
/*TODO*///	menu_item[total] = 0;	/* terminate array */
/*TODO*///
/*TODO*///	ui_displaymenu(bitmap,menu_item,0,0,sel,0);
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///		sel = (sel + 1) % total;
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///		sel = (sel + total - 1) % total;
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///	{
/*TODO*///		if (sel == total - 1)
/*TODO*///		{
/*TODO*///			submenu_choice = 0;
/*TODO*///			sel = -1;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			submenu_choice = 1;
/*TODO*///			/* tell updatescreen() to clean after us */
/*TODO*///			need_to_clear_bitmap = 1;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* Cancel pops us up a menu level */
/*TODO*///	if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		sel = -1;
/*TODO*///
/*TODO*///	/* The UI key takes us all the way back out */
/*TODO*///	if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///		sel = -2;
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		/* tell updatescreen() to clean after us */
/*TODO*///		need_to_clear_bitmap = 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
    public static void RestoreSearch() {
        /*TODO*///	int restoreString = NULL;	/* Steph */
/*TODO*///
/*TODO*///	switch (restoreStatus)
/*TODO*///	{
/*TODO*///		case kRestore_NoInit: restoreString = UI_search_noinit; break;
/*TODO*///		case kRestore_NoSave: restoreString = UI_search_nosave; break;
/*TODO*///		case kRestore_Done:   restoreString = UI_search_done; break;
/*TODO*///		case kRestore_OK:     restoreString = UI_search_OK; break;
/*TODO*///	}
/*TODO*///	usrintf_showmessage_secs(4, "%s", ui_getstring(restoreString));
/*TODO*///
/*TODO*///	/* Now restore the tables if possible */
/*TODO*///	if (restoreStatus == kRestore_OK)
/*TODO*///	{
/*TODO*///		copy_ram (BackupRam, OldBackupRam);
/*TODO*///		copy_ram (FlagTable, OldFlagTable);
/*TODO*///
/*TODO*///		/* flag it as restored so we don't do it again */
/*TODO*///		restoreStatus = kRestore_Done;
/*TODO*///	}
    }

    /*TODO*///static int FindFreeWatch (void)
/*TODO*///{
/*TODO*///	int i;
/*TODO*///	for (i = 0; i < MAX_WATCHES; i ++)
/*TODO*///	{
/*TODO*///		if (watches[i].num_bytes == 0)
/*TODO*///			return i;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* indicate no free watch found */
/*TODO*///	return -1;
/*TODO*///}
/*TODO*///
    public static void DisplayWatches(mame_bitmap bitmap) {
        /*TODO*///	int i;
/*TODO*///	char buf[256];
/*TODO*///
/*TODO*///	if ((!is_watch_active) || (!is_watch_visible)) return;
/*TODO*///
/*TODO*///	for (i = 0; i < MAX_WATCHES; i++)
/*TODO*///	{
/*TODO*///		/* Is this watchpoint active? */
/*TODO*///		if (watches[i].num_bytes != 0)
/*TODO*///		{
/*TODO*///			char buf2[80];
/*TODO*///
/*TODO*///			/* Display the first byte */
/*TODO*///			sprintf (buf, "%02x", cpunum_read_byte (watches[i].cpu, watches[i].address));
/*TODO*///
/*TODO*///			/* If this is for more than one byte, display the rest */
/*TODO*///			if (watches[i].num_bytes > 1)
/*TODO*///			{
/*TODO*///				int j;
/*TODO*///
/*TODO*///				for (j = 1; j < watches[i].num_bytes; j ++)
/*TODO*///				{
/*TODO*///					sprintf (buf2, " %02x", cpunum_read_byte (watches[i].cpu, watches[i].address + j));
/*TODO*///					strcat (buf, buf2);
/*TODO*///				}
/*TODO*///			}
/*TODO*///
/*TODO*///			/* Handle any labels */
/*TODO*///			switch (watches[i].label_type)
/*TODO*///			{
/*TODO*///				case 0:
/*TODO*///				default:
/*TODO*///					break;
/*TODO*///				case 1:
/*TODO*///					if (cpunum_address_bits(watches[i].cpu) <= 16)
/*TODO*///					{
/*TODO*///						sprintf (buf2, " (%04x)", watches[i].address);
/*TODO*///						strcat (buf, buf2);
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						sprintf (buf2, " (%08x)", watches[i].address);
/*TODO*///						strcat (buf, buf2);
/*TODO*///					}
/*TODO*///					break;
/*TODO*///				case 2:
/*TODO*///					{
/*TODO*///						sprintf (buf2, " (%s)", watches[i].label);
/*TODO*///						strcat (buf, buf2);
/*TODO*///					}
/*TODO*///					break;
/*TODO*///			}
/*TODO*///
/*TODO*///			ui_text (bitmap, buf, watches[i].x, watches[i].y);
/*TODO*///		}
/*TODO*///	}
    }
    /*TODO*///
/*TODO*///static INT32 ConfigureWatch (struct osd_bitmap *bitmap, INT32 selected, UINT8 watchnum)
/*TODO*///{
/*TODO*///#ifdef NUM_ENTRIES
/*TODO*///#undef NUM_ENTRIES
/*TODO*///#endif
/*TODO*///#define NUM_ENTRIES 9
/*TODO*///
/*TODO*///	int sel;
/*TODO*///	int total, total2;
/*TODO*///	static INT8 submenu_choice;
/*TODO*///	static UINT8 textedit_active;
/*TODO*///	const char *menu_item[NUM_ENTRIES];
/*TODO*///	const char *menu_subitem[NUM_ENTRIES];
/*TODO*///	char setting[NUM_ENTRIES][30];
/*TODO*///	char flag[NUM_ENTRIES];
/*TODO*///	int arrowize;
/*TODO*///	int i;
/*TODO*///
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///	total = 0;
/*TODO*///	/* No submenu active, display the main cheat menu */
/*TODO*///	menu_item[total++] = ui_getstring (UI_cpu);
/*TODO*///	menu_item[total++] = ui_getstring (UI_address);
/*TODO*///	menu_item[total++] = ui_getstring (UI_watchlength);
/*TODO*///	menu_item[total++] = ui_getstring (UI_watchlabeltype);
/*TODO*///	menu_item[total++] = ui_getstring (UI_watchlabel);
/*TODO*///	menu_item[total++] = ui_getstring (UI_watchx);
/*TODO*///	menu_item[total++] = ui_getstring (UI_watchy);
/*TODO*///	menu_item[total++] = ui_getstring (UI_returntoprior);
/*TODO*///	menu_item[total] = 0;
/*TODO*///
/*TODO*///	arrowize = 0;
/*TODO*///
/*TODO*///	/* set up the submenu selections */
/*TODO*///	total2 = 0;
/*TODO*///	for (i = 0; i < NUM_ENTRIES; i ++)
/*TODO*///		flag[i] = 0;
/*TODO*///
/*TODO*///	/* if we're editing the label, make it inverse */
/*TODO*///	if (textedit_active)
/*TODO*///		flag[sel] = 1;
/*TODO*///
/*TODO*///	/* cpu number */
/*TODO*///	sprintf (setting[total2], "%d", watches[watchnum].cpu);
/*TODO*///	menu_subitem[total2] = setting[total2]; total2++;
/*TODO*///
/*TODO*///	/* address */
/*TODO*///	if (cpunum_address_bits(watches[watchnum].cpu) <= 16)
/*TODO*///	{
/*TODO*///		sprintf (setting[total2], "%04x", watches[watchnum].address);
/*TODO*///	}
/*TODO*///	else
/*TODO*///	{
/*TODO*///		sprintf (setting[total2], "%08x", watches[watchnum].address);
/*TODO*///	}
/*TODO*///	menu_subitem[total2] = setting[total2]; total2++;
/*TODO*///
/*TODO*///	/* length */
/*TODO*///	sprintf (setting[total2], "%d", watches[watchnum].num_bytes);
/*TODO*///	menu_subitem[total2] = setting[total2]; total2++;
/*TODO*///
/*TODO*///	/* label type */
/*TODO*///	switch (watches[watchnum].label_type)
/*TODO*///	{
/*TODO*///		case 0:
/*TODO*///			strcpy (setting[total2], ui_getstring (UI_none));
/*TODO*///			break;
/*TODO*///		case 1:
/*TODO*///			strcpy (setting[total2], ui_getstring (UI_address));
/*TODO*///			break;
/*TODO*///		case 2:
/*TODO*///			strcpy (setting[total2], ui_getstring (UI_text));
/*TODO*///			break;
/*TODO*///	}
/*TODO*///	menu_subitem[total2] = setting[total2]; total2++;
/*TODO*///
/*TODO*///	/* label */
/*TODO*///	if (watches[watchnum].label[0] != 0x00)
/*TODO*///		sprintf (setting[total2], "%s", watches[watchnum].label);
/*TODO*///	else
/*TODO*///		strcpy (setting[total2], ui_getstring (UI_none));
/*TODO*///	menu_subitem[total2] = setting[total2]; total2++;
/*TODO*///
/*TODO*///	/* x */
/*TODO*///	sprintf (setting[total2], "%d", watches[watchnum].x);
/*TODO*///	menu_subitem[total2] = setting[total2]; total2++;
/*TODO*///
/*TODO*///	/* y */
/*TODO*///	sprintf (setting[total2], "%d", watches[watchnum].y);
/*TODO*///	menu_subitem[total2] = setting[total2]; total2++;
/*TODO*///
/*TODO*///	menu_subitem[total2] = NULL;
/*TODO*///
/*TODO*///	ui_displaymenu(bitmap,menu_item,menu_subitem,flag,sel,arrowize);
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///	{
/*TODO*///		textedit_active = 0;
/*TODO*///		sel = (sel + 1) % total;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///	{
/*TODO*///		textedit_active = 0;
/*TODO*///		sel = (sel + total - 1) % total;
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_LEFT,8))
/*TODO*///	{
/*TODO*///		switch (sel)
/*TODO*///		{
/*TODO*///			case 0: /* CPU */
/*TODO*///				watches[watchnum].cpu --;
/*TODO*///				/* skip audio CPUs when the sound is off */
/*TODO*///				if (CPU_AUDIO_OFF(watches[watchnum].cpu))
/*TODO*///					watches[watchnum].cpu --;
/*TODO*///				if (watches[watchnum].cpu < 0)
/*TODO*///					watches[watchnum].cpu = cpu_gettotalcpu() - 1;
/*TODO*///				watches[watchnum].address &= cpunum_address_mask(watches[watchnum].cpu);
/*TODO*///				break;
/*TODO*///			case 1: /* address */
/*TODO*///				textedit_active = 0;
/*TODO*///				watches[watchnum].address --;
/*TODO*///				watches[watchnum].address &= cpunum_address_mask(watches[watchnum].cpu);
/*TODO*///				break;
/*TODO*///			case 2: /* number of bytes */
/*TODO*///				watches[watchnum].num_bytes --;
/*TODO*///				if (watches[watchnum].num_bytes == (UINT8) -1)
/*TODO*///					watches[watchnum].num_bytes = 16;
/*TODO*///				break;
/*TODO*///			case 3: /* label type */
/*TODO*///				watches[watchnum].label_type --;
/*TODO*///				if (watches[watchnum].label_type == (UINT8) -1)
/*TODO*///					watches[watchnum].label_type = 2;
/*TODO*///				break;
/*TODO*///			case 4: /* label string */
/*TODO*///				textedit_active = 0;
/*TODO*///				break;
/*TODO*///			case 5: /* x */
/*TODO*///				watches[watchnum].x --;
/*TODO*///				if (watches[watchnum].x == (UINT16) -1)
/*TODO*///					watches[watchnum].x = Machine->uiwidth - 1;
/*TODO*///				break;
/*TODO*///			case 6: /* y */
/*TODO*///				watches[watchnum].y --;
/*TODO*///				if (watches[watchnum].y == (UINT16) -1)
/*TODO*///					watches[watchnum].y = Machine->uiheight - 1;
/*TODO*///				break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_RIGHT,8))
/*TODO*///	{
/*TODO*///		switch (sel)
/*TODO*///		{
/*TODO*///			case 0:
/*TODO*///				watches[watchnum].cpu ++;
/*TODO*///				/* skip audio CPUs when the sound is off */
/*TODO*///				if (CPU_AUDIO_OFF(watches[watchnum].cpu))
/*TODO*///					watches[watchnum].cpu ++;
/*TODO*///				if (watches[watchnum].cpu >= cpu_gettotalcpu())
/*TODO*///					watches[watchnum].cpu = 0;
/*TODO*///				watches[watchnum].address &= cpunum_address_mask(watches[watchnum].cpu);
/*TODO*///				break;
/*TODO*///			case 1:
/*TODO*///				textedit_active = 0;
/*TODO*///				watches[watchnum].address ++;
/*TODO*///				watches[watchnum].address &= cpunum_address_mask(watches[watchnum].cpu);
/*TODO*///				break;
/*TODO*///			case 2:
/*TODO*///				watches[watchnum].num_bytes ++;
/*TODO*///				if (watches[watchnum].num_bytes > 16)
/*TODO*///					watches[watchnum].num_bytes = 0;
/*TODO*///				break;
/*TODO*///			case 3:
/*TODO*///				watches[watchnum].label_type ++;
/*TODO*///				if (watches[watchnum].label_type > 2)
/*TODO*///					watches[watchnum].label_type = 0;
/*TODO*///				break;
/*TODO*///			case 4:
/*TODO*///				textedit_active = 0;
/*TODO*///				break;
/*TODO*///			case 5:
/*TODO*///				watches[watchnum].x ++;
/*TODO*///				if (watches[watchnum].x >= Machine->uiwidth)
/*TODO*///					watches[watchnum].x = 0;
/*TODO*///				break;
/*TODO*///			case 6:
/*TODO*///				watches[watchnum].y ++;
/*TODO*///				if (watches[watchnum].y >= Machine->uiheight)
/*TODO*///					watches[watchnum].y = 0;
/*TODO*///				break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* see if any watchpoints are active and set the flag if so */
/*TODO*///	is_watch_active = 0;
/*TODO*///	for (i = 0; i < MAX_WATCHES; i ++)
/*TODO*///	{
/*TODO*///		if (watches[i].num_bytes != 0)
/*TODO*///		{
/*TODO*///			is_watch_active = 1;
/*TODO*///			break;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///	{
/*TODO*///		if (sel == 7)
/*TODO*///		{
/*TODO*///			/* return to main menu */
/*TODO*///			submenu_choice = 0;
/*TODO*///			sel = -1;
/*TODO*///		}
/*TODO*///		else if ((sel == 4) || (sel == 1))
/*TODO*///		{
/*TODO*///			/* wait for key up */
/*TODO*///			while (input_ui_pressed(IPT_UI_SELECT)) {};
/*TODO*///
/*TODO*///			/* flush the text buffer */
/*TODO*///			osd_readkey_unicode (1);
/*TODO*///			textedit_active ^= 1;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			submenu_choice = 1;
/*TODO*///			/* tell updatescreen() to clean after us */
/*TODO*///			need_to_clear_bitmap = 1;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* Cancel pops us up a menu level */
/*TODO*///	if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		sel = -1;
/*TODO*///
/*TODO*///	/* The UI key takes us all the way back out */
/*TODO*///	if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///		sel = -2;
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		textedit_active = 0;
/*TODO*///		/* flush the text buffer */
/*TODO*///		osd_readkey_unicode (1);
/*TODO*///		/* tell updatescreen() to clean after us */
/*TODO*///		need_to_clear_bitmap = 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* After we've weeded out any control characters, look for text */
/*TODO*///	if (textedit_active)
/*TODO*///	{
/*TODO*///		int code;
/*TODO*///
/*TODO*///		/* is this the address field? */
/*TODO*///		if (sel == 1)
/*TODO*///		{
/*TODO*///			INT8 hex_val;
/*TODO*///
/*TODO*///			/* see if a hex digit was typed */
/*TODO*///			hex_val = code_read_hex_async();
/*TODO*///			if (hex_val != -1)
/*TODO*///			{
/*TODO*///				/* shift over one digit, add in the new value and clip */
/*TODO*///				watches[watchnum].address <<= 4;
/*TODO*///				watches[watchnum].address |= hex_val;
/*TODO*///				watches[watchnum].address &= cpunum_address_mask(watches[watchnum].cpu);
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			int length = strlen(watches[watchnum].label);
/*TODO*///
/*TODO*///			if (length < 254)
/*TODO*///			{
/*TODO*///				code = osd_readkey_unicode(0) & 0xff; /* no 16-bit support */
/*TODO*///
/*TODO*///				if (code)
/*TODO*///				{
/*TODO*///					if (code == 0x08) /* backspace */
/*TODO*///					{
/*TODO*///						/* clear the buffer */
/*TODO*///						watches[watchnum].label[0] = 0x00;
/*TODO*///					}
/*TODO*///					else
/*TODO*///					{
/*TODO*///						/* append the character */
/*TODO*///						watches[watchnum].label[length] = code;
/*TODO*///						watches[watchnum].label[length+1] = 0x00;
/*TODO*///					}
/*TODO*///				}
/*TODO*///			}
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*///static INT32 ChooseWatch (struct osd_bitmap *bitmap, INT32 selected)
/*TODO*///{
/*TODO*///	int sel;
/*TODO*///	static INT8 submenu_choice;
/*TODO*///	const char *menu_item[MAX_WATCHES + 2];
/*TODO*///	char buf[MAX_WATCHES][80];
/*TODO*///	const char *watchpoint_str = ui_getstring (UI_watchpoint);
/*TODO*///	const char *disabled_str = ui_getstring (UI_disabled);
/*TODO*///	int i, total = 0;
/*TODO*///
/*TODO*///
/*TODO*///	sel = selected - 1;
/*TODO*///
/*TODO*///	/* If a submenu has been selected, go there */
/*TODO*///	if (submenu_choice)
/*TODO*///	{
/*TODO*///		submenu_choice = ConfigureWatch (bitmap, submenu_choice, sel);
/*TODO*///
/*TODO*///		if (submenu_choice == -1)
/*TODO*///		{
/*TODO*///			submenu_choice = 0;
/*TODO*///			sel = -2;
/*TODO*///		}
/*TODO*///
/*TODO*///		return sel + 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	/* No submenu active, do the watchpoint menu */
/*TODO*///	for (i = 0; i < MAX_WATCHES; i ++)
/*TODO*///	{
/*TODO*///		sprintf (buf[i], "%s %d: ", watchpoint_str, i);
/*TODO*///		/* If the watchpoint is active (1 or more bytes long), show it */
/*TODO*///		if (watches[i].num_bytes)
/*TODO*///		{
/*TODO*///			char buf2[80];
/*TODO*///
/*TODO*///			if (cpunum_address_bits(watches[i].cpu) <= 16)
/*TODO*///			{
/*TODO*///				sprintf (buf2, "%04x", watches[i].address);
/*TODO*///				strcat (buf[i], buf2);
/*TODO*///			}
/*TODO*///			else
/*TODO*///			{
/*TODO*///				sprintf (buf2, "%08x", watches[i].address);
/*TODO*///				strcat (buf[i], buf2);
/*TODO*///			}
/*TODO*///		}
/*TODO*///		else
/*TODO*///			strcat (buf[i], disabled_str);
/*TODO*///
/*TODO*///		menu_item[total++] = buf[i];
/*TODO*///	}
/*TODO*///
/*TODO*///	menu_item[total++] = ui_getstring (UI_returntoprior);
/*TODO*///	menu_item[total] = 0;	/* terminate array */
/*TODO*///
/*TODO*///	ui_displaymenu(bitmap,menu_item,0,0,sel,0);
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_DOWN,8))
/*TODO*///		sel = (sel + 1) % total;
/*TODO*///
/*TODO*///	if (input_ui_pressed_repeat(IPT_UI_UP,8))
/*TODO*///		sel = (sel + total - 1) % total;
/*TODO*///
/*TODO*///	if (input_ui_pressed(IPT_UI_SELECT))
/*TODO*///	{
/*TODO*///		if (sel == MAX_WATCHES)
/*TODO*///		{
/*TODO*///			submenu_choice = 0;
/*TODO*///			sel = -1;
/*TODO*///		}
/*TODO*///		else
/*TODO*///		{
/*TODO*///			submenu_choice = 1;
/*TODO*///			/* tell updatescreen() to clean after us */
/*TODO*///			need_to_clear_bitmap = 1;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	/* Cancel pops us up a menu level */
/*TODO*///	if (input_ui_pressed(IPT_UI_CANCEL))
/*TODO*///		sel = -1;
/*TODO*///
/*TODO*///	/* The UI key takes us all the way back out */
/*TODO*///	if (input_ui_pressed(IPT_UI_CONFIGURE))
/*TODO*///		sel = -2;
/*TODO*///
/*TODO*///	if (sel == -1 || sel == -2)
/*TODO*///	{
/*TODO*///		/* tell updatescreen() to clean after us */
/*TODO*///		need_to_clear_bitmap = 1;
/*TODO*///	}
/*TODO*///
/*TODO*///	return sel + 1;
/*TODO*///}
/*TODO*///
/*TODO*///
/*TODO*///#ifdef macintosh
/*TODO*///#pragma mark -
/*TODO*///#endif
/*TODO*///
/*TODO*///static INT32 DisplayHelpFile (INT32 selected)
/*TODO*///{
/*TODO*///	return -1;
/*TODO*///}
/*TODO*///
/*TODO*///#ifdef macintosh
/*TODO*///#pragma mark -
/*TODO*///#endif
/*TODO*///
    static int cm_submenu_choice;
    public static final int Menu_EnableDisable = 0;
    public static final int Menu_AddEdit = 1;
    public static final int Menu_StartSearch = 2;
    public static final int Menu_ContinueSearch = 3;
    public static final int Menu_ViewResults = 4;
    public static final int Menu_RestoreSearch = 5;
    public static final int Menu_ChooseWatch = 6;
    public static final int Menu_DisplayHelp = 7;
    public static final int Menu_Return = 8;

    public static int cheat_menu(mame_bitmap bitmap, int selected) {

        String[] menu_item = new String[10];
        int sel;
        int total = 0;

        sel = selected - 1;

        /* If a submenu has been selected, go there */
        if (cm_submenu_choice != 0) {
            switch (sel) {
                case Menu_EnableDisable:
                    cm_submenu_choice = EnableDisableCheatMenu(bitmap, cm_submenu_choice);
                    break;
                case Menu_AddEdit:
                    /*TODO*///				submenu_choice = AddEditCheatMenu (bitmap, submenu_choice);
                    cm_submenu_choice = -1;/*TODO Remove it when implemented*/
                    break;
                case Menu_StartSearch:
                    /*TODO*///				submenu_choice = StartSearch (bitmap, submenu_choice);
                    cm_submenu_choice = -1;/*TODO Remove it when implemented*/
                    break;
                case Menu_ContinueSearch:
                    /*TODO*///				submenu_choice = ContinueSearch (submenu_choice);
                    cm_submenu_choice = -1;/*TODO Remove it when implemented*/
                    break;
                case Menu_ViewResults:
                    /*TODO*///				submenu_choice = ViewSearchResults (bitmap, submenu_choice);
                    cm_submenu_choice = -1;/*TODO Remove it when implemented*/
                    break;
                case Menu_ChooseWatch:
                    /*TODO*///				submenu_choice = ChooseWatch (bitmap, submenu_choice);
                    cm_submenu_choice = -1;/*TODO Remove it when implemented*/
                    break;
                case Menu_DisplayHelp:
                    /*TODO*///				submenu_choice = DisplayHelpFile (submenu_choice);
                    cm_submenu_choice = -1;/*TODO Remove it when implemented*/
                    break;
                case Menu_Return:
                    cm_submenu_choice = 0;
                    sel = -1;
                    break;
            }

            if (cm_submenu_choice == -1) {
                cm_submenu_choice = 0;
            }

            return sel + 1;
        }

        /* No submenu active, display the main cheat menu */
        menu_item[total++] = ui_getstring(UI_enablecheat);
        menu_item[total++] = ui_getstring(UI_addeditcheat);
        menu_item[total++] = ui_getstring(UI_startcheat);
        menu_item[total++] = ui_getstring(UI_continuesearch);
        menu_item[total++] = ui_getstring(UI_viewresults);
        menu_item[total++] = ui_getstring(UI_restoreresults);
        menu_item[total++] = ui_getstring(UI_memorywatch);
        menu_item[total++] = ui_getstring(UI_generalhelp);
        menu_item[total++] = ui_getstring(UI_returntomain);
        menu_item[total] = null;

        ui_displaymenu(bitmap, menu_item, null, null, sel, 0);

        if (input_ui_pressed_repeat(IPT_UI_DOWN, 8) != 0) {
            sel = (sel + 1) % total;
        }

        if (input_ui_pressed_repeat(IPT_UI_UP, 8) != 0) {
            sel = (sel + total - 1) % total;
        }

        if (input_ui_pressed(IPT_UI_SELECT) != 0) {
            if (sel == Menu_Return) {
                cm_submenu_choice = 0;
                sel = -1;
            } else if (sel == Menu_RestoreSearch) {
                RestoreSearch();
            } else {
                cm_submenu_choice = 1;
                /* tell updatescreen() to clean after us */
                schedule_full_refresh();
            }
        }

        /* Cancel pops us up a menu level */
        if (input_ui_pressed(IPT_UI_CANCEL) != 0) {
            sel = -1;
        }

        /* The UI key takes us all the way back out */
        if (input_ui_pressed(IPT_UI_CONFIGURE) != 0) {
            sel = -2;
        }

        if (sel == -1 || sel == -2) {
            /* tell updatescreen() to clean after us */
            schedule_full_refresh();
        }

        return sel + 1;
    }

    /* Free allocated arrays */
    public static void StopCheat() {
        /*TODO*///	int i;
/*TODO*///
/*TODO*///	for (i = 0; i < LoadedCheatTotal; i ++)
/*TODO*///	{
/*TODO*///		/* free storage for the strings */
/*TODO*///		if (CheatTable[i].name)
/*TODO*///		{
/*TODO*///			free (CheatTable[i].name);
/*TODO*///			CheatTable[i].name = NULL;
/*TODO*///		}
/*TODO*///		if (CheatTable[i].comment)
/*TODO*///		{
/*TODO*///			free (CheatTable[i].comment);
/*TODO*///			CheatTable[i].comment = NULL;
/*TODO*///		}
/*TODO*///	}
/*TODO*///
/*TODO*///	reset_table (StartRam);
/*TODO*///	reset_table (BackupRam);
/*TODO*///	reset_table (FlagTable);
/*TODO*///
/*TODO*///	reset_table (OldBackupRam);
/*TODO*///	reset_table (OldFlagTable);
    }

    public static void DoCheat(mame_bitmap bitmap) {
        DisplayWatches(bitmap);

        if ((CheatEnabled) != 0 && (ActiveCheatTotal) != 0) {
            int i, j;

            /* At least one cheat is active, handle them */
            for (i = 0; i < LoadedCheatTotal; i++) {
                /* skip if this isn't an active cheat */
                if ((CheatTable[i].flags & CHEAT_FLAG_ACTIVE) == 0) {
                    continue;
                }

                /* loop through all subcheats */
                for (j = 0; j <= CheatTable[i].num_sub; j++) {
                    subcheat_struct subcheat = CheatTable[i].subcheat[j];

                    if ((subcheat.flags & SUBCHEAT_FLAG_DONE) != 0) {
                        continue;
                    }

                    /* most common case: 0 */
                    if (subcheat.code == kCheatSpecial_Poke) {
                        cpunum_write_byte(subcheat.cpu, subcheat.address, subcheat.data);
                    } /* Check special function if cheat counter is ready */ else if (subcheat.frame_count == 0) {
                        switch (subcheat.code) {
                            case 1:
                                cpunum_write_byte(subcheat.cpu, subcheat.address, subcheat.data);
                                subcheat.flags |= SUBCHEAT_FLAG_DONE;
                                break;
                            case 2:
                            case 3:
                            case 4:
                                cpunum_write_byte(subcheat.cpu, subcheat.address, subcheat.data);
                                subcheat.frame_count = subcheat.frames_til_trigger;
                                break;

                            /* 5,6,7 check if the value has changed, if yes, start a timer. */
 /* When the timer ends, change the location */
                            case 5:
                            case 6:
                            case 7:
                                if ((subcheat.flags & SUBCHEAT_FLAG_TIMED) != 0) {
                                    cpunum_write_byte(subcheat.cpu, subcheat.address, subcheat.data);
                                    subcheat.flags &= ~SUBCHEAT_FLAG_TIMED;
                                } else if (cpunum_read_byte(subcheat.cpu, subcheat.address) != subcheat.data) {
                                    subcheat.frame_count = subcheat.frames_til_trigger;
                                    subcheat.flags |= SUBCHEAT_FLAG_TIMED;
                                }
                                break;

                            /* 8,9,10,11 do not change the location if the value change by X every frames
						  This is to try to not change the value of an energy bar
				 		  when a bonus is awarded to it at the end of a level
				 		  See Kung Fu Master */
                            case 8:
                            case 9:
                            case 10:
                            case 11:
                                if ((subcheat.flags & SUBCHEAT_FLAG_TIMED) != 0) {
                                    /* Check the value to see if it has increased over the original value by 1 or more */
                                    if (cpunum_read_byte(subcheat.cpu, subcheat.address) != subcheat.backup - (kCheatSpecial_Backup1 - subcheat.code + 1)) {
                                        cpunum_write_byte(subcheat.cpu, subcheat.address, subcheat.data);
                                    }
                                    subcheat.flags &= ~SUBCHEAT_FLAG_TIMED;
                                } else {
                                    subcheat.backup = cpunum_read_byte(subcheat.cpu, subcheat.address);
                                    subcheat.frame_count = 1;
                                    subcheat.flags |= SUBCHEAT_FLAG_TIMED;
                                }
                                break;

                            /* 20-24: set bits */
                            case 20:
                                cpunum_write_byte(subcheat.cpu, subcheat.address, cpunum_read_byte(subcheat.cpu, subcheat.address) | subcheat.data);
                                break;
                            case 21:
                                cpunum_write_byte(subcheat.cpu, subcheat.address, cpunum_read_byte(subcheat.cpu, subcheat.address) | subcheat.data);
                                subcheat.flags |= SUBCHEAT_FLAG_DONE;
                                break;
                            case 22:
                            case 23:
                            case 24:
                                cpunum_write_byte(subcheat.cpu, subcheat.address, cpunum_read_byte(subcheat.cpu, subcheat.address) | subcheat.data);
                                subcheat.frame_count = subcheat.frames_til_trigger;
                                break;

                            /* 40-44: reset bits */
                            case 40:
                                cpunum_write_byte(subcheat.cpu, subcheat.address, cpunum_read_byte(subcheat.cpu, subcheat.address) & ~subcheat.data);
                                break;
                            case 41:
                                cpunum_write_byte(subcheat.cpu, subcheat.address, cpunum_read_byte(subcheat.cpu, subcheat.address) & ~subcheat.data);
                                subcheat.flags |= SUBCHEAT_FLAG_DONE;
                                break;
                            case 42:
                            case 43:
                            case 44:
                                cpunum_write_byte(subcheat.cpu, subcheat.address, cpunum_read_byte(subcheat.cpu, subcheat.address) & ~subcheat.data);
                                subcheat.frame_count = subcheat.frames_til_trigger;
                                break;

                            /* 60-65: user select, poke when changes */
                            case 60:
                            case 61:
                            case 62:
                            case 63:
                            case 64:
                            case 65:
                                if ((subcheat.flags & SUBCHEAT_FLAG_TIMED) != 0) {
                                    if (cpunum_read_byte(subcheat.cpu, subcheat.address) != subcheat.backup) {
                                        cpunum_write_byte(subcheat.cpu, subcheat.address, subcheat.data);
                                        subcheat.flags |= SUBCHEAT_FLAG_DONE;
                                    }
                                } else {
                                    subcheat.backup = cpunum_read_byte(subcheat.cpu, subcheat.address);
                                    subcheat.frame_count = 1;
                                    subcheat.flags |= SUBCHEAT_FLAG_TIMED;
                                }
                                break;

                            /* 70-75: user select, poke once */
                            case 70:
                            case 71:
                            case 72:
                            case 73:
                            case 74:
                            case 75:
                                cpunum_write_byte(subcheat.cpu, subcheat.address, subcheat.data);
                                subcheat.flags |= SUBCHEAT_FLAG_DONE;
                                break;
                        }
                    } else {
                        subcheat.frame_count--;
                    }
                }
            }
            /* end for */
        }

        /* IPT_UI_TOGGLE_CHEAT Enable/Disable the active cheats on the fly. Required for some cheats. */
        if ((input_ui_pressed(IPT_UI_TOGGLE_CHEAT)) != 0) {
            /* Hold down shift to toggle the watchpoints */
            if (code_pressed(KEYCODE_LSHIFT) != 0 || code_pressed(KEYCODE_RSHIFT) != 0) {
                is_watch_visible ^= 1;
                usrintf_showmessage("%s %s", ui_getstring(UI_watchpoints), (is_watch_visible != 0 ? ui_getstring(UI_on) : ui_getstring(UI_off)));
            } else if (ActiveCheatTotal != 0) {
                CheatEnabled ^= 1;
                usrintf_showmessage("%s %s", ui_getstring(UI_cheats), (CheatEnabled != 0 ? ui_getstring(UI_on) : ui_getstring(UI_off)));
            }
        }

    }

}
