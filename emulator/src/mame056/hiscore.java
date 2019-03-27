/**
 * ported to v0.56
 * ported to v0.37b7
 *
 */
package mame056;

import static common.ptr.*;
import static mame056.mame.Machine;
import arcadeflex056.util.hiscoreFileParser;
import static mame056.osdependH.*;
import static arcadeflex056.fileio.*;
import static arcadeflex036.osdepend.logerror;
import static mame056.cpuintrf.*;

public class hiscore {

    public static class mem_range {

        public int/*UINT32*/ cpu, addr, num_bytes, start_value, end_value;
        public mem_range next;
    }

    public static class _state {

        public int hiscores_have_been_loaded;
        public mem_range mem_range = null;
    }
    public static _state state = new _state();

    /**
     * **************************************************************************
     */
    static void copy_to_memory(int cpu, int addr, UBytePtr source, int num_bytes) {
        int i;
        for (i = 0; i < num_bytes; i++) {
            cpunum_write_byte(cpu, addr + i, source.read(i));
        }
    }

    static void copy_from_memory(int cpu, int addr, UBytePtr dest, int num_bytes) {
        int i;
        for (i = 0; i < num_bytes; i++) {
            dest.write(i, cpunum_read_byte(cpu, addr + i));
        }
    }

    /**
     * **************************************************************************
     */

    /* safe_to_load checks the start and end values of each memory range */
    public static int safe_to_load() {
        mem_range mem_range = state.mem_range;
        while (mem_range != null) {
            if (cpunum_read_byte(mem_range.cpu, mem_range.addr)
                    != mem_range.start_value) {
                return 0;
            }
            if (cpunum_read_byte(mem_range.cpu, (mem_range.addr + mem_range.num_bytes - 1))
                    != mem_range.end_value) {
                return 0;
            }
            mem_range = mem_range.next;
        }
        return 1;
    }

    /* hs_free disposes of the mem_range linked list */
    static void hs_free() {
        mem_range mem_range = state.mem_range;
        while (mem_range != null) {
            mem_range next = mem_range.next;
            mem_range = null;
            mem_range = next;
        }
        state.mem_range = null;
    }

    static void hs_load() {
        Object f = osd_fopen(Machine.gamedrv.name, null, OSD_FILETYPE_HIGHSCORE, 0);
        state.hiscores_have_been_loaded = 1;
        logerror("hs_load\n");
        if (f != null) {
            mem_range mem_range = state.mem_range;
            logerror("loading...\n");
            while (mem_range != null) {
                UBytePtr data = new UBytePtr(mem_range.num_bytes);
                if (data != null) {
                    /*	this buffer will almost certainly be small
					enough to be dynamically allocated, but let's
					avoid memory trashing just in case
                     */
                    osd_fread(f, data, mem_range.num_bytes);
                    copy_to_memory(mem_range.cpu, mem_range.addr, data, mem_range.num_bytes);
                    data = null;
                }
                mem_range = mem_range.next;
            }
            osd_fclose(f);
        }
    }

    static void hs_save() {
        Object f = osd_fopen(Machine.gamedrv.name, null, OSD_FILETYPE_HIGHSCORE, 1);
        logerror("hs_save\n");
        if (f != null) {
            mem_range mem_range = state.mem_range;
            logerror("saving...\n");
            while (mem_range != null) {
                UBytePtr data = new UBytePtr(mem_range.num_bytes);
                if (data != null) {
                    /*	this buffer will almost certainly be small
					enough to be dynamically allocated, but let's
					avoid memory trashing just in case
                     */
                    copy_from_memory(mem_range.cpu, mem_range.addr, data, mem_range.num_bytes);
                    osd_fwrite(f, data, mem_range.num_bytes);
                }
                mem_range = mem_range.next;
            }
            osd_fclose(f);
        }
    }

    /**
     * **************************************************************************
     */
    /* public API */
 /* call hs_open once after loading a game */
    public static void hs_open(String name) {
        state.mem_range = null;
        logerror("hs_open: '%s'\n", name);
        if (hiscoreFileParser.loadHiscoreFile("hiscore.dat") != 0) {
            hiscoreFileParser.read(name);
        }
    }

    /* call hs_init when emulation starts, and when the game is reset */
    public static void hs_init() {
        mem_range mem_range = state.mem_range;
        state.hiscores_have_been_loaded = 0;

        while (mem_range != null) {
            cpunum_write_byte(
                    mem_range.cpu,
                    mem_range.addr,
                    ~mem_range.start_value
            );

            cpunum_write_byte(
                    mem_range.cpu,
                    mem_range.addr + mem_range.num_bytes - 1,
                    ~mem_range.end_value
            );
            mem_range = mem_range.next;
        }
    }

    /* call hs_update periodically (i.e. once per frame) */
    public static void hs_update() {
        if (state.mem_range != null) {
            if (state.hiscores_have_been_loaded == 0) {
                if (safe_to_load() != 0) {
                    hs_load();
                }
            }
        }
    }

    /* call hs_close when done playing game */
    public static void hs_close() {
        if (state.hiscores_have_been_loaded != 0) {
            hs_save();
        }
        hs_free();
    }

}
